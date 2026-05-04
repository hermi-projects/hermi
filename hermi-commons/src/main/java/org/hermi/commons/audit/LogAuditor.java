package org.hermi.commons.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Auditor} that writes structured execution lifecycle events to the SLF4J debug log.
 *
 * <p>Uses SLF4J 2 fluent API to attach structured data as key-value pairs, which {@code
 * LoggingEventCompositeJsonEncoder} renders as top-level JSON fields:
 *
 * <ul>
 *   <li>{@code uuid} — correlation id across STARTED/SUCCEEDED/FAILED (all events)
 *   <li>{@code class} — executor class name (all events)
 *   <li>{@code context} — normalized context (STARTED, FAILED)
 *   <li>{@code result} — normalized result (SUCCEEDED)
 *   <li>{@code exceptionClass}, {@code exceptionMessage} — error metadata (FAILED)
 * </ul>
 *
 * <p>On FAILED, the exception is passed via {@code setCause} so logback's native {@code
 * <stackTrace/>} provider renders the full stack trace.
 */
public class LogAuditor<C, R> extends Auditor<C, R> {

  private static final UUID NIL = new UUID(0, 0);
  private static final String KEY_CONTEXT_ID = "contextId";
  private static final String KEY_CONTEXT = "context";
  private static final String KEY_RESULT = "result";
  private static final String KEY_EX_CLASS = "exceptionClass";
  private static final String KEY_EX_MESSAGE = "exceptionMessage";

  private final Logger log;

  public LogAuditor(Class<?> executorClass) {
    this.log = LoggerFactory.getLogger(executorClass);
  }

  @Override
  protected UUID doRecordContext(C context) {
    if (!log.isDebugEnabled()) {
      return NIL;
    }
    UUID uuid = UUID.randomUUID();
    log.atDebug()
        .addKeyValue(KEY_CONTEXT_ID, uuid)
        .addKeyValue(KEY_CONTEXT, context)
        .log("Execution started.");
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID uuid, R result) {
    if (log.isDebugEnabled()) {
      log.atDebug()
          .addKeyValue(KEY_CONTEXT_ID, uuid)
          .addKeyValue(KEY_RESULT, result)
          .log("Execution succeeded.");
    }
  }

  @Override
  protected void doRecordError(UUID uuid, C context, Exception exception) {
    log.atError()
        .addKeyValue(KEY_CONTEXT_ID, uuid)
        .addKeyValue(KEY_CONTEXT, context)
        .addKeyValue(KEY_EX_CLASS, exception.getClass().getName())
        .addKeyValue(KEY_EX_MESSAGE, exception.getMessage())
        .setCause(exception)
        .log("Execution failed.");
  }
}
