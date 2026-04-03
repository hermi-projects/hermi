package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a repository for data access, including database, file system,
 * memory, etc.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
public abstract class Repository<I, O extends Validatable> extends Executor<I, O> {

  /**
   * Saves the repository request to an external data source and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input type {@code I} and output type {@code O}. Only the Output (which
   *       returns to the Use Case) typically implements {@link
   *       org.hermi.usecase.commons.validation.Validatable Validatable}.
   *   <li><b>Shell Layer (Phase 2)</b>: Implements the real-world data access logic using specific
   *       technologies, prefixed with the technology name (e.g., {@code JpaSaveUserRepository}).
   * </ul>
   *
   * <p>Example SaveUserRepository Contract in Use Case (Phase 1):
   *
   * <pre>{@code
   * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Input, SaveUserRepository.Output> {
   *   public static record Input(String name, String email) {}
   *   public static record Output(String id) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example SaveUserRepository Implementation in Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class JpaSaveUserRepository extends SaveUserRepository
   *     implements RepositoryAdapter<UserEntity, UserEntity, SaveUserRepository.Input, SaveUserRepository.Output> {
   *
   *   private final UserJpaRepository jpaRepository;
   *
   *   @Override
   *   protected Output doSend(Input input) {
   *     UserEntity entity = convertInput(input);
   *     UserEntity savedEntity = process(entity);
   *     return convertOutput(savedEntity);
   *   }
   *
   *   @Override
   *   public UserEntity convertInput(Input input) {
   *     return new UserEntity(input.name(), input.email());
   *   }
   *
   *   @Override
   *   public UserEntity process(UserEntity entity) {
   *     return jpaRepository.save(entity);
   *   }
   *
   *   @Override
   *   public Output convertOutput(UserEntity entity) {
   *     return new Output(entity.getId());
   *   }
   * }
   * }</pre>
   *
   * @param input the repository request input
   * @return the repository response output
   */
  protected abstract O doSend(I input);

  public O send(I input) {
    return run(input);
  }

  public O send(Convertible<I> convertibleInput) {
    Objects.requireNonNull(
        convertibleInput, getSimpleClassName() + ", convertible input cannot be null");
    return send(convertibleInput.convert());
  }

  public <S> O send(S source, Converter<S, I> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return send(converter.convert(source));
  }

  @Override
  protected O doRun(I input) {
    return doSend(input);
  }
}
