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
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class Repository<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Saves the repository request to an external data source and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer</b>: Defines the contract by extending this class and specifying the
   *       command type {@code C} and result type {@code R}.
   *   <li><b>Shell Layer</b>: Implements the real-world data access logic, typically by adopting
   *       the adapter design pattern by implementing {@link
   *       org.hermi.shell.adapter.RepositoryAdapter RepositoryAdapter} to coordinate data
   *       transformation and storage execution.
   * </ul>
   *
   * <p>Example SaveUserRepository Contract in Use Case:
   *
   * <pre>{@code
   * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Command, SaveUserRepository.Result> {
   *   public static record Command(String name, String email) implements Validatable {}
   *   public static record Result(String id) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example SaveUserRepository Implementation in Shell Layer:
   *
   * <pre>{@code
   * public class DefaultSaveUserRepository extends SaveUserRepository
   *     implements RepositoryAdapter<UserEntity, UserEntity, SaveUserRepository.Command, SaveUserRepository.Result> {
   *
   *   private final UserJpaRepository jpaRepository;
   *
   *   @Override
   *   protected Result doSend(Command command) {
   *     UserEntity entity = convertCommand(command);
   *     UserEntity savedEntity = process(entity);
   *     return convertResult(savedEntity);
   *   }
   *
   *   @Override
   *   public UserEntity convertCommand(Command command) {
   *     return new UserEntity(command.name(), command.email());
   *   }
   *
   *   @Override
   *   public UserEntity process(UserEntity entity) {
   *     return jpaRepository.save(entity);
   *   }
   *
   *   @Override
   *   public Result convertResult(UserEntity entity) {
   *     return new Result(entity.getId());
   *   }
   * }
   * }</pre>
   *
   * @param command the repository request command
   * @return the repository response result
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
