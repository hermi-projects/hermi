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
 * <p><b>AI-Friendly Architecture (Rule of Three)</b>: Follow the decoupled pattern to stay within
 * AI context limits:
 *
 * <ol>
 *   <li><b>VendorMessenger</b>: Inherits from this class, handles protocol (Kafka, JMS) and
 *       auditing.
 *   <li><b>Mapper</b>: Handles translation between Domain Fact and Vendor Record.
 *   <li><b>Adapter</b>: Wires Messenger and Mapper.
 * </ol>
 *
 * @param <M> Vendor message type (e.g., ProducerRecord)
 * @param <R> Vendor result type (e.g., RecordMetadata)
 */
public abstract class Messenger<M, R> extends AuditedExecutor<M, R> {

  /**
   * Constructs a Messenger with the required auditor.
   *
   * @param auditor the auditor to wrap all publish operations
   */
  protected Messenger(Auditor<M, R> auditor) {
    super(auditor);
  }

  /**
   * Implementation hook for executing the underlying messaging protocol (e.g., Kafka, JMS, SQS).
   * Transactional Outbox Pattern
   *
   * @param message the vendor-specific message payload
   * @return the native vendor-specific metadata or result
   */
  protected abstract R doPublish(M message);

  /**
   * Publishes the message with full auditing lifecycle protection.
   *
   * <p>This method guarantees that all asynchronous publications are mechanically wrapped by the
   * {@link Auditor}.
   *
   * @param message the vendor message payload
   * @return the vendor result payload
   */
  public final R publish(M message) {
    return execute(message);
  }

  @Override
  protected R doExecute(M message) {
    return doPublish(message);
  }
}
