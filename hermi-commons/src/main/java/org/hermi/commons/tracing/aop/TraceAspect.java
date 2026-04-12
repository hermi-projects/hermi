package org.hermi.commons.tracing.aop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
 *   <li><b>Google SRE Alignment</b>: Adheres to standard fields (severity, message, jsonPayload)
 *       for compatibility with Google Cloud Logging and modern observability stacks.
 *   <li><b>Analytics Optimized</b>: Enables complex queries and metric extraction (e.g., p99
 *       latency via duration_ms) without regex overhead.
 * </ul>
 */
@Aspect
public class TraceAspect {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Around("@annotation(trace) || @within(trace)")
  public Object traceExecution(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String methodName = signature.getName();
    Object[] args = joinPoint.getArgs();

    Logger log = LoggerFactory.getLogger(targetClass);
    long startTime = System.currentTimeMillis();

    try {
      // START LOG
      Map<String, Object> startPayload = new LinkedHashMap<>();
      startPayload.put("severity", "INFO");
      startPayload.put("logger", targetClass.getName());
      startPayload.put("method", methodName);
      startPayload.put(
          "message",
          String.format("Execution of %s.%s() started", targetClass.getSimpleName(), methodName));
      startPayload.put("args", summarize(args, 100, false));
      log.info(MAPPER.writeValueAsString(startPayload));

      Object result = joinPoint.proceed();
      long duration = System.currentTimeMillis() - startTime;

      // SUCCESS LOG
      Map<String, Object> successPayload = new LinkedHashMap<>();
      successPayload.put("severity", "INFO");
      successPayload.put("logger", targetClass.getName());
      successPayload.put("method", methodName);
      successPayload.put(
          "message",
          String.format(
              "Execution of %s.%s() finished in %dms",
              targetClass.getSimpleName(), methodName, duration));
      successPayload.put("result", result != null ? result.toString() : "null");
      successPayload.put("duration_ms", duration);
      log.info(MAPPER.writeValueAsString(successPayload));

      return result;
    } catch (Throwable ex) {
      long duration = System.currentTimeMillis() - startTime;

      // FAILURE LOG
      Map<String, Object> failurePayload = new LinkedHashMap<>();
      failurePayload.put("severity", "ERROR");
      failurePayload.put("logger", targetClass.getName());
      failurePayload.put("method", methodName);
      failurePayload.put(
          "message",
          String.format(
              "Execution of %s.%s() failed in %dms",
              targetClass.getSimpleName(), methodName, duration));
      failurePayload.put("args", summarize(args, Integer.MAX_VALUE, true));
      failurePayload.put("exception", ex.getClass().getSimpleName());
      failurePayload.put("exception_message", ex.getMessage());
      failurePayload.put("duration_ms", duration);
      log.error(MAPPER.writeValueAsString(failurePayload));

      throw ex;
    }
  }

  /**
   * Summarizes arguments with custom constraints.
   *
   * @param args The method arguments.
   * @param maxLength Maximum characters before truncation.
   * @param useFullName If true, use full qualified class names; otherwise simple names.
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
