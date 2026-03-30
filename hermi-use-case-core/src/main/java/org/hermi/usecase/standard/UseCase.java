package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a business use case.
 *
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class UseCase<C extends Validatable, R> extends Executor<C, R> {

  /**
   * Executes the business logic of the use case.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the boundary (Command/Result) and establishes
   *       the core business logic. Discovers and defines I/O contracts (Client, Repository,
   *       Messenger) Just-In-Time as needed. Verification uses stateful, technology-agnostic test
   *       shells.
   *   <li><b>Shell Layer (Phase 2)</b>: Orchestrates the execution by injecting production-grade
   *       infrastructure adapters (e.g., from a Spring Web Layer) into the Use Case.
   * </ul>
   *
   * <p>Example FindUserUseCase Contract & Core Logic (Phase 1):
   *
   * <pre>{@code
   * public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
   *   public record Command(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public record Result(String name, String email) {}
   * }
   *
   * public record User(String ssn, String name, String email) {}
   *
   * public class DefaultFindUserUseCase extends FindUserUseCase {
   *   private final FindUserClient findUserClient;
   *   private final SaveUserRepository saveUserRepository;
   *   private final UserNotificationMessenger messenger;
   *
   *   public DefaultFindUserUseCase(
   *       FindUserClient findUserClient,
   *       SaveUserRepository saveUserRepository,
   *       UserNotificationMessenger messenger) {
   *     this.findUserClient = findUserClient;
   *     this.saveUserRepository = saveUserRepository;
   *     this.messenger = messenger;
   *   }
   *
   *   @Override
   *   protected Result doExecute(Command command) {
   *     // 1. Fetch user data via the client contract
   *     FindUserClient.Result apiResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
   *     User user = new User(command.ssn(), apiResult.name(), apiResult.email());
   *
   *     // 2. Save the user via the repository contract
   *     saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
   *
   *     // 3. Send notification
   *     messenger.send(new UserNotificationMessenger.Command(user.email(), "User found: " + user.name()));
   *
   *     return new Result(user.name(), user.email());
   *   }
   * }
   * }</pre>
   *
   * <p>Example FindUserService Orchestration in Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Service
   * @Transactional
   * public class FindUserService {
   *   private final FindUserUseCase findUserUseCase;
   *
   *   @Autowired
   *   public FindUserService(LexisNexisFindUserClient client,
   *                          JdbcSaveUserRepository repo,
   *                          KafkaUserNotificationMessenger messenger) {
   *     // Instantiate the Use Case with production adapters
   *     this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
   *   }
   *
   *   public FindUserUseCase.Result findUser(FindUserUseCase.Command command) {
   *     return findUserUseCase.execute(command);
   *   }
   * }
   *
   * @RestController
   * @RequestMapping("/users")
   * public class FindUserController {
   *   private final FindUserService findUserService;
   *
   *   @Autowired
   *   public FindUserController(FindUserService findUserService) {
   *     this.findUserService = findUserService;
   *   }
   *
   *   @GetMapping
   *   public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Command command) {
   *     return findUserService.findUser(command);
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
