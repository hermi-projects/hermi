package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.Auditor;

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
 *
 * import org.hermi.commons.audit.Auditor;
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
public abstract class Messenger<M, R> extends Executor<M, R> {

  protected Messenger(Auditor<M, R> auditor) {
    setAuditor(auditor);
  }

  /**
   * Implementation hook for executing the underlying messaging protocol (e.g., Kafka, JMS, SQS).
   * Transactional Outbox Pattern
   *
   * @param message the message to publish to the external system
   * @return the result received from the external system
   */
  protected abstract R doPublish(M message);

  /**
   * Publishes the message with full auditing lifecycle protection.
   *
   * @param message the message to publish to the external system
   * @return the result received from the external system
   */
  public final R publish(M message) {
    return execute(message);
  }

  @Override
  protected final R doExecute(M message) {
    return doPublish(message);
  }
}
