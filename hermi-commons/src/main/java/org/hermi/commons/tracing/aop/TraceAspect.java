package org.hermi.commons.tracing.aop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hermi.commons.tracing.Trace;
import org.hermi.commons.tracing.Traceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

/**
 * Aspect for structured execution tracing of Hermi components.
 *
 * <p>Adheres to core observability standards:
 *
 * <ul>
 *   <li><b>AI Friendly</b>: Uses structured JSON to ensure precision parsing for machines and AI
 *       agents.
 *   <li><b>Human Friendly</b>: Implements natural language messaging for enhanced intuitive
 *       experiences during manual reviews.
 *   <li><b>Analytical Depth</b>: Provides independent numeric and categorical fields (e.g.,
 *       duration_ms) to enable complex metric extraction and multi-dimensional queries.
 *   <li><b>Industry Standard</b>: Aligns with <b>Google SRE</b> and <b>Google Cloud Logging</b>
 *       structured logging definitions (using severity and service-context fields).
 * </ul>
 */
@Aspect
public class TraceAspect {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String START_MESSAGE = "Execution of %s.%s() started";
  private static final String SUCCESS_MESSAGE = "Execution of %s.%s() finished in %dms";
  private static final String FAILURE_MESSAGE = "Execution of %s.%s() failed";

  @Around("execution(* *(..)) && @annotation(trace)")
  public Object traceMethod(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
    return proceed(joinPoint, trace);
  }

  @Around(
      "execution(* *(..)) && @within(org.hermi.commons.tracing.Trace) && !@annotation(org.hermi.commons.tracing.Trace)")
  public Object traceClass(ProceedingJoinPoint joinPoint) throws Throwable {
    Trace trace = joinPoint.getTarget().getClass().getAnnotation(Trace.class);
    return proceed(joinPoint, trace);
  }

  private Object proceed(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String simpleName = targetClass.getSimpleName();
    String fullName = targetClass.getName();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();
    Logger log = LoggerFactory.getLogger(targetClass);

    long startTime = System.currentTimeMillis();

    // Event Propagation Logic: Directly using the bound trace parameter
    String event = trace.event();
    String previousEvent = MDC.get("event");
    boolean eventOverride = event != null && !event.isEmpty();

    if (eventOverride) {
      MDC.put("event", event);
    }

    try {
      logStart(log, trace.severity(), fullName, simpleName, methodName, args, trace.maxArgLength());

      Object result = joinPoint.proceed();
      logSuccess(
          log,
          trace.severity(),
          fullName,
          simpleName,
          methodName,
          result,
          System.currentTimeMillis() - startTime,
          trace.maxResultLength(),
          trace.slowThresholdMs());
      return result;
    } catch (Throwable ex) {
      logFailure(log, fullName, simpleName, methodName, args, ex);
      throw ex;
    } finally {
      if (eventOverride) {
        if (previousEvent != null) {
          MDC.put("event", previousEvent);
        } else {
          MDC.remove("event");
        }
      }
    }
  }

  private void logStart(
      Logger log,
      String severity,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args,
      int maxArgLength) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", severity);
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(START_MESSAGE, simpleName, methodName));
    payload.put(
        "args", (maxArgLength < 0) ? "[MASKED]" : summarizeArgs(args, maxArgLength, false, true));

    logSilently(log, severity, payload);
  }

  private void logSuccess(
      Logger log,
      String baseSeverity,
      String fullName,
      String simpleName,
      String methodName,
      Object result,
      long duration,
      int maxResultLength,
      long slowThresholdMs) {
    String severity = baseSeverity;
    if (slowThresholdMs > 0 && duration >= slowThresholdMs) {
      severity = "WARN";
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", severity);
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(SUCCESS_MESSAGE, simpleName, methodName, duration));
    payload.put(
        "result",
        (maxResultLength < 0) ? "[MASKED]" : summarizeResult(result, maxResultLength, true));
    payload.put("duration_ms", duration);

    logSilently(log, severity, payload);
  }

  private void logFailure(
      Logger log,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args,
      Throwable ex) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", "ERROR");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(FAILURE_MESSAGE, simpleName, methodName));
    payload.put("args", summarizeArgs(args, Integer.MAX_VALUE, true, false));
    payload.put("exception", ex.getClass().getSimpleName());
    payload.put("exception_message", ex.getMessage());

    logSilently(log, "ERROR", payload);
  }

  /**
   * Serializes the payload to JSON and logs it with the specified severity. Ensures that logging
   * failures never impact the main business logic.
   */
  private void logSilently(Logger log, String severity, Map<String, Object> payload) {
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
    } catch (Exception e) {
      // Internal logging failure should never impact business logic
    }
  }

  /**
   * Summarizes arguments with custom constraints to prevent log bloat.
   *
   * @param args The method arguments.
   * @param maxArgLength Maximum characters before truncation.
   * @param useFullName If true, use fully qualified class names; otherwise simple names.
   * @param supportTraceable If true, allow custom summaries via Traceable interface.
   */
  private String summarizeArgs(
      Object[] args, int maxArgLength, boolean useFullName, boolean supportTraceable) {
    if (args == null || args.length == 0) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder("");
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      if (arg == null) {
        sb.append("null");
      } else {
        String type = useFullName ? arg.getClass().getName() : arg.getClass().getSimpleName();
        String value =
            (supportTraceable && arg instanceof Traceable traceable)
                ? traceable.toTraceString()
                : type + ":" + String.valueOf(arg);
        sb.append(value);
      }
      if (i < args.length - 1) {
        sb.append(", ");
      }
    }

    String full = sb.toString();
    if (full.length() > maxArgLength) {
      return full.substring(0, maxArgLength) + "...(truncated)";
    }
    return "[" + full + "]";
  }

  /**
   * Summarizes the result to prevent log bloat.
   *
   * @param result The method return value.
   * @param maxResultLength Maximum characters before truncation.
   * @param supportTraceable If true, allow custom summaries via Traceable interface.
   */
  private String summarizeResult(Object result, int maxResultLength, boolean supportTraceable) {
    if (result == null) {
      return "null";
    }
    String full =
        (supportTraceable && result instanceof Traceable traceable)
            ? traceable.toTraceString()
            : result.toString();
    if (full == null) {
      full = "null";
    }
    if (full.length() > maxResultLength) {
      return full.substring(0, maxResultLength) + "...(truncated)";
    }
    return full;
  }
}
