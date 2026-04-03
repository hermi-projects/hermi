package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a business use case.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
public abstract class UseCase<I extends Validatable, O> extends Executor<I, O> {

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
   * public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Input, FindUserUseCase.Output> {
   *   public static record Input(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public static record Output(String name, String email) {}
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
   *   protected Output doExecute(Input input) {
   *     // 1. Fetch user data via the client contract
   *     FindUserClient.Output apiResult = findUserClient.send(new FindUserClient.Input(input.ssn()));
   *     User user = new User(input.ssn(), apiResult.name(), apiResult.email());
   *
   *     // 2. Save the user via the repository contract
   *     saveUserRepository.send(new SaveUserRepository.Input(user.name(), user.email()));
   *
   *     // 3. Send notification
   *     messenger.publish(new UserNotificationMessenger.Input(user.email(), "User found: " + user.name()));
   *
   *     return new Output(user.name(), user.email());
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
   *   public FindUserUseCase.Output findUser(FindUserUseCase.Input input) {
   *     return findUserUseCase.execute(input);
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
   *   public FindUserUseCase.Output findUser(@RequestBody FindUserUseCase.Input input) {
   *     return findUserService.findUser(input);
   *   }
   * }
   * }</pre>
   *
   * @param input the use case input
   * @return the use case output
   */
  protected abstract O doExecute(I input);

  public O execute(I input) {
    return run(input);
  }

  public O execute(Convertible<I> input) {
    Objects.requireNonNull(input, getSimpleClassName() + ", convertible input cannot be null");
    return execute(input.convert());
  }

  public <S> O execute(S source, Converter<S, I> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return execute(converter.convert(source));
  }

  @Override
  protected O doRun(I input) {
    return doExecute(input);
  }
}
