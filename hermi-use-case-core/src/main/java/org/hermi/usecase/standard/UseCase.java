package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a use case for business logic execution.
 *
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class UseCase<C extends Validatable, R> extends Executor<C, R> {

  /**
   * Executes the business logic of the use case.
   *
   * <p>When implementing a use case in the <b>Use Case Layer</b>, follow these steps:
   *
   * <ol>
   *   <li><b>Define the Contract</b>: Specify the command type {@code C} (must implement {@link
   *       org.hermi.usecase.commons.validation.Validatable Validatable}) and result type {@code R}.
   *   <li><b>Provide the Implementation</b>: Create a default implementation that:
   *       <ul>
   *         <li>Performs business rule validation and logic.
   *         <li>Orchestrates I/O operations by calling {@link org.hermi.usecase.standard.Client
   *             Client}, {@link org.hermi.usecase.standard.Messenger Messenger}, or {@link
   *             org.hermi.usecase.standard.Repository Repository} components.
   *       </ul>
   * </ol>
   *
   * <p>Example:
   *
   * <pre>{@code
   * public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
   *   public static record Command(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public static record Result(String name, String email) {}
   * }
   *
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
   *     // 2. Save user to database
   *     userRepository.send(new SaveUserRepository.Command(clientResult.name(), clientResult.email()));
   *
   *     // 3. Send notification message
   *     messenger.send(new UserNotificationMessenger.Command(clientResult.email(), "User found: " + clientResult.name()));
   *
   *     return new Result(clientResult.name(), clientResult.email());
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
