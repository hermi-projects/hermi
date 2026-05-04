package org.hermi.shell.audit;

import org.hermi.commons.audit.Auditor;

/**
 * Abstract {@link Auditor} for production-grade, persistent auditing of external interactions.
 *
 * <p>Designed for compliance and external audit requirements. Implementations persist payload,
 * result, and error information to a durable store (e.g., database, audit log service).
 *
 * <p>Contrast with the default {@link org.hermi.commons.audit.LogAuditor}, which writes to the
 * debug log — {@code PersistentAuditor} is for environments where a permanent, queryable audit
 * trail is required.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * public class PaymentAuditor extends PersistentAuditor<PaymentRequest, PaymentResponse> {
 *     private final AuditRepository repository;
 *
 *     public PaymentAuditor(AuditRepository repository) {
 *         this.repository = repository;
 *     }
 *
 *     @Override protected UUID doRecordContext(PaymentRequest request) {
 *         return repository.insert(request);
 *     }
 *
 *     @Override protected void doRecordResult(UUID id, PaymentResponse response) {
 *         repository.updateSuccess(id, response);
 *     }
 *
 *     @Override protected void doRecordError(UUID id, Exception exception) {
 *         repository.markFailed(id, exception.getMessage());
 *     }
 * }
 * }</pre>
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class PersistentAuditor<P, R> extends Auditor<P, R> {

  // PersistentAuditor inherits the full lifecycle from Auditor.
  // Subclasses implement doRecordContext, doRecordResult, and doRecordError
  // with persistent storage logic.
}
