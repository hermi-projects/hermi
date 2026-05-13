package org.hermi.commons.audit;

import java.util.UUID;
import org.hermi.constraint.mask.MaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li>{@code uuid} — correlation id across STARTED/SUCCEEDED/FAILED (all events)
 *   <li>{@code context} — masked context JSON (STARTED, FAILED)
 *   <li>{@code result} — masked result JSON (SUCCEEDED)
 *   <li>{@code exceptionClass}, {@code exceptionMessage} — error metadata (FAILED)
 * </ul>
 *
 * <p>On FAILED, the exception is passed via {@code setCause} so logback's native {@code
 * <stackTrace/>} provider renders the full stack trace.
 */
public class LogAuditor<C, R> extends Auditor<C, R> {

  private static final String KEY_CONTEXT_ID = "contextId";
  private static final String KEY_CONTEXT = "context";
  private static final String KEY_RESULT = "result";

  private static final String KEY_EX_CLASS = "exceptionClass";
  private static final String KEY_EX_MESSAGE = "exceptionMessage";

  private final Logger log;
  private final String executorClassName;

  public LogAuditor(Class<?> executorClass) {
    this.log = LoggerFactory.getLogger(executorClass);
    this.executorClassName = executorClass.getSimpleName();
  }

  @Override
  protected UUID doRecordContext(C context) {
    UUID uuid = UUID.randomUUID();
    log.atInfo()
        .addKeyValue(KEY_CONTEXT_ID, uuid)
        .addKeyValue(KEY_CONTEXT, MaskMapper.mask(context))
        .log("{} execution started.", executorClassName);
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID uuid, R result) {
    log.atDebug()
        .addKeyValue(KEY_CONTEXT_ID, uuid)
        .addKeyValue(KEY_RESULT, MaskMapper.mask(result))
        .log("{} execution succeeded.", executorClassName);
  }

  @Override
  protected void doRecordError(UUID uuid, C context, Exception exception) {
    log.atError()
        .addKeyValue(KEY_CONTEXT_ID, uuid)
        .addKeyValue(KEY_CONTEXT, MaskMapper.mask(context))
        .addKeyValue(KEY_EX_CLASS, exception.getClass().getName())
        .addKeyValue(KEY_EX_MESSAGE, exception.getMessage())
        .setCause(exception)
        .log("{} execution failed.", executorClassName);
  }
}
