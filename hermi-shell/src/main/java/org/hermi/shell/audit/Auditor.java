package org.hermi.shell.audit;

import java.util.UUID;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Side-car Telemetry / Audit Recorder for external interactions.
 *     <p>DESIGN INTENT: Provides immutable compliance logs that persist independently of the
 *     primary transaction.
 *     <p>PURPOSE: Ensures interaction history is captured without interrupting service
 *     availability.
 *     <p>Phase: 2
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. Do not define instance
 *           variables to store request-specific data.
 *       <li>2. INDEPENDENT TRANSACTION: Implementations MUST use a new transaction to ensure audit
 *           persistence regardless of the caller's transaction outcome.
 *       <li>3. NO LOCAL TRY-CATCH: Subclasses MUST NOT use local try-catch; safety and suppression
 *           logic are managed by the base class {@code final} methods.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER override final methods (save, error).
 *       <li>DO NOT inject business services into the auditor to avoid circular dependencies.
 *       <li>NO REDUNDANT LOGGING: Avoid {@code log.error()} in {@code doError} as the
 *           infrastructure already handles failure telemetry.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class PaymentAuditor extends Auditor<PaymentReq, PaymentRes> {
 *     private final AuditRepository repository;
 *
 *     @Override protected UUID doSave(PaymentReq paymentReq) { return repository.insertInitial(paymentReq); }
 *     @Override protected void doSave(UUID id, PaymentRes paymentRes) { repository.updateSuccess(id, paymentRes); }
 *     @Override protected void doError(UUID id, Exception e) { repository.markAsFailed(id, e.getMessage()); }
 * }
 * }</pre>
 */

/**
 * Generic base class for auditing external interactions within the Hermi framework.
 *
 * @param <I> input type (request or message payload)
 * @param <O> output type (response or result payload)
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
