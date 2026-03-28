package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a business use case.
 *
 * <p>Implementation of a use case follows a two-phase life cycle:
 *
 * <h3>Phase 1: Use Case Layer (Core Business Logic)</h3>
 *
 * <ol>
 *   <li><b>Define the Contract</b>: Specify the command type {@code C} (typically {@link
 *       org.hermi.usecase.commons.validation.Validatable Validatable}) and result type {@code R}.
 *   <li><b>Start Implementation</b>: Create the default implementation of the use case.
 *   <li><b>Discover & Define I/O Contracts (JIT)</b>: As you implement business logic, if you need
 *       to talk to an external system, define the contract (Client, Repository, or Messenger) right
 *       then.
 *   <li><b>Verify with JUnit Shell</b>: Verify the logic using minimal technology-prefixed
 *       implementations (e.g., {@code MemorySaveUserRepository}) in your test suite.
 * </ol>
 *
 * <h3>Phase 2: Shell Layer (Infrastructure)</h3>
 *
 * <ol>
 *   <li><b>Implement Real Adapters</b>: Create production implementations using specific technology
 *       prefixes (e.g., {@code JpaSaveUserRepository}, {@code KafkaUserNotificationMessenger}).
 * </ol>
 *
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class UseCase<C extends Validatable, R> extends Executor<C, R> {

  /**
   * Executes the business logic of the use case.
   *
   * <p>Example: Find User Use Case
   *
   * <pre>{@code
   * // 1. Use Case Contract & Scoped Domain Model
   * public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
   *   public static record Command(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public static record Result(String name, String email) {}
   * }
   *
   * public record User(String ssn, String name, String email) {}
   *
   * // 2. JIT I/O Contracts discovered during implementation
   * public abstract class FindUserClient extends Client<FindUserClient.Command, FindUserClient.Result> { ... }
   * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Command, SaveUserRepository.Result> { ... }
   * public abstract class UserNotificationMessenger extends Messenger<UserNotificationMessenger.Command, UserNotificationMessenger.Result> { ... }
   *
   * // 3. Use Case Implementation
   * public class DefaultFindUserUseCase extends FindUserUseCase {
   *   private final SaveUserRepository userRepository;
   *   private final FindUserClient findUserClient;
   *   private final UserNotificationMessenger messenger;
   *
   *   public DefaultFindUserUseCase(
   *       SaveUserRepository userRepository,
   *       FindUserClient findUserClient,
   *       UserNotificationMessenger messenger) {
   *     this.userRepository = userRepository;
   *     this.findUserClient = findUserClient;
   *     this.messenger = messenger;
   *   }
   *
   *   @Override
   *   protected Result doExecute(Command command) {
   *     // 1. Find user from 3rd party API
   *     FindUserClient.Result clientResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
   *
   *     // 2. Map to Domain Model
   *     User user = new User(command.ssn(), clientResult.name(), clientResult.email());
   *
   *     // 3. Save user to database
   *     userRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
   *
   *     // 4. Send notification message
   *     messenger.send(new UserNotificationMessenger.Command(user.email(), "User found: " + user.name()));
   *
   *     return new Result(user.name(), user.email());
   *   }
   * }
   * }</pre>
   *
   * @param command the use case command
   * @return the use case result
   */
  protected abstract R doExecute(C command);

  public R execute(C command) {
    return run(command);
  }

  public R execute(Convertible<C> command) {
    Objects.requireNonNull(command, getSimpleClassName() + ", convertible command cannot be null");
    return execute(command.convert());
  }

  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return execute(converter.convert(source));
  }

  @Override
  protected R doRun(C command) {
    return doExecute(command);
  }
}
