package org.hermi.commons.audit;

import java.util.UUID;
import org.hermi.constraint.mask.MaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Default {@link Auditor} that writes structured execution lifecycle events to the SLF4J debug log.
 *
 * <p>Context and result values are masked through {@link MaskMapper} before logging, so sensitive
 * fields annotated with {@code @Mask} or {@code @SSN} are obfuscated.
 *
 * <p>Uses SLF4J 2 fluent API to attach structured data as key-value pairs, which {@code
 * LoggingEventCompositeJsonEncoder} renders as top-level JSON fields:
 *
 * <ul>
 *   <li>{@code executionId} — correlation id across STARTED/SUCCEEDED/FAILED (all events)
 *   <li>{@code executor} — fully-qualified class name of the executor (all events)
 *   <li>{@code traceId}, {@code spanId} — distributed tracing identifiers from SLF4J MDC (all
 *       events)
 *   <li>{@code context} — masked context JSON (STARTED, FAILED)
 *   <li>{@code result} — masked result JSON (SUCCEEDED)
 *   <li>{@code exceptionClass}, {@code exceptionMessage} — error metadata (FAILED)
 * </ul>
 *
 * <p>On FAILED, the exception is passed via {@code setCause} so logback's native {@code
 * <stackTrace/>} provider renders the full stack trace.
 */
public class LogAuditor<C, R> extends Auditor<C, R> {

  private static final String EXECUTOR = "executor";
  private static final String EXECUTION_ID = "executionId";
  private static final String TRACE_ID = "traceId";
  private static final String SPAN_ID = "spanId";
  private static final String CONTEXT = "context";
  private static final String RESULT = "result";
  private static final String EXCEPTION_CLASS = "exceptionClass";
  private static final String EXCEPTION_MESSAGE = "exceptionMessage";

  private final Logger log;
  private final String executor;
  private final String executorSimpleName;

  public LogAuditor(Class<?> executorClass) {
    this.log = LoggerFactory.getLogger(executorClass);
    this.executor = executorClass.getName();
    this.executorSimpleName = executorClass.getSimpleName();
  }

  @Override
  protected UUID doRecordContext(C context) {
    UUID uuid = UUID.randomUUID();
    if (!log.isInfoEnabled()) {
      return uuid;
    }
    log.atInfo()
        .addKeyValue(EXECUTION_ID, uuid)
        .addKeyValue(EXECUTOR, executor)
        .addKeyValue(TRACE_ID, MDC.get(TRACE_ID))
        .addKeyValue(SPAN_ID, MDC.get(SPAN_ID))
        .addKeyValue(CONTEXT, MaskMapper.mask(context))
        .log("{} execution started.", executorSimpleName);
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID uuid, R result) {
    if (!log.isInfoEnabled()) {
      return;
    }
    log.atInfo()
        .addKeyValue(EXECUTION_ID, uuid)
        .addKeyValue(EXECUTOR, executor)
        .addKeyValue(TRACE_ID, MDC.get(TRACE_ID))
        .addKeyValue(SPAN_ID, MDC.get(SPAN_ID))
        .addKeyValue(RESULT, MaskMapper.mask(result))
        .log("{} execution succeeded.", executorSimpleName);
  }

  @Override
  protected void doRecordError(UUID uuid, C context, Exception exception) {
    if (!log.isErrorEnabled()) {
      return;
    }
    log.atError()
        .addKeyValue(EXECUTION_ID, uuid)
        .addKeyValue(EXECUTOR, executor)
        .addKeyValue(TRACE_ID, MDC.get(TRACE_ID))
        .addKeyValue(SPAN_ID, MDC.get(SPAN_ID))
        .addKeyValue(CONTEXT, MaskMapper.mask(context))
        .addKeyValue(EXCEPTION_CLASS, exception.getClass().getName())
        .addKeyValue(EXCEPTION_MESSAGE, exception.getMessage())
        .setCause(exception)
        .log("{} execution failed.", executorSimpleName);
  }
}
