package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.validation.Validatable;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Asynchronous Event Dispatcher.
 *     <p>DESIGN INTENT: Decouple domain logic from messaging protocols (Kafka, RabbitMQ, Email).
 *     <p>PURPOSE: Provide a technology-neutral interface for asynchronous outbound communication.
 *     <p>Phase: 1 (Contract Discovery)
 *     <p>Priority: 4 (Critical Core)
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific
 *           instance variables. Only final, immutable dependencies (via constructor injection) are
 *           allowed.
 *       <li>2. NO BROKER TYPES: NEVER use broker-specific types (e.g., ProducerRecord, Message) in
 *           contract records.
 *       <li>3. PURE JAVA TYPES: Records MUST use ONLY plain Java types (String, UUID, BigDecimal,
 *           etc.).
 *       <li>4. NAMING PROPHECY: Follow the {@code {Notify}{Fact}Messenger} pattern (e.g., {@code
 *           NotifyUserFoundMessenger}).
 *       <li>5. VALIDATION: The {@code Result} record MUST implement {@link Validatable}.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>DO NOT leak infrastructure-specific metadata (headers, partitions) into the domain
 *           context.
 *       <li>NEVER add implementation logic in Phase 1; only define the I/O records.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
 *   public static record Context(String userId, String message) {}
 *   public static record Result(String messageId) implements Validatable {}
 * }
 * }</pre>
 */

/**
 * Base class for all asynchronous messaging contracts in the Hermi framework.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result, which MUST implement {@link Validatable}
 */
public abstract class Messenger<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Sends the message to an external system and returns the response.
   *
   * <p>This method following a two-layer architectural pattern:
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input and output types. Only the output (which returns to the Use Case)
   *       typically implements {@link Validatable Validatable}.
   *   <li><b>Shell Layer (Phase 2)</b>: Implements the real-world communication logic using
   *       specific technologies, prefixed with the technology name (e.g., {@code
   *       KafkaNotifyUserFoundMessenger}).
   * </ul>
   *
   * <p>Example NotifyUserFoundMessenger Contract in Use Case Layer (Phase 1):
   *
   * <pre>{@code
   * public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
   *   public static record Context(String userId, String message) {}
   *   public static record Result(String messageId) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example KafkaNotifyUserFoundMessenger Implementation in Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger {
   *
   *   private final org.hermi.shell.Messenger<ProducerRecord<String, String>, RecordMetadata> vendorMessenger;
   *   private final Mapper<Context, Result, ProducerRecord<String, String>, RecordMetadata> mapper;
   *
   *   public KafkaNotifyUserFoundMessenger(
   *       org.hermi.shell.Messenger<ProducerRecord<String, String>, RecordMetadata> vendorMessenger,
   *       Mapper<Context, Result, ProducerRecord<String, String>, RecordMetadata> mapper) {
   *     this.vendorMessenger = vendorMessenger;
   *     this.mapper = mapper;
   *   }
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     ProducerRecord<String, String> record = mapper.toPayload(context);
   *     RecordMetadata metadata = vendorMessenger.publish(record);
   *     return mapper.toResult(metadata);
   *   }
   * }
   * }</pre>
   *
   * @param context the message request context
   * @return the message response result
   */
  protected abstract R doExecute(C context);
}
