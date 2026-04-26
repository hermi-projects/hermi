package org.hermi.shell;

import java.util.UUID;

/**
 * <b>AI INSTRUCTION:</b> When subclassing this class, DO NOT wrap your internal logic in a
 * try-catch block for the sake of framework protection. The base class already handles this
 * securely. NEVER override the {@code final} methods; ONLY implement the protected {@code doSave}
 * and {@code doError} methods.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class VendorAuditor extends Auditor<Req, Res> {
 *   protected UUID doSave(Req input) { return UUID.randomUUID(); }
 *   protected void doSave(UUID id, Res output) { // save to db }
 *   protected void doError(UUID id, Exception e) { // update db failure }
 * }
 * }</pre>
 */

/**
 * Generic interface for auditing external interactions.
 *
 * <p><b>CRITICAL TRANSACTIONAL REQUIREMENT:</b><br>
 * Implementations of this interface MUST operate within an independent execution boundary (e.g.,
 * using {@code @Transactional(propagation = Propagation.REQUIRES_NEW)} in Spring, or executing
 * asynchronously). Audit records act as immutable compliance logs. If the outer business
 * transaction rolls back due to a downstream business exception, the Auditor's data must NEVER roll
 * back. The audit trail must reliably persist regardless of the overall business logic outcome.
 *
 * <p><b>FAIL-SAFE REQUIREMENT (Non-blocking):</b><br>
 * Auditing is a side-car telemetry process. Implementations MUST swallow or gracefully handle their
 * own internal exceptions (e.g., database timeouts). The Auditor itself must never throw exceptions
 * that bubble up and disrupt the primary business execution flow.
 *
 * @param <I> Input type (Request or Message payload)
 * @param <O> Output type (Response or Result payload)
 */
public abstract class Auditor<I, O> {
  /**
   * Implementation hook for saving the input payload.
   *
   * <p>This method runs securely within a try-catch block. Implementations should focus solely on
   * the persistence logic (e.g., executing a DB insert or sending a Kafka message).
   *
   * @param input the external input data
   * @return a unique ID to be used as the tracking identifier
   * @throws Exception any internal failure; the Auditor base class will catch and suppress it
   */
  protected abstract UUID doSave(I input);

  /**
   * Implementation hook for saving the successful output payload.
   *
   * @param trackingId the generated tracking identifier
   * @param output the resulting output data
   * @throws Exception any internal failure; the Auditor base class will catch and ignore it
   */
  protected abstract void doSave(UUID trackingId, O output);

  /**
   * Implementation hook for recording an execution failure.
   *
   * @param trackingId the generated tracking identifier
   * @param exception the exception caught during the main execution
   * @throws Exception any internal failure; the Auditor base class will suppress it onto the
   *     primary exception
   */
  protected abstract void doError(UUID trackingId, Exception exception);

  /**
   * Saves the input payload before protocol execution and returns a unique tracking identifier.
   *
   * @param input the external input data to be sent or published
   * @return a unique ID for tracking the entire lifecycle of this interaction
   */
  public final UUID save(I input) {
    try {
      return doSave(input);
    } catch (Exception e) {
      return UUID.randomUUID();
    }
  }

  /**
   * Saves the successful output payload after protocol execution completes.
   *
   * @param trackingId the unique ID returned by {@link #save(Object)}
   * @param output the resulting output data from the external system
   */
  public final void save(UUID trackingId, O output) {
    try {
      doSave(trackingId, output);
    } catch (Exception e) {
      // Swallow exception
    }
  }

  /**
   * Records the exception if the protocol execution fails.
   *
   * @param trackingId the unique ID returned by {@link #save(Object)}
   * @param exception the execution error that was intercepted
   */
  public final void error(UUID trackingId, Exception exception) {
    try {
      doError(trackingId, exception);
    } catch (Exception auditEx) {
      exception.addSuppressed(auditEx);
    }
  }
}
