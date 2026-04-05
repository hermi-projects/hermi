package org.hermi.usecase.standard;

import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a business use case.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
 */
public abstract class UseCase<C extends Validatable, R> extends Executor<C, R> {

  /**
   * Executes the business logic of the use case.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the boundary (Input/Output) and establishes the
   *       core business logic. Discovers and defines I/O contracts (Client, Repository, Messenger)
   *       Just-In-Time as needed. Verification uses stateful, technology-agnostic test shells.
   *   <li><b>Shell Layer (Phase 2)</b>: Orchestrates the execution by injecting production-grade
   *       infrastructure adapters (e.g., from a Spring Web Layer) into the Use Case.
   * </ul>
   *
   * <p>Example FindUserUseCase Contract & Core Logic (Phase 1):
   *
   * <pre>{@code
   * public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {
   *   public static record Context(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public static record Result(String name, String email) {}
   * }
   *
   * public record User(String ssn, String name, String email) {}
   *
   * public class DefaultFindUserUseCase extends FindUserUseCase {
   *   private final FindUserClient findUserClient;
   *   private final SaveUserRepository saveUserRepository;
   *   private final UserFoundMessenger messenger;
   *
   *   public DefaultFindUserUseCase(
   *       FindUserClient findUserClient,
   *       SaveUserRepository saveUserRepository,
   *       NotifyUserFoundMessenger messenger) {
   *     this.findUserClient = findUserClient;
   *     this.saveUserRepository = saveUserRepository;
   *     this.messenger = messenger;
   *   }
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     // 1. Fetch user data via the client contract
   *     FindUserClient.Result apiResult = findUserClient.execute(new FindUserClient.Context(context.ssn()));
   *     User user = new User(context.ssn(), apiResult.name(), apiResult.email());
   *
   *     // 2. Save the user via the repository contract
   *     saveUserRepository.execute(new SaveUserRepository.Context(user.name(), user.email()));
   *
   *     // 3. Send notification
   *     messenger.execute(new NotifyUserFoundMessenger.Context(user.email(), "User found: " + user.name()));
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
   *                          KafkaNotifyUserFoundMessenger messenger) {
   *     // Instantiate the Use Case with production adapters
   *     this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
   *   }
   *
   *   public FindUserUseCase.Result findUser(FindUserUseCase.Context context) {
   *     return findUserUseCase.execute(context);
   *   }
   * }
   *
   * @RestController
   * @RequestMapping("/users")
   * public class FindUserApiShell {
   *   private final FindUserService findUserService;
   *
   *   @Autowired
   *   public FindUserApiShell(FindUserService findUserService) {
   *     this.findUserService = findUserService;
   *   }
   *
   *   @GetMapping
   *   public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Context context) {
   *     return findUserService.findUser(context);
   *   }
   * }
   * }</pre>
   *
   * @param input the use case input
   * @return the use case output
   */
  protected abstract R doExecute(C context);
}
