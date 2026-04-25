package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.validation.Validatable;

/**
 * An abstract class representing a repository for data access, including database, file system,
 * memory, etc.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
 */
public abstract class Repository<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Saves the repository request to an external data source and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input type {@code I} and output type {@code O}. Only the Output (which
   *       returns to the Use Case) typically implements {@link Validatable Validatable}.
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
   *     implements Adapter<SaveUserRepository.Context, SaveUserRepository.Result, UserEntity, UserEntity> {
   *
   *   private final UserJpaRepository jpaRepository;
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     UserEntity entity = convertContext(context);
   *     UserEntity savedEntity = process(entity);
   *     return convertResult(savedEntity);
   *   }
   *
   *   @Override
   *   public UserEntity convertContext(Context context) {
   *     return new UserEntity(context.name(), context.email());
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
   * @param context the repository request context
   * @return the repository response result
   */
  protected abstract R doExecute(C context);
}
