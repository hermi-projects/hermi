package org.hermi.usecase.standard;

import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a messenger for message sending, including email, SMS, Kafka, etc.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
 */
public abstract class Messenger<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Sends the message to an external system and returns the response.
   *
   * <p>This method following a two-layer architectural pattern:
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input type {@code I} and output type {@code O}. Only the Output (which
   *       returns to the Use Case) typically implements {@link
   *       org.hermi.usecase.commons.validation.Validatable Validatable}.
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
   * public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger
   *     implements Adapter<ProducerRecord<String, String>, RecordMetadata, NotifyUserFoundMessenger.Input, NotifyUserFoundMessenger.Output> {
   *
   *   private final KafkaTemplate<String, String> kafkaTemplate;
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     ProducerRecord<String, String> record = convertInput(context);
   *     RecordMetadata metadata = process(record);
   *     return convertOutput(metadata);
   *   }
   *
   *   @Override
   *   public ProducerRecord<String, String> convertInput(Input input) {
   *     return new ProducerRecord<>("user.notifications", input.message());
   *   }
   *
   *   @Override
   *   public RecordMetadata process(ProducerRecord<String, String> record) {
   *     try {
   *       return kafkaTemplate.send(record).get().getRecordMetadata();
   *     } catch (Exception e) {
   *       throw new RuntimeException(e);
   *     }
   *   }
   *
   *   @Override
   *   public Output convertOutput(RecordMetadata metadata) {
   *     return new Output(metadata.toString());
   *   }
   * }
   * }</pre>
   *
   * @param context the message request context
   * @return the message response result
   */
  protected abstract R doExecute(C context);
}
