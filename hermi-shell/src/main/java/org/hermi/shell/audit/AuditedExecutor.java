package org.hermi.shell.audit;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.Executor;

/**
 * Base class for vendor-specific components that require audited execution lifecycle.
 *
 * <p>Provides the common {@link Auditor} field and the auditing lifecycle ({@link #execute}) shared
 * by {@link org.hermi.shell.Client} and {@link org.hermi.shell.Messenger}.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class AuditedExecutor<P, R> extends Executor<P, R> {
  protected final Auditor<P, R> auditor;

  protected AuditedExecutor(Auditor<P, R> auditor) {
    this.auditor = Objects.requireNonNull(auditor, "Auditor is required for AuditedExecutor");
  }

  /**
   * Executes the given input with full auditing lifecycle protection.
   *
   * <p>Saves the input via the {@link Auditor} before execution, records the result on success, and
   * logs the error on failure.
   *
   * @param payload the execution payload
   * @return the execution result
   */
  @Override
  public final R execute(P payload) {
    UUID trackingId = auditor.recordPayload(payload);
    try {
      R result = super.execute(payload);
      auditor.recordResponse(trackingId, result);
      return result;
    } catch (Exception e) {
      auditor.recordError(trackingId, e);
      throw e;
    }
  }
}
