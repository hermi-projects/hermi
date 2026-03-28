package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a messenger for message sending, including email, SMS, Kafka, etc.
 *
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class Messenger<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Sends the message to an external system and returns the response.
   *
   * <p>This method following a two-layer architectural pattern:
   *
   * <ul>
   *   <li><b>Use Case Layer</b>: Defines the contract by extending this class and specifying the
   *       command type {@code C} and result type {@code R}.
   *   <li><b>Shell Layer</b>: Implements the real-world communication logic, typically by adopting
   *       the adapter design pattern by implementing {@link
   *       org.hermi.shell.adapter.MessengerAdapter MessengerAdapter} to coordinate data
   *       transformation and message transmission.
   * </ul>
   *
   * <p>Example UserNotificationMessenger Contract in Use Case Layer:
   *
   * <pre>{@code
   * public abstract class UserNotificationMessenger extends Messenger<UserNotificationMessenger.Command, UserNotificationMessenger.Result> {
   *   public static record Command(String userId, String message) implements Validatable {}
   *   public static record Result(String messageId) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example KafkaUserNotificationMessenger Implementation in Shell Layer:
   *
   * <pre>{@code
   * public class KafkaUserNotificationMessenger extends UserNotificationMessenger
   *     implements MessengerAdapter<ProducerRecord<String, String>, RecordMetadata, UserNotificationMessenger.Command, UserNotificationMessenger.Result> {
   *
   *   private final KafkaTemplate<String, String> kafkaTemplate;
   *
   *   @Override
   *   protected Result doSend(Command command) {
   *     ProducerRecord<String, String> record = convertCommand(command);
   *     RecordMetadata metadata = process(record);
   *     return convertResult(metadata);
   *   }
   *
   *   @Override
   *   public ProducerRecord<String, String> convertCommand(Command command) {
   *     return new ProducerRecord<>("user.notifications", command.message());
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
   *   public Result convertResult(RecordMetadata metadata) {
   *     return new Result(metadata.toString());
   *   }
   * }
   * }</pre>
   *
   * @param command the message request command
   * @return the message response result
   */
  protected abstract R doSend(C command);

  public R send(C command) {
    return run(command);
  }

  public R send(Convertible<C> convertibleCommand) {
    Objects.requireNonNull(
        convertibleCommand, getSimpleClassName() + ", convertible command cannot be null");
    return send(convertibleCommand.convert());
  }

  public <S> R send(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return send(converter.convert(source));
  }

  @Override
  protected R doRun(C command) {
    return doSend(command);
  }
}
