package org.hermi.commons.tracing.aop;

import java.util.Arrays;
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
 *   <li><b>Human-Readable</b>: Uses clear delimiters (|) for rapid manual inspection and grep.
 *   <li><b>AI-Parseable</b>: Consistent, low-ambiguity patterns optimized for automated diagnostic
 *       agents.
 *   <li><b>Log-Analytics Ready (e.g., ELK)</b>: Uses Key-Value Pairs (KVP) to enable automatic
 *       field extraction and performance metric calculations (e.g., duration_ms).
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
      startPayload.put("message", "START");
      startPayload.put("method", methodName);
      startPayload.put("args", Arrays.toString(args));
      if (trace != null && !trace.value().isEmpty()) {
        startPayload.put("intent", trace.value());
      }
      log.info(MAPPER.writeValueAsString(startPayload));

      Object result = joinPoint.proceed();
      long duration = System.currentTimeMillis() - startTime;

      // SUCCESS LOG
      Map<String, Object> successPayload = new LinkedHashMap<>();
      successPayload.put("severity", "INFO");
      successPayload.put("message", "SUCCESS");
      successPayload.put("method", methodName);
      successPayload.put("result", result != null ? result.toString() : "null");
      successPayload.put("duration_ms", duration);
      log.info(MAPPER.writeValueAsString(successPayload));

      return result;
    } catch (Throwable ex) {
      long duration = System.currentTimeMillis() - startTime;

      // FAILURE LOG
      Map<String, Object> failurePayload = new LinkedHashMap<>();
      failurePayload.put("severity", "ERROR");
      failurePayload.put("message", "FAILURE");
      failurePayload.put("method", methodName);
      failurePayload.put("exception", ex.getClass().getSimpleName());
      failurePayload.put("exception_message", ex.getMessage());
      failurePayload.put("duration_ms", duration);
      log.error(MAPPER.writeValueAsString(failurePayload));

      throw ex;
    }
  }
}
