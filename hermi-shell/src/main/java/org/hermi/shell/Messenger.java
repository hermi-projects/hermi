package org.hermi.shell;

import org.hermi.shell.audit.AuditedExecutor;
import org.hermi.shell.audit.Auditor;

/**
 * <b>AI INSTRUCTION:</b> When subclassing, ONLY override {@code doPublish}. Do NOT add telemetry or
 * try-catch bounds. Return the raw vendor metadata directly.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class KafkaUserMessenger extends Messenger<ProducerRecord, RecordMetadata> {
 *   public KafkaUserMessenger(Auditor<ProducerRecord, RecordMetadata> auditor) { super(auditor); }
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
public abstract class Messenger<P, R> extends AuditedExecutor<P, R> {

  /**
   * Constructs a Messenger with the required auditor.
   *
   * @param auditor the auditor to wrap all publish operations
   */
  protected Messenger(Auditor<P, R> auditor) {
    super(auditor);
  }

  /**
   * Implementation hook for executing the underlying messaging protocol (e.g., Kafka, JMS, SQS).
   * Transactional Outbox Pattern
   *
   * @param payload the vendor-specific message payload
   * @return the native vendor-specific metadata or result
   */
  protected abstract R doPublish(P payload);

  /**
   * Publishes the message with full auditing lifecycle protection.
   *
   * <p>This method guarantees that all asynchronous publications are mechanically wrapped by the
   * {@link Auditor}.
   *
   * @param payload the vendor message payload
   * @return the vendor result payload
   */
  public final R publish(P payload) {
    return execute(payload);
  }

  @Override
  protected final R doExecute(P payload) {
    return doPublish(payload);
  }
}
