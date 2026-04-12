package org.hermi.commons.tracing.aop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hermi.commons.tracing.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Aspect for structured execution tracing of Hermi components.
 *
 * <p>Adheres to three core observability standards:
 *
 * <ul>
 *   <li><b>JSON Structured Format</b>: Direct serialization to JSON objects for seamless machine
 *       ingestion and processing.
 *   <li><b>Google SRE Alignment</b>: Adheres to standard fields (severity, message) for
 *       compatibility with Google Cloud Logging and modern observability stacks.
 *   <li><b>Analytics Optimized</b>: Enables complex queries and metric extraction (e.g., p99
 *       latency via duration_ms) without regex overhead.
 * </ul>
 */
@Aspect
public class TraceAspect {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Around("@annotation(trace) || @within(trace)")
  public Object traceExecution(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
    long startTime = System.currentTimeMillis();

    logStart(joinPoint);

    try {
      Object result = joinPoint.proceed();
      long duration = System.currentTimeMillis() - startTime;
      logSuccess(joinPoint, result, duration);
      return result;
    } catch (Throwable ex) {
      long duration = System.currentTimeMillis() - startTime;
      logFailure(joinPoint, ex, duration);
      throw ex;
    }
  }

  private void logStart(ProceedingJoinPoint joinPoint) {
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();
    Logger log = LoggerFactory.getLogger(targetClass);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("severity", "INFO");
    payload.put("logger", targetClass.getName());
    payload.put("method", methodName);
    payload.put(
        "message",
        String.format("Execution of %s.%s() started", targetClass.getSimpleName(), methodName));
    payload.put("args", summarize(args, 100, false));

    logSilently(log, payload);
  }

  private void logSuccess(ProceedingJoinPoint joinPoint, Object result, long duration) {
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Logger log = LoggerFactory.getLogger(targetClass);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("severity", "INFO");
    payload.put("logger", targetClass.getName());
    payload.put("method", methodName);
    payload.put(
        "message",
        String.format(
            "Execution of %s.%s() finished in %dms",
            targetClass.getSimpleName(), methodName, duration));
    payload.put("result", result != null ? result.toString() : "null");
    payload.put("duration_ms", duration);

    logSilently(log, payload);
  }

  private void logFailure(ProceedingJoinPoint joinPoint, Throwable ex, long duration) {
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();
    Logger log = LoggerFactory.getLogger(targetClass);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("severity", "ERROR");
    payload.put("logger", targetClass.getName());
    payload.put("method", methodName);
    payload.put(
        "message",
        String.format(
            "Execution of %s.%s() failed in %dms",
            targetClass.getSimpleName(), methodName, duration));
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
  private void logSilently(Logger log, Map<String, Object> payload) {
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
