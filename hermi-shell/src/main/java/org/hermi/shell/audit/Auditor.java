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
 *       <li>NEVER override final methods (recordPayload, recordResponse, recordError).
 *       <li>DO NOT inject business services into the auditor to avoid circular dependencies.
 *       <li>NO REDUNDANT LOGGING: Avoid {@code log.error()} in {@code doRecordError} as the
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
 *     @Override protected UUID doRecordPayload(PaymentReq paymentReq) { return repository.insertInitial(paymentReq); }
 *     @Override protected void doRecordResponse(UUID id, PaymentRes paymentRes) { repository.updateSuccess(id, paymentRes); }
 *     @Override protected void doRecordError(UUID id, Exception e) { repository.markAsFailed(id, e.getMessage()); }
 * }
 * }</pre>
 */

/**
 * Generic base class for auditing external interactions within the Hermi framework.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class Auditor<P, R> {

  /**
   * Implementation hook for saving the input payload.
   *
   * <p>This method runs securely within a try-catch block. Implementations should focus solely on
   * the persistence logic (e.g., executing a DB insert or sending a Kafka message).
   *
   * @param payload the external payload data
   * @return a unique ID to be used as the tracking identifier
   * @throws Exception any internal failure; the Auditor base class will catch and suppress it
   */
  protected abstract UUID doRecordPayload(P payload);

  /**
   * Implementation hook for saving the successful output payload.
   *
   * @param trackingId the generated tracking identifier
   * @param response the resulting response data
   * @throws Exception any internal failure; the Auditor base class will catch and ignore it
   */
  protected abstract void doRecordResponse(UUID trackingId, R response);

  /**
   * Implementation hook for recording an execution failure.
   *
   * @param trackingId the generated tracking identifier
   * @param exception the exception caught during the main execution
   * @throws Exception any internal failure; the Auditor base class will suppress it onto the
   *     primary exception
   */
  protected abstract void doRecordError(UUID trackingId, Exception exception);

  /**
   * Saves the input payload before protocol execution and returns a unique tracking identifier.
   *
   * @param payload the external payload data to be sent or published
   * @return a unique ID for tracking the entire lifecycle of this interaction
   */
  public final UUID recordPayload(P payload) {
    try {
      return doRecordPayload(payload);
    } catch (Exception e) {
      return UUID.randomUUID();
    }
  }

  /**
   * Saves the successful output payload after protocol execution completes.
   *
   * @param trackingId the unique ID returned by {@link #recordPayload(Object)}
   * @param response the resulting response data from the external system
   */
  public final void recordResponse(UUID trackingId, R response) {
    try {
      doRecordResponse(trackingId, response);
    } catch (Exception e) {
      // Swallow exception
    }
  }

  /**
   * Records the exception if the protocol execution fails.
   *
   * @param trackingId the unique ID returned by {@link #recordPayload(Object)}
   * @param exception the execution error that was intercepted
   */
  public final void recordError(UUID trackingId, Exception exception) {
    try {
      doRecordError(trackingId, exception);
    } catch (Exception auditEx) {
      exception.addSuppressed(auditEx);
    }
  }
}
