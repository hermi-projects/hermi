package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a messenger for message sending, including email, SMS, Kafka, etc.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
public abstract class Messenger<I, O extends Validatable> extends Executor<I, O> {

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
   *       KafkaUserFoundMessenger}).
   * </ul>
   *
   * <p>Example UserFoundMessenger Contract in Use Case Layer (Phase 1):
   *
   * <pre>{@code
   * public abstract class UserFoundMessenger extends Messenger<UserFoundMessenger.Input, UserFoundMessenger.Output> {
   *   public static record Input(String userId, String message) {}
   *   public static record Output(String messageId) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example KafkaUserFoundMessenger Implementation in Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class KafkaUserFoundMessenger extends UserFoundMessenger
   *     implements MessengerAdapter<ProducerRecord<String, String>, RecordMetadata, UserFoundMessenger.Input, UserFoundMessenger.Output> {
   *
   *   private final KafkaTemplate<String, String> kafkaTemplate;
   *
   *   @Override
   *   protected Output doPublish(Input input) {
   *     ProducerRecord<String, String> record = convertInput(input);
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
   * @param input the message request input
   * @return the message response output
   */
  protected abstract O doPublish(I input);

  public O publish(I input) {
    return run(input);
  }

  public O publish(Convertible<I> convertibleInput) {
    Objects.requireNonNull(
        convertibleInput, getSimpleClassName() + ", convertible input cannot be null");
    return publish(convertibleInput.convert());
  }

  public <S> O publish(S source, Converter<S, I> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return publish(converter.convert(source));
  }

  @Override
  protected O doRun(I input) {
    return doPublish(input);
  }
}
