package org.hermi.logging.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.logging.annotations.EnableHermiLogging;
import org.hermi.logging.annotations.HermiLogging;
import org.hermi.logging.api.Loggable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

/**
 * Core AOP aspect that powers the HermiTracing framework.
 *
 * <p>This aspect provides:
 *
 * <ul>
 *   <li>Automatic tracing for all non-private methods inside the package where {@link
 *       EnableHermiLogging} is applied.
 *   <li>Optional fine-grained control via {@link HermiLogging} on classes or methods.
 *   <li>Structured JSON tracing for start, success, and failure events.
 *   <li>MDC event propagation.
 *   <li>Argument/result summarization with length limits.
 *   <li>Slow-call detection via {@code slowThresholdMs}.
 *   <li>Support for {@link Loggable} objects to customize trace output.
 * </ul>
 *
 * <p>Performance:
 *
 * <ul>
 *   <li>Root package is resolved once and cached.
 *   <li>Tracing decisions are O(1) string prefix checks.
 *   <li>No classpath scanning, no reflection loops.
 * </ul>
 */
@Aspect
public class HermiLoggingAspect {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Cache for annotation lookups to avoid repeated reflection */
  private static final ConcurrentHashMap<Method, HermiLogging> METHOD_TRACE_CACHE =
      new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<Class<?>, HermiLogging> CLASS_TRACE_CACHE =
      new ConcurrentHashMap<>();

  /** Sensitive keywords to mask */
  private static final List<String> SENSITIVE_KEYS =
      List.of("password", "pwd", "secret", "token", "key", "credential");

  /** Cached root package */
  private static final List<String> ROOT_PACKAGES = List.of("org.hermi");

  // ----------------------------------------------------------------------
  // Root package initialization
  // ----------------------------------------------------------------------

  @Around("execution(* *(..)) && @within(org.hermi.logging.annotations.EnableHermiLogging)")
  public Object initRootPackage(ProceedingJoinPoint jp) throws Throwable {
    initRootIfNeeded(jp.getSignature().getDeclaringType());
    return jp.proceed();
  }

  private void initRootIfNeeded(Class<?> clazz) {
    if (ROOT_PACKAGES.size() == 1 && ROOT_PACKAGES.get(0).equals("org.hermi")) return;

    synchronized (HermiLoggingAspect.class) {
      if (ROOT_PACKAGES.size() == 1 && ROOT_PACKAGES.get(0).equals("org.hermi")) return;

      EnableHermiLogging enable = clazz.getAnnotation(EnableHermiLogging.class);
      if (enable == null) return;
      if (enable.value().isEmpty()) {
        ROOT_PACKAGES.add(clazz.getPackageName());
      } else {
        for (String packageName : enable.value().split(";")) {
          packageName = packageName.trim();
          if (!ROOT_PACKAGES.contains(packageName)) {
            ROOT_PACKAGES.add(packageName);
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------
  // Trace entry point
  // ----------------------------------------------------------------------

  @Around("execution(* *(..))")
  public Object traceEntry(ProceedingJoinPoint jp) throws Throwable {

    MethodSignature sig = (MethodSignature) jp.getSignature();
    Method method = sig.getMethod();
    Class<?> clazz = sig.getDeclaringType();

    // 1. Method-level annotation (cached)
    HermiLogging trace =
        METHOD_TRACE_CACHE.computeIfAbsent(method, m -> m.getAnnotation(HermiLogging.class));

    // 2. Class-level annotation (cached)
    if (trace == null) {
      trace = CLASS_TRACE_CACHE.computeIfAbsent(clazz, c -> c.getAnnotation(HermiLogging.class));
    }

    // 3. Explicit annotation wins
    if (trace != null) {
      return proceed(jp, trace);
    }

    // 4. Automatic tracing for root package
    if (shouldTrace(jp)) {
      return proceed(jp, DEFAULT_TRACE);
    }

    return jp.proceed();
  }

  private boolean shouldTrace(ProceedingJoinPoint jp) {
    String packageName = jp.getSignature().getDeclaringType().getPackageName();
    for (String rootPackage : ROOT_PACKAGES) {
      if (packageName.startsWith(rootPackage)) {
        return true;
      }
    }
    return false;
  }

  // ----------------------------------------------------------------------
  // Core tracing logic
  // ----------------------------------------------------------------------

  private Object proceed(ProceedingJoinPoint joinPoint, HermiLogging trace) throws Throwable {

    Class<?> targetClass = joinPoint.getTarget().getClass();
    Logger log = LoggerFactory.getLogger(targetClass);

    // Skip expensive work if log level is disabled
    if (!log.isInfoEnabled()) {
      return joinPoint.proceed();
    }

    String methodName = joinPoint.getSignature().getName();
    String simpleName = targetClass.getSimpleName();
    String fullName = targetClass.getName();
    Object[] args = joinPoint.getArgs();

    long startTime = System.currentTimeMillis();

    // MDC event override
    String event = trace.event();
    String previousEvent = MDC.get("event");
    boolean eventOverride = event != null && !event.isEmpty();

    if (eventOverride) MDC.put("event", event);

    try {
      logStart(log, trace, fullName, simpleName, methodName, args);

      Object result = joinPoint.proceed();

      logSuccess(
          log,
          trace,
          fullName,
          simpleName,
          methodName,
          result,
          System.currentTimeMillis() - startTime);

      return result;

    } catch (Throwable ex) {
      logFailure(log, fullName, simpleName, methodName, args, ex);
      throw ex;

    } finally {
      if (eventOverride) {
        if (previousEvent != null) MDC.put("event", previousEvent);
        else MDC.remove("event");
      }
    }
  }

  // ----------------------------------------------------------------------
  // Logging helpers (performance + security enhanced)
  // ----------------------------------------------------------------------

  private void logStart(
      Logger log,
      HermiLogging trace,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args) {

    if (!log.isInfoEnabled()) return;

    Map<String, Object> payload = new LinkedHashMap<>();
    addCommonFields(payload, trace, fullName, methodName);

    payload.put("message", String.format("Execution of %s.%s() started", simpleName, methodName));
    payload.put("args", summarizeArgs(args, trace.maxArgLength()));

    logJson(log, trace.severity(), payload);
  }

  private void logSuccess(
      Logger log,
      HermiLogging trace,
      String fullName,
      String simpleName,
      String methodName,
      Object result,
      long duration) {

    if (!log.isInfoEnabled()) return;

    String severity =
        duration >= trace.slowThresholdMs() && trace.slowThresholdMs() > 0
            ? "WARN"
            : trace.severity();

    Map<String, Object> payload = new LinkedHashMap<>();
    addCommonFields(payload, trace, fullName, methodName);

    payload.put(
        "message",
        String.format("Execution of %s.%s() finished in %dms", simpleName, methodName, duration));
    payload.put("result", summarizeResult(result, trace.maxResultLength()));
    payload.put("duration_ms", duration);

    logJson(log, severity, payload);
  }

  private void logFailure(
      Logger log,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args,
      Throwable ex) {

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("severity", "ERROR");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format("Execution of %s.%s() failed", simpleName, methodName));
    payload.put("args", summarizeArgs(args, 200));
    payload.put("exception", ex.getClass().getSimpleName());
    payload.put("exception_message", ex.getMessage());

    logJson(log, "ERROR", payload);
  }

  private void addCommonFields(
      Map<String, Object> payload, HermiLogging trace, String fullName, String methodName) {

    String currentEvent = MDC.get("event");
    if (currentEvent != null) payload.put("event", currentEvent);

    payload.put("severity", trace.severity());
    payload.put("logger", fullName);
    payload.put("method", methodName);
  }

  private void logJson(Logger log, String severity, Map<String, Object> payload) {
    try {
      String json = MAPPER.writeValueAsString(payload);
      switch (severity.toUpperCase()) {
        case "ERROR":
          log.error(json);
          break;
        case "WARN":
          log.warn(json);
          break;
        case "INFO":
          log.info(json);
          break;
        case "DEBUG":
          log.debug(json);
          break;
        case "TRACE":
          log.trace(json);
          break;
        default:
          log.info(json);
      }
    } catch (Exception ignored) {
    }
  }

  // ----------------------------------------------------------------------
  // Argument/result summarization with security masking
  // ----------------------------------------------------------------------

  private String summarizeArgs(Object[] args, int maxLength) {
    if (args == null || args.length == 0) return "[]";

    StringBuilder sb = new StringBuilder("[");

    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];

      if (arg == null) {
        sb.append("null");
      } else {
        String value = maskIfSensitive(arg.toString());
        sb.append(value);
      }

      if (i < args.length - 1) sb.append(", ");
    }

    sb.append("]");

    String full = sb.toString();
    return full.length() > maxLength ? full.substring(0, maxLength) + "...(truncated)" : full;
  }

  private String summarizeResult(Object result, int maxLength) {
    if (result == null) return "null";

    String full = (result instanceof Loggable t) ? t.toHermiLoggingString() : result.toString();

    return full.length() > maxLength ? full.substring(0, maxLength) + "...(truncated)" : full;
  }

  private String maskIfSensitive(String value) {
    String lower = value.toLowerCase();
    for (String key : SENSITIVE_KEYS) {
      if (lower.contains(key)) {
        return "[MASKED]";
      }
    }
    return value;
  }

  /**
   * A single shared default HermiLogging configuration.
   *
   * <p>This avoids creating new anonymous annotation instances on every method call, improving
   * performance and reducing GC pressure.
   */
  private static final HermiLogging DEFAULT_TRACE =
      new HermiLogging() {

        @Override
        public String event() {
          return "";
        }

        @Override
        public String severity() {
          return "INFO";
        }

        @Override
        public int maxResultLength() {
          return 20;
        }

        @Override
        public long slowThresholdMs() {
          return -1;
        }

        @Override
        public int maxArgLength() {
          return 20;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
          return HermiLogging.class;
        }
      };
}
