package org.hermi.shell;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.Executor;

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
public abstract class Messenger<M, R> extends Executor<M, R> {
  private final Auditor<M, R> auditor;

  protected Messenger(Auditor<M, R> auditor) {
    this.auditor = Objects.requireNonNull(auditor, "Auditor is required for Messenger");
  }

  /**
   * Implementation hook for executing the underlying messaging protocol (e.g., Kafka, JMS, SQS).
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
    UUID trackingId = auditor.save(message);
    try {
      R result = super.execute(message);
      auditor.save(trackingId, result);
      return result;
    } catch (Exception e) {
      auditor.error(trackingId, e);
      throw e;
    }
  }

  public final R execute(M message) {
    return this.publish(message);
  }

  @Override
  protected R doExecute(M message) {
    return doPublish(message);
  }
}
