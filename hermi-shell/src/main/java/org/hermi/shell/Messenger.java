package org.hermi.shell;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.Executor;
import org.hermi.shell.audit.NoOpPersistentAuditor;
import org.hermi.shell.audit.PersistentAuditor;

/**
 * <b>AI INSTRUCTION:</b> When subclassing, ONLY override {@code doPublish}. Do NOT add telemetry or
 * try-catch bounds. Return the raw vendor metadata directly. Always pass a {@link
 * PersistentAuditor} to the constructor — use {@link org.hermi.shell.audit.NoOpPersistentAuditor}
 * for Phase 1 validation.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class KafkaUserMessenger extends Messenger<ProducerRecord, RecordMetadata> {
 *   public KafkaUserMessenger(PersistentAuditor<ProducerRecord, RecordMetadata> auditor) {
 *     super(auditor);
 *   }
 *   protected RecordMetadata doPublish(ProducerRecord msg) { return kafkaTemplate.send(msg).get(); }
 * }
 * }</pre>
 */

/**
 * Base class for vendor-specific messaging clients (Protocol layer).
 *
 * <p><b>AI-Friendly Architecture</b>: Follow the decoupled pattern to stay within AI context limits
 * — a concrete messenger composes a VendorMessenger (inheriting from this class for protocol and
 * auditing) with a {@link Mapper} for domain-vendor translation.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class Messenger<P, R> extends Executor<P, R> {

  private final PersistentAuditor<P, R> persistentAuditor;

  /**
   * Constructs a Messenger with a {@link PersistentAuditor}. The built-in {@link
   * org.hermi.commons.audit.LogAuditor} is always active for debug logging.
   *
   * @param persistentAuditor the persistent auditor for compliance/production audit
   */
  protected Messenger(PersistentAuditor<P, R> persistentAuditor) {
    this.persistentAuditor =
        Objects.requireNonNullElse(persistentAuditor, new NoOpPersistentAuditor<>());
  }

  /**
   * Implementation hook for executing the underlying messaging protocol (e.g., Kafka, JMS, SQS).
   * Transactional Outbox Pattern
   *
   * @param payload the payload to publish to the external system
   * @return the result received from the external system
   */
  protected abstract R doPublish(P payload);

  /**
   * Publishes the message with full auditing lifecycle protection.
   *
   * @param payload the payload to publish to the external system
   * @return the result received from the external system
   */
  public final R publish(P payload) {
    return execute(payload);
  }

  @Override
  protected final R doExecute(P payload) {
    UUID auditId = persistentAuditor.recordContext(payload);
    try {
      R result = doPublish(payload);
      persistentAuditor.recordResult(auditId, result);
      return result;
    } catch (Exception e) {
      persistentAuditor.recordError(auditId, payload, e);
      throw e;
    }
  }
}
