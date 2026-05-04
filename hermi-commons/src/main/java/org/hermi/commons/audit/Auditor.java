package org.hermi.commons.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic base class for auditing execution lifecycles within the Hermi framework.
 *
 * <p>Provides three lifecycle hooks — payload, response, and error — with built-in safety
 * guarantees: exceptions in the hooks are caught and suppressed so they never interrupt the primary
 * execution flow.
 *
 * @param <C> context type (input to the audited operation)
 * @param <R> result type (output of the audited operation)
 */
public abstract class Auditor<C, R> {
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  /**
   * Implementation hook for saving the input context.
   *
   * @param context the execution context
   * @return a unique ID used to correlate the lifecycle stages
   */
  protected abstract UUID doRecordContext(C context);

  /**
   * Implementation hook for recording the successful result.
   *
   * @param trackingId the ID returned by {@link #doRecordContext(Object)}
   * @param result the execution result
   */
  protected abstract void doRecordResult(UUID trackingId, R result);

  /**
   * Implementation hook for recording a failure.
   *
   * @param trackingId the ID returned by {@link #doRecordContext(Object)}
   * @param exception the exception that was thrown
   */
  protected abstract void doRecordError(UUID trackingId, Exception exception);

  /**
   * Saves the input context and returns a tracking ID.
   *
   * @param context the execution context
   * @return a tracking ID for correlating response/error with this payload
   */
  public final UUID recordContext(C context) {
    try {
      return doRecordContext(context);
    } catch (Exception e) {
      log.warn("Auditor failed to record context", e);
      return UUID.randomUUID();
    }
  }

  /**
   * Records the successful result.
   *
   * @param trackingId the ID from {@link #recordContext(Object)}
   * @param result the execution result
   */
  public final void recordResult(UUID trackingId, R result) {
    try {
      doRecordResult(trackingId, result);
    } catch (Exception e) {
      log.warn("Auditor failed to record result for trackingId={}", trackingId, e);
    }
  }

  /**
   * Records the failure.
   *
   * @param trackingId the ID from {@link #recordContext(Object)}
   * @param exception the exception that was thrown
   */
  public final void recordError(UUID trackingId, Exception exception) {
    try {
      doRecordError(trackingId, exception);
    } catch (Exception auditEx) {
      log.warn("Auditor failed to record error for trackingId={}", trackingId, auditEx);
    }
  }
}
