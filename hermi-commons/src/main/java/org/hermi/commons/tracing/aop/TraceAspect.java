package org.hermi.commons.tracing.aop;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hermi.commons.tracing.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  @Around("@annotation(trace) || @within(trace)")
  public Object traceExecution(ProceedingJoinPoint joinPoint, Trace trace) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Class<?> targetClass = joinPoint.getTarget().getClass();
    String methodName = signature.getName();
    Object[] args = joinPoint.getArgs();

    Logger log = LoggerFactory.getLogger(targetClass);

    String intentStr =
        (trace != null && !trace.value().isEmpty()) ? " intent=\"" + trace.value() + "\"" : "";

    log.debug("START | method={} args=\"{}\"{}", methodName, Arrays.toString(args), intentStr);

    long startTime = System.currentTimeMillis();
    try {
      Object result = joinPoint.proceed();
      long duration = System.currentTimeMillis() - startTime;

      log.debug("SUCCESS | method={} result=\"{}\" duration_ms={}", methodName, result, duration);
      return result;
    } catch (Throwable ex) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "FAILURE | method={} exception={} message=\"{}\" duration_ms={}",
          methodName,
          ex.getClass().getSimpleName(),
          ex.getMessage(),
          duration);
      throw ex;
    }
  }
}
