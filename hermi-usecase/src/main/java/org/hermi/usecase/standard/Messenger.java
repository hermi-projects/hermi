package org.hermi.usecase.standard;

import org.hermi.commons.Executor;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Defines a technology-neutral contract for asynchronous outbound
 * notification. Specifies input ({@code Context}) and output ({@code Result}) using plain Java
 * types only. WHY: Decouple domain logic from messaging protocols. The use case announces facts
 * (e.g., "user was found") without knowing whether the transport is Kafka, RabbitMQ, or Email. WHO:
 * Defined by Use Case layer as an abstract contract. Called by {@code doExecute} in a use case.
 * Implemented by Shell layer with technology-specific adapters (e.g., {@code
 * KafkaNotifyUserFoundMessenger}). WHEN: Phase 1: define the contract skeleton (Context/Result
 * records only). Phase 2: implement real messaging logic with the chosen broker or protocol. WHERE:
 * Use Case layer — contract definition only. The implementation lives in Shell layer, prefixed with
 * the technology name (e.g., {@code Kafka}, {@code Rabbit}, {@code Email}). HOW: Extend with nested
 * static {@code Context} and {@code Result} records. Records use ONLY plain Java types ({@code
 * String}, {@code UUID}, {@code BigDecimal}). Name the contract {@code Notify{Fact}Messenger}.
 * Phase 1 {@code doExecute} returns {@code null}; Phase 2 adapters implement real logic.
 *
 * <p>DO NOT add: - broker-specific types ({@code ProducerRecord}, {@code Message}) in
 * Context/Result records - implementation logic in Phase 1 (contract definition only) - try-catch,
 * logging, or null checks (handled by {@link org.hermi.commons.Executor} base class) - business
 * logic (Messenger is a notification gateway, not a domain service)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Extends Messenger with nested static Context/Result records, pure Java types only
 * public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
 *   public static record Context(String userId, String message) {}
 *   public static record Result(String messageId) {}
 * }
 * // WRONG: NotifyUserFoundMessengerImpl, UserNotificationService — do NOT use these names
 * // WRONG: Do NOT add ProducerRecord, Message, or any broker types in records
 * }</pre>
 */

/** Asynchronous Notification Gateway: technology-neutral contract for outbound messaging. */

/**
 * Base class for all asynchronous messaging contracts in the Hermi framework. Extends {@link
 * org.hermi.commons.Executor} to provide a technology-neutral interface for asynchronous outbound
 * communication (Kafka, RabbitMQ, Email).
 *
 * @param <C> the type of the context (use plain Java types only)
 * @param <R> the type of the result (use plain Java types only)
 */
public abstract class Messenger<C, R> extends Executor<C, R> {
  /**
   * Sends the message to an external system.
   *
   * <p>Phase 1 (Use Case Layer): Define the contract by extending this class with Context/Result
   * records. Phase 2 (Shell Layer): Implement the real messaging logic using specific technologies
   * (e.g., {@code KafkaNotifyUserFoundMessenger}, {@code EmailNotifyUserFoundMessenger}).
   *
   * @param context the message request context
   * @return the message response result
   */
  @Override
  protected abstract R doExecute(C context);
}
