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
 * @param <I> vendor-specific input type
 * @param <O> vendor-specific output type
 */
public abstract class AuditedExecutor<I, O> extends Executor<I, O> {
  protected final Auditor<I, O> auditor;

  protected AuditedExecutor(Auditor<I, O> auditor) {
    this.auditor = Objects.requireNonNull(auditor, "Auditor is required for AuditedExecutor");
  }

  /**
   * Executes the given input with full auditing lifecycle protection.
   *
   * <p>Saves the input via the {@link Auditor} before execution, records the result on success, and
   * logs the error on failure.
   *
   * @param input the execution input
   * @return the execution result
   */
  public final O execute(I input) {
    UUID trackingId = auditor.save(input);
    try {
      O result = super.execute(input);
      auditor.save(trackingId, result);
      return result;
    } catch (Exception e) {
      auditor.error(trackingId, e);
      throw e;
    }
  }
}
