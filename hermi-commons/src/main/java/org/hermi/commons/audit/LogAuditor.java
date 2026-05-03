package org.hermi.commons.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Auditor} that writes execution lifecycle events to the SLF4J debug log.
 *
 * <p>Automatically used by {@link org.hermi.commons.Executor} when no custom Auditor is configured.
 * Each log message includes a UUID (derived from thread name and timestamp via {@link
 * UUID#nameUUIDFromBytes}) so that a single executor's record/result/error lifecycle can be traced.
 * Thread information is left to the logging framework's pattern layout (e.g. {@code %t}).
 *
 * <p>All UUID generation is guarded by {@code isDebugEnabled()} — zero overhead when debug logging
 * is disabled.
 *
 * <p>Logs the context and result via {@code toString()}. Sensitive fields should be masked in the
 * data class's {@code toString()} override.
 *
 * @param <C> context type
 * @param <R> result type
 */
public class LogAuditor<C, R> extends Auditor<C, R> {

  private static final UUID NIL = new UUID(0, 0);
  private final Logger log;

  public LogAuditor(Class<?> executorClass) {
    this.log = LoggerFactory.getLogger(executorClass);
  }

  @Override
  protected UUID doRecord(C context) {
    if (!log.isDebugEnabled()) {
      return NIL;
    }
    UUID id = uuid();
    log.debug("[{}] Executing with context: {}", id, context);
    return id;
  }

  @Override
  protected void doRecordResult(UUID uuid, R result) {
    if (log.isDebugEnabled()) {
      log.debug("[{}] Execution completed: {}", uuid, result);
    }
  }

  @Override
  protected void doRecordError(UUID uuid, Exception exception) {
    if (log.isDebugEnabled()) {
      log.debug("[{}] Execution failed: {}", uuid, exception.toString());
    }
  }

  private UUID uuid() {
    return UUID.nameUUIDFromBytes(
        (Thread.currentThread().getName() + "@" + System.currentTimeMillis()).getBytes());
  }
}
