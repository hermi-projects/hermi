package org.hermi.commons.tracing.aop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hermi.commons.tracing.Trace;
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
  private static final String FAILURE_MESSAGE = "Execution of %s.%s() failed in %dms";

  @Around("@annotation(trace) || @within(trace)")
  public Object trace(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
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
      logStart(log, trace.severity(), fullName, simpleName, methodName, args, trace.excludeArgs());

      Object result = joinPoint.proceed();
      logSuccess(
          log,
          trace.severity(),
          fullName,
          simpleName,
          methodName,
          result,
          System.currentTimeMillis() - startTime,
          trace.excludeResult(),
          trace.slowThresholdMs());
      return result;
    } catch (Throwable ex) {
      logFailure(
          log,
          fullName,
          simpleName,
          methodName,
          args,
          ex,
          System.currentTimeMillis() - startTime,
          trace.excludeArgs());
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
      boolean excludeArgs) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", severity);
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(START_MESSAGE, simpleName, methodName));
    payload.put("args", excludeArgs ? "[MASKED]" : summarize(args, 100, false));

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
      boolean excludeResult,
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
        "result", excludeResult ? "[MASKED]" : (result != null ? result.toString() : "null"));
    payload.put("duration_ms", duration);

    logSilently(log, severity, payload);
  }

  private void logFailure(
      Logger log,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args,
      Throwable ex,
      long duration,
      boolean excludeArgs) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", "ERROR");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(FAILURE_MESSAGE, simpleName, methodName, duration));
    payload.put("args", excludeArgs ? "[MASKED]" : summarize(args, Integer.MAX_VALUE, true));
    payload.put("exception", ex.getClass().getSimpleName());
    payload.put("exception_message", ex.getMessage());
    payload.put("duration_ms", duration);

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
   * @param maxLength Maximum characters before truncation.
   * @param useFullName If true, use fully qualified class names; otherwise simple names.
   */
  private String summarize(Object[] args, int maxLength, boolean useFullName) {
    if (args == null || args.length == 0) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      if (arg == null) {
        sb.append("null");
      } else {
        String type = useFullName ? arg.getClass().getName() : arg.getClass().getSimpleName();
        sb.append(type).append(":").append(arg);
      }
      if (i < args.length - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");

    String full = sb.toString();
    int actualMax = Math.max(maxLength, 100);
    if (full.length() > actualMax) {
      return full.substring(0, actualMax) + "...(truncated)";
    }
    return full;
  }
}
