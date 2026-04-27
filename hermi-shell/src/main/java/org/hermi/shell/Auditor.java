package org.hermi.shell;

import java.util.UUID;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Side-car Telemetry / Audit Recorder for external interactions.
 *     <p>DESIGN INTENT: Provides immutable compliance logs. Audit records MUST persist even if the
 *     primary business transaction rolls back.
 *     <p>PURPOSE: Ensures interaction history is captured without interrupting service
 *     availability.
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. Do not define instance
 *           variables to store request-specific data.
 *       <li>2. INDEPENDENT TRANSACTION: Implementations MUST use {@code @Transactional(propagation
 *           = Propagation.REQUIRES_NEW)} to ensure audit persistence regardless of the caller's
 *           transaction outcome.
 *       <li>3. NO LOCAL TRY-CATCH: Subclasses MUST NOT use local try-catch; safety and suppression
 *           logic are managed by the base class {@code final} methods.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER override final methods (save, error).
 *       <li>DO NOT inject business services into the auditor to avoid circular dependencies.
 *       <li>NO REDUNDANT LOGGING: Avoid {@code log.error()} in {@code doError} as the base class or
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
 * Generic interface for auditing external interactions.
 *
 * <p>Auditing is a side-car telemetry process designed to provide immutable compliance logs. This
 * class ensures that interaction history is captured reliably through a fail-safe, non-blocking
 * execution boundary that operates independently of the primary business flow.
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
