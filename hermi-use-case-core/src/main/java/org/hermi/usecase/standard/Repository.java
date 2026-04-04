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
public abstract class Repository<C, R extends Validatable> extends Executor<C, R> {

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
   * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
   *   public static record Context(String name, String email) {}
   *   public static record Result(String id) implements Validatable {}
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
   *   protected Result doExecute(Context context) {
   *     UserEntity entity = convertInput(context);
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
   * @param context the repository request context
   * @return the repository response result
   */
  protected abstract R doExecute(C context);

  public R execute(Convertible<C> convertibleContext) {
    Objects.requireNonNull(
        convertibleContext, getSimpleClassName() + ", convertible context cannot be null");
    return execute(convertibleContext.convert());
  }

  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return execute(converter.convert(source));
  }
}
