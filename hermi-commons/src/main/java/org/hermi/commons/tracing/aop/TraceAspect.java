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
      logStart(log, fullName, simpleName, methodName, args);

      Object result = joinPoint.proceed();
      logSuccess(
          log, fullName, simpleName, methodName, result, System.currentTimeMillis() - startTime);
      return result;
    } catch (Throwable ex) {
      logFailure(
          log, fullName, simpleName, methodName, args, ex, System.currentTimeMillis() - startTime);
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
      Logger log, String fullName, String simpleName, String methodName, Object[] args) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", "INFO");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(START_MESSAGE, simpleName, methodName));
    payload.put("args", summarize(args, 100, false));

    logInfoSilently(log, payload);
  }

  private void logSuccess(
      Logger log,
      String fullName,
      String simpleName,
      String methodName,
      Object result,
      long duration) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", "INFO");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(SUCCESS_MESSAGE, simpleName, methodName, duration));
    payload.put("result", result != null ? result.toString() : "null");
    payload.put("duration_ms", duration);

    logInfoSilently(log, payload);
  }

  private void logFailure(
      Logger log,
      String fullName,
      String simpleName,
      String methodName,
      Object[] args,
      Throwable ex,
      long duration) {
    Map<String, Object> payload = new LinkedHashMap<>();
    String currentEvent = MDC.get("event");
    if (currentEvent != null) {
      payload.put("event", currentEvent);
    }
    payload.put("severity", "ERROR");
    payload.put("logger", fullName);
    payload.put("method", methodName);
    payload.put("message", String.format(FAILURE_MESSAGE, simpleName, methodName, duration));
    payload.put("args", summarize(args, Integer.MAX_VALUE, true));
    payload.put("exception", ex.getClass().getSimpleName());
    payload.put("exception_message", ex.getMessage());
    payload.put("duration_ms", duration);

    logErrorSilently(log, payload);
  }

  /**
   * Serializes the payload to JSON and logs it as INFO. Ensures that logging failures never impact
   * the main business logic.
   */
  private void logInfoSilently(Logger log, Map<String, Object> payload) {
    try {
      log.info(MAPPER.writeValueAsString(payload));
    } catch (Exception e) {
      // Internal logging failure should never impact business logic
    }
  }

  /**
   * Serializes the payload to JSON and logs it as ERROR. Ensures that logging failures never impact
   * the main business logic.
   */
  private void logErrorSilently(Logger log, Map<String, Object> payload) {
    try {
      log.error(MAPPER.writeValueAsString(payload));
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
