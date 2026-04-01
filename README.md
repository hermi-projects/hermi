# Hermi Framework

A lightweight, opinionated framework for building Java applications rooted in Clean Architecture and Engineering-First principles.

By enforcing a strict boundary between business logic and infrastructure, the Hermi framework ensures your system remains:
- **Independent of Frameworks**: Treat frameworks as tools, not constraints. Swap one for another without altering a single business rule.
- **Testable**: Verify business rules without UI, databases, or web servers.
- **Independent of UI & Database**: Swap a Web API for a CLI, or SQL for MongoDB, without touching a single line of core logic.
- **Independent of External Agencies**: Business rules know nothing about the outside world — and are never allowed to.

## Table of Contents
1. [Core Philosophy](#core-philosophy)
2. [Architectural Responsibilities](#architectural-responsibilities)
3. [The Engineering-First Lifecycle](#the-engineering-first-lifecycle)
4. [Progressive Tutorial: Implementation Workflow](#progressive-tutorial-implementation-workflow)
5. [Naming Conventions](#naming-conventions)
6. [Validation Rules](#validation-rules)
7. [Project Structure](#project-structure)

---

## 1. Core Philosophy

- **Business First**: The Use Case (Phase 1) must be fully functional using only plain Java and in-memory/console adapters before any Phase 2 infrastructure is written.
- **Just-In-Time (JIT) Contracts**: I/O contracts (`Client`, `Repository`, `Messenger`) are defined *exactly* when the business logic requires them, rather than attempting to guess external dependencies upfront.
- **No Mocks**: Verification uses stateful, technology-agnostic "Test Shells" (e.g., `InMemoryRepository`), rather than fragile mocking frameworks like Mockito.
- **Scoped Models**: Domain models are local to the use case package and are never shared globally. While this introduces intentional duplication (trading DRY for isolation), it guarantees independent evolvability and prevents the emergence of fragile "God-classes" where modifying a shared entity breaks unrelated features.

---

## 2. Architectural Responsibilities

The framework divides your application into two distinct, non-overlapping domains:

- **Use Case (The Core)**: Dictates _**What**_ the system does. This layer owns input validation, business rules, workflow orchestration, and domain models. 
- **Shell (The Infrastructure)**: Dictates _**How**_ the system does it. This layer implements the I/O contracts defined by the Use Case using specific technologies, vendors, or frameworks (e.g., Spring Data JPA, REST clients, LexisNexis APIs).

> [!IMPORTANT]
> The Use Case knows absolutely nothing about the Shell. The Shell depends entirely on the contracts defined by the Use Case.

---

## 3. The Engineering-First Lifecycle

To guarantee a clean separation of concerns, development in the Hermi Framework mandates a strict, two-phase implementation process. You must prove your business logic works before writing a single line of infrastructure-specific code.

### Traversal Strategy: Top-Down, Breadth-First

Phase 1 deliberately employs a **top-down, breadth-first traversal** over business logic. Rather than front-loading the design of all external dependencies before a single line of business logic exists — a speculative, depth-first approach — Phase 1 inverts this order entirely.

The engineer begins at the highest level of abstraction: what does this use case *accept*, and what does it *return*? From that boundary, the orchestration logic is written top-down, as if all necessary collaborators already exist. External dependencies — whether for data retrieval, persistence, or event publishing — are only defined at the exact moment the business logic *reveals* it needs them.

This is not a stylistic preference. It is a disciplined response to a well-known failure mode: **speculative dependency design**. In traditional depth-first development, engineers pre-design schemas, repositories, and API clients before any domain logic is written. This embeds infrastructure assumptions directly into the business domain before the domain is even understood. In Hermi, **business logic drives dependency discovery** — not the other way around.

> [!NOTE]
> This traversal strategy is also why the framework prohibits mocking frameworks. Mocks require pre-specifying dependencies that haven't been discovered yet, directly violating the JIT discovery principle. Local test adapters are the correct substitute because they are written *after* a contract is revealed — never before.

### Phase 1: The Core Logic Lifecycle (The Brain)
Phase 1 is about Discovery and Verification. It answers _"What"_ the system does — all business rules, workflow orchestration, and domain models — proven correct using exclusively pure Java:
1. **Establish the Boundary**: Define your `UseCase` contract (`Command` and `Result`).
2. **Initialize Core Implementation**: Create a skeletal `DefaultUseCase` class.
3. **The Test Harness**: Build a minimal test execution harness as a "Play Button" for continuous, instant execution and debugging during development.
4. **JIT I/O Discovery**: Write logic; define I/O contracts the moment you need a specific behavior, categorizing them strictly by architectural intent:
   - **`Client` (Inbound / Exchange)**: Retrieves external data or performs transactional external actions (e.g., 3rd-party APIs, RPCs).
   - **`Repository` (State Persistence)**: Manages the application's own persistent state (e.g., SQL, NoSQL, Caching).
   - **`Messenger` (Outbound)**: Fire-and-forget notifications to the rest of the world (e.g., Kafka, Event Bus, Emails).
5. **Final Orchestration**: Coordinate the flow between your discovered I/O contracts.
6. **The Phase 1 Gate**: Assert the logic against edge cases in the Test Shell. Phase 1 is complete when all edge cases pass.

### Phase 2: Shell (Infrastructure)
Phase 2 is about Implementation and Wiring. It answers _"How"_ the system does it — translating each I/O contract into a technology-specific adapter and wiring it into the chosen Shell.

7. **Implement Production Adapters**: For each I/O contract discovered in Phase 1, build the technology-specific or vendor-specific adapter class (e.g., `JdbcSaveUserRepository`, `LexisNexisFindUserClient`, `KafkaUserNotificationMessenger`).

8. **Expose via Entry Points**: Wire the production adapters into the appropriate entry point for the chosen Shell — e.g., a REST controller, a message consumer, a CLI runner, or an AI MCP tool handler. If cross-cutting infrastructure concerns (such as transaction management or AOP) are required, introduce an intermediate service layer between the adapters and the entry point.

---

## 4. Progressive Tutorial: Implementation Workflow

The following tutorial demonstrates the implementation of a Use Case step-by-step. By the end, you will have a complete, fully testable Use Case with three I/O contracts — all verified in isolation before a single line of infrastructure is written.

**Scenario**: We want to build a feature to retrieve a user by their SSN from a 3rd-party API, save them to a local database, and publish a notification.

### Phase 1: Use Case (The Core)

#### Step 1: Establish the Boundary
Everything starts with defining the exact input (`Command`) and output (`Result`) representing the Use Case.

> [!WARNING]
> Data entering the Use Case boundary **MUST** implement the `Validatable` interface.

> [!NOTE]
> `Validatable` is not just a marker. The framework's base `execute()` method automatically invokes validation on any `Validatable` input before ever delegating to your `doExecute()` core logic. Your business logic is guaranteed to receive safe data.

```java
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
    public record Command(@NotNull @NotBlank String ssn) implements Validatable {}
    public record Result(String name, String email) {}
}
```

#### Step 2: Skeletal Implementation
Establish the core Use Case implementation. Initially, it simply defines a local domain model scoped strictly to this logic.

```java
/** Domain Model scoped strictly to this use case. */
public record User(String ssn, String name, String email) {}

public class DefaultFindUserUseCase extends FindUserUseCase {
    @Override
    protected Result doExecute(Command command) {
        // Business logic goes here
        return new Result(null, null);
    }
}
```

#### Step 3: The Test Harness：JUnit Component Test
Establish the execution harness to enable continuous execution and debugging during development.

```java
@DisplayName("Find User Use Case: Execution Harness")
class FindUserUseCaseComponentTest {
    @Test
    void debug_run() {
        var useCase = new DefaultFindUserUseCase();
        
        // Execute this test iteratively to verify the logic
        var result = useCase.execute(new FindUserUseCase.Command("123-456-789"));
    }
}
```

#### Step 4: Just-In-Time Discovery (Fetching the User)
When the core logic requires external data retrieval, do not implement a protocol-specific client (e.g., HTTP). Instead, define a pure Java contract tailored precisely to the required data.

```java
// Define the required client contract:
public abstract class FindUserClient extends Client<FindUserClient.Command, FindUserClient.Result> {
    public record Command(String ssn) {}
    public record Result(String name, String email) implements Validatable {}
}
```

Inject this contract into the Use Case to process the external data:

```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;

    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }

    @Override
    protected Result doExecute(Command command) {
        // 1. Fetch user data via the client contract
        var apiResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
        var user = new User(command.ssn(), apiResult.name(), apiResult.email());
        
        return new Result(user.name(), user.email());
    }
}
```

#### Step 5: Just-In-Time Discovery (Saving the User)
When the logic requires data persistence, define a repository contract.

```java
// Define the required repository contract:
public abstract class SaveUserRepository extends Repository<SaveUserRepository.Command, SaveUserRepository.Result> {
    public record Command(String name, String email) {}
    public record Result(String id) implements Validatable {}
}
```

Update the Use Case orchestration to implement the persistence flow:

```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;

    public DefaultFindUserUseCase(FindUserClient findUserClient, SaveUserRepository saveUserRepository) {
        this.findUserClient = findUserClient;
        this.saveUserRepository = saveUserRepository;
    }

    @Override
    protected Result doExecute(Command command) {
        var apiResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
        var user = new User(command.ssn(), apiResult.name(), apiResult.email());
        
        // 2. Save the user via the repository contract
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        
        return new Result(user.name(), user.email());
    }
}
```

#### Step 6: Just-In-Time Discovery (Sending Notification)
If an outbound event is required upon completion, define a messenger contract and finalize the orchestration.

```java
// Define the required messenger contract:
public abstract class UserNotificationMessenger extends Messenger<UserNotificationMessenger.Command, UserNotificationMessenger.Result> {
    public record Command(String email, String message) {}
    public record Result(String messageId) implements Validatable {}
}
```

Our Phase 1 Use Case logic is finalized:

```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;
    private final UserNotificationMessenger messenger;

    public DefaultFindUserUseCase(FindUserClient findUserClient, 
                                  SaveUserRepository saveUserRepository, 
                                  UserNotificationMessenger messenger) {
        this.findUserClient = findUserClient;
        this.saveUserRepository = saveUserRepository;
        this.messenger = messenger;
    }

    @Override
    protected Result doExecute(Command command) {
        // 1. Fetch user data
        var apiResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
        var user = new User(command.ssn(), apiResult.name(), apiResult.email());
        
        // 2. Save user
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        
        // 3. Send notification
        var notificationCommand = new UserNotificationMessenger.Command(user.email(), "User found: " + user.name());
        messenger.send(notificationCommand);
        
        return new Result(user.name(), user.email());
    }
}
```

> [!NOTE]
> **Why `.send(Command)`?** Notice that the framework abstracts all I/O interactions into a universal `Result send(Command)` signature, rather than traditional DAO methods like `save()` or `findById()`. Because Hermi strictly enforces Interface Segregation (one contract class per action), the method name no longer needs to describe the action — the class name already does (`FindUserClient`). This universal Command Pattern signature makes it trivial to apply cross-cutting middleware (like retries, circuit breakers, or logging) to all boundary-crossing calls.

#### Step 7: Verify with the Test Shell
With the orchestration complete, verify all boundary and edge cases within the test suite. Unlike mocking frameworks which couple tests to implementation details, state-backed test adapters prove your logic handles real-world state transitions.

> [!TIP]
> Maintaining local abstractions (e.g., in-memory repositories) for every use case can feel heavy. The `hermi-shell` project provides pre-built, reusable test adapters and utilities to significantly reduce this boilerplate.

```java
class InMemorySaveUserRepository extends SaveUserRepository { ... }
 
class ConsoleNotificationMessenger extends UserNotificationMessenger { ... }
 
class LocalFindUserClient extends FindUserClient { ... }
```
```java
@DisplayName("Find User Use Case Shell")
class FindUserUseCaseComponentTest {
    @Test
    @DisplayName("Phase 1 Gate: Edge Case Verification - User Not Found")
    void shouldHandleUserNotFound() {
        // Setup local stubs/state simulators
        var client = new LocalFindUserClient().simulateNotFound(); 
        var repo = new InMemorySaveUserRepository();
        var messenger = new ConsoleNotificationMessenger();
    
        var useCase = new DefaultFindUserUseCase(client, repo, messenger);
        
        // Assert logic holds up on error states
        assertThrows(UserNotFoundException.class, () -> useCase.execute(new FindUserUseCase.Command("999-00-9999")));
    }
}
```

### Phase 2: Building the Shell (Example: Spring Boot)

#### Step 8: Implement Production Adapters
With Phase 1 complete and the core logic verified, build a technology-specific adapter class for each I/O contract discovered in Phase 1.

```java
@Component
public class LexisNexisFindUserClient extends FindUserClient
    implements ClientAdapter<ApiRequest, ApiResponse, FindUserClient.Command, FindUserClient.Result> {

  private RestTemplate restTemplate;

  @Override
  protected Result doSend(Command command) {
    ApiRequest apiRequest = convertCommand(command);
    ApiResponse apiResponse = process(apiRequest);
    return convertResult(apiResponse);
  }

  @Override
  public ApiRequest convertCommand(Command command) {
    return new ApiRequest(command.ssn());
  }

  @Override
  public ApiResponse process(ApiRequest input) {
    return restTemplate.postForObject("/api/users", input, ApiResponse.class);
  }

  @Override
  public Result convertResult(ApiResponse output) {
    return new Result(output.getName(), output.getEmail());
  }
}

@Component
public class JdbcSaveUserRepository extends SaveUserRepository
    implements RepositoryAdapter<UserEntity, UserEntity, SaveUserRepository.Command, SaveUserRepository.Result> {

  private final UserJpaRepository jpaRepository;

  public JdbcSaveUserRepository(UserJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  protected Result doSend(Command command) {
    UserEntity entity = convertCommand(command);
    UserEntity savedEntity = process(entity);
    return convertResult(savedEntity);
  }

  @Override
  public UserEntity convertCommand(Command command) {
    return new UserEntity(command.name(), command.email());
  }

  @Override
  public UserEntity process(UserEntity entity) {
    return jpaRepository.save(entity);
  }

  @Override
  public Result convertResult(UserEntity entity) {
    return new Result(entity.getId());
  }
}

@Component
public class KafkaUserNotificationMessenger extends UserNotificationMessenger
    implements MessengerAdapter<ProducerRecord<String, String>, RecordMetadata, UserNotificationMessenger.Command, UserNotificationMessenger.Result> {

  private final KafkaTemplate<String, String> kafkaTemplate;

  public KafkaUserNotificationMessenger(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  protected Result doSend(Command command) {
    ProducerRecord<String, String> record = convertCommand(command);
    RecordMetadata metadata = process(record);
    return convertResult(metadata);
  }

  @Override
  public ProducerRecord<String, String> convertCommand(Command command) {
    return new ProducerRecord<>("user.notifications", command.message());
  }

  @Override
  public RecordMetadata process(ProducerRecord<String, String> record) {
    try {
      return kafkaTemplate.send(record).get().getRecordMetadata();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Result convertResult(RecordMetadata metadata) {
    return new Result(metadata.toString());
  }
}
```

#### Step 9: Expose via Entry Points
Wire the production adapters into the appropriate entry point for your Shell. The exact mechanism depends on the chosen framework. In this Spring Boot example, a `@RestController` is used, with an intermediate `@Service` layer to support `@Transactional`. If no cross-cutting concerns are required, the `DefaultFindUserUseCase` can be wired directly into the entry point without a dedicated Service class. Other Shell implementations (e.g., Quarkus, CLI runners, message consumers, AI MCP servers) will differ in their wiring approach, but the Use Case core remains unchanged.

```java
@Service
@Transactional
public class FindUserService {
    private final FindUserUseCase findUserUseCase;

    @Autowired
    public FindUserService(LexisNexisFindUserClient client, 
                           JdbcSaveUserRepository repo, 
                           KafkaUserNotificationMessenger messenger) {
        // Instantiate the Use Case with production adapters
        this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
    }

    public FindUserUseCase.Result findUser(FindUserUseCase.Command command) {
        return findUserUseCase.execute(command);
    }
}
```

```java
@RestController
@RequestMapping("/users")
public class FindUserController {
    private final FindUserService findUserService;

    @Autowired
    public FindUserController(FindUserService findUserService) {
        this.findUserService = findUserService;
    }

    @GetMapping
    public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Command command) {
        return findUserService.findUser(command);
    }
}
```

---

## 5. Naming Conventions

Strict predictable boundaries are enforced entirely by stringent naming conventions. Adherence is non-negotiable.

| Component | Target Layer | Naming Pattern | Example |
| :--- | :--- | :--- | :--- |
| **Module** | Use Case | `{project}-{action}-{resource}-usecase` | `hermi-find-user-usecase` |
| **Package** | Use Case | `{org}.{resource}.{action}.usecase` | `org.hermi.user.find.usecase` |
| **Use Case** | Use Case | `{Action}{Resource}UseCase` | `FindUserUseCase` |
| **Implementation** | Use Case | `Default{Action}{Resource}UseCase` | `DefaultFindUserUseCase` |
| **I/O Contract** | Use Case | `{Action}{Resource}{Type}` | `FindUserClient`, `SaveUserRepository` |
| **Adapter (Test)** | Use Case | `{Local/InMemory}{Action}{Resource}{Type}` | `InMemorySaveUserRepository` |
| **Adapter (Prod)**| Shell | `{Tech/Vendor}{Action}{Resource}{Type}` | `JdbcSaveUserRepository` |
| **Shell Module**| Shell | `{project}-{framework}-{type}-shell` | `hermi-spring-rest-shell` |

*(Type refers to `Client`, `Repository`, or `Messenger`)*

---

## 6. Validation Rules

To protect the integrity of the application, data crossing boundaries into the Use Case is heavily policed. All entries must be explicitly validated.

> [!NOTE]
> **Validation Philosophy**: In the Hermi Framework, backend input validation acts as a strict contract enforcement. If a validation error is triggered, it serves as an explicit signal to the upstream developer that their client (e.g., a Web UI or Mobile App) is missing necessary validation logic. By failing fast, the framework forces developers to add missing validations directly to the user interface, improving the end-user experience via instant client-side feedback rather than relying on network round-trips.

| Boundary | Interface | Requirement |
| :--- | :--- | :--- |
| **Entering Use Case** | `UseCase.Command` | `implements Validatable` (Mandatory) |
| **Entering Use Case** | `Client.Result` | `implements Validatable` (Mandatory) |
| **Entering Use Case** | `Repository.Result` | `implements Validatable` (Mandatory) |
| **Entering Use Case** | `Messenger.Result` | `implements Validatable` (Mandatory) |
| **Leaving Use Case** | `UseCase.Result` | Optional |
| **Leaving Use Case** | `Client.Command` | Optional |
| **Leaving Use Case** | `Repository.Command` | Optional |
| **Leaving Use Case** | `Messenger.Command` | Optional |

---

## 7. Project Structure

A pure Java Use Case model coupled with a Framework Shell model dictates a robust multi-module project layout. By breaking the project into strictly separated modules, you physically prevent infrastructure libraries from leaking into business domains.

```text
hermi-user (Parent)
├── pom.xml
│
├── use-cases/hermi-find-user-usecase (Phase 1 Layer: Pure Java)
│   ├── pom.xml
│   ├── src/main/java/org/hermi/user/find/usecase
│   │   ├── FindUserUseCase.java                    (Use Case Contract)
│   │   ├── DefaultFindUserUseCase.java             (Use Case Implementation)
│   │   ├── User.java                               (Scoped Domain Model)
│   │   ├── FindUserClient.java                     (I/O Contract)
│   │   ├── SaveUserRepository.java                 (I/O Contract)
│   │   └── UserNotificationMessenger.java          (I/O Contract)
│   └── src/test/java/org/hermi/user/find/shell
│       ├── FindUserUseCaseComponentTest.java       (Test Harness)
│       ├── LocalFindUserClient.java                (Test Adapter)
│       ├── InMemorySaveUserRepository.java         (Test Adapter)
│       └── ConsoleUserNotificationMessenger.java   (Test Adapter)
│
└── hermi-spring-api-shell (Phase 2 Layer: Framework)
    ├── pom.xml
    └── src/main/java/org/hermi/user/find/shell
        ├── FindUserController.java                 (Spring RestController)
        ├── FindUserService.java                    (Spring Service)
        ├── LexisNexisFindUserClient.java           (Production Adapter)
        ├── JdbcSaveUserRepository.java             (Production Adapter)
        └── KafkaUserNotificationMessenger.java     (Production Adapter)
```