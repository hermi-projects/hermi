# Hermi Use Case Framework

A lightweight framework for building applications following Clean Architecture principles, focusing specifically on the Use Case and Shell architectural patterns.

## Architectural Responsibilities

- **Use Case**: Responsible for "What" the system does. Contains input validation rules, business validation rules, orchestration, and domain models. **Domain models are scoped to the use case** and are defined within the use case package.
- **Shell**: Responsible for "How" the system does it. Implements I/O contracts using specific technologies (e.g., JPA, REST, Kafka).

## Implementation Life Cycle

To ensure a clean separation of concerns, follow this two-phase implementation process:

### Phase 1: Use Case: Business Logic
- **Step 1: Define Use Case Contract**: Create an abstract `UseCase` class and its `Command`/`Result` records.
- **Step 2 & 3: Implementation & JIT I/O Contracts**: Implement the business logic iteratively in a `DefaultUseCase` implementation. If it needs to talk to an external system, define an I/O contract (`Client`, `Repository`, or `Messenger`) right then.
- **Step 4: Finalize Orchestration**: Complete the logic that coordinates these discovered components.
- **Step 5: Verify with Test Shell**: Create minimal, technology-agnostic implementations in your test suite. **Do not use Mocks**; use In-memory, Local, or Console implementations (e.g., `InMemoryRepository`, `LocalClient`, `ConsoleMessenger`) to verify the logic. Completion of tests marks the end of Phase 1.

### Phase 2: Shell: Infrastructure
- **Step 6: Implement Real Adapters**: Create the production implementations in the Shell layer. These implementations can be suffixed with the technology or data vendor name(e.g., `JpaSaveUserRepository`, `KafkaUserNotificationMessenger`, `VendorFindUserClient`).

---

## Example: Find User Use Case

**Description**: Find a user by SSN from a 3rd party API, save them to our database, and notify the system via a message broker.

**Use Case Naming Rule**: `{project}-{action}-{resource}-usecase` (e.g., `hermi-find-user-usecase`)

### Phase 1: Use Case

**Package Naming Rule**: `{org}.{resource}.{action}.usecase` (e.g., `org.hermi.user.find.usecase`)

#### Step 1: Define Use Case Contract
**Use case Naming Rule**: `{action}{resource}UseCase` (e.g., `FindUserUseCase`)

> **Tip**: Data **entering** the use case MUST implement `Validatable`.

```java
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
    public static record Command(@NotNull @NotBlank String ssn) implements Validatable {}
    public static record Result(String name, String email) {}
}
```

#### Step 2 & 3: Implementation & JIT I/O Contracts (Process)

**Use case implementation Naming Rule**: `Default{action}{resource}UseCase` (e.g., `DefaultFindUserUseCase`)

As we implement `DefaultFindUserUseCase`, we discover I/O needs iteratively. We define the contracts **at the moment of discovery**, it contains an abstract class and Command/Result records:

**I/O Contract Naming Rule**: `{action}{resource}{Client|Repository|Messenger}` (e.g., `FindUserClient`)
```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    public DefaultFindUserUseCase() {}

    @Override
    protected Result doExecute(Command command) {
        // Initial skeletal implementation
        return new Result(null, null);
    }
}

/**
 * Domain Model scoped to this specific use case.
 */
public record User(String ssn, String name, String email) {}
```

**1. Need to fetch user?** Define `FindUserClient` contract:
```java
// a. Need to fetch user from 3rd party API.
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }
    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        return new Result(null, null);
    }
}

// b. Define a client contract, FindUserClient.
public abstract class FindUserClient extends Client<FindUserClient.Command, FindUserClient.Result> {
    public static record Command(String ssn) {}
    public static record Result(String name, String email) implements Validatable {}
}
// c. Add the client to constructor and Fetch user and do something with result.
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;

    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }

    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        return new Result(null, null);
    }
}
```

**2. Need to save user?** Define `SaveUserRepository` contract:
```java
// d. Need to save user to database.
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }
    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        User user = new User(result.name(), result.email());
        // 3. Save user to database
        return new Result(null, null);
    }
}
// e. Define a repository contract, SaveUserRepository.
public abstract class SaveUserRepository extends Repository<SaveUserRepository.Command, SaveUserRepository.Result> {
    public static record Command(String name, String email) {}
    public static record Result(String id) implements Validatable {}
}
// f. Add the repository to constructor and Save user and do something with result.
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;

    public DefaultFindUserUseCase(FindUserClient findUserClient, SaveUserRepository saveUserRepository) {
        this.findUserClient = findUserClient;
        this.saveUserRepository = saveUserRepository;
    }

    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        User user = new User(result.name(), result.email());
        // 3. Save user to database
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        // 4. do something with result
        return new Result(null, null);
    }
}
```

**3. Need to notify?** Define `UserNotificationMessenger` contract:
```java
// g. Need to notify user.
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;
    public DefaultFindUserUseCase(FindUserClient findUserClient, SaveUserRepository saveUserRepository) {
        this.findUserClient = findUserClient;
        this.saveUserRepository = saveUserRepository;
    }
    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        User user = new User(result.name(), result.email());
        // 3. Save user to database
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        // 4. do something with result
        // 5. Send notification message
        return new Result(null, null);
    }
}
// h. Define a messenger contract, UserNotificationMessenger.
public abstract class UserNotificationMessenger extends Messenger<UserNotificationMessenger.Command, UserNotificationMessenger.Result> {
    public static record Command(String email, String message) {}
    public static record Result(String messageId) implements Validatable {}
}
// i. Add the messenger to constructor and Send notification message
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
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        User user = new User(result.name(), result.email());
        // 3. Save user to database
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        // 4. do something with result
        // 5. Send notification message
        var notificationCommand = new UserNotificationMessenger.Command(user.email(), "User found: " + user.name());
        UserNotificationMessenger.Result notificationResult = messenger.send(notificationCommand);
        return new Result(null, null);
    }
}
```

#### Step 4: Final Orchestration
```java
// j. Final Orchestration
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final SaveUserRepository userRepository;
    private final FindUserClient findUserClient;
    private final UserNotificationMessenger messenger;

    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. do something with result
        User user = new User(result.name(), result.email());
        // 3. Save user to database
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        // 4. do something with result
        // 5. Send notification message
        var notificationCommand = new UserNotificationMessenger.Command(user.email(), "User found: " + user.name());
        UserNotificationMessenger.Result notificationResult = messenger.send(notificationCommand);
        return new Result(user.name(), user.email());
    }
}
```

#### Step 5: Verify with Test Shell (Phase 1 Gate)
> **Tip**: Use technology-agnostic In-Memory or Console implementations for the Test Shell; avoid Mocks.

Implement contracts:

**Package Naming Rule**: `{org}.{resource}.{action}.shell.{type}` (e.g., `org.hermi.user.find.shell.junit`)

**I/O Contract Implementation Naming Rule**: `{Tech|Vendor}{action}{resource}{type}` (e.g., `LocalFindUserClient`, `ConsoleNotificationMessenger`, `InMemorySaveUserRepository`)

```java
class InMemorySaveUserRepository extends SaveUserRepository implements RepositoryAdapter<...> { ... }
class ConsoleNotificationMessenger extends UserNotificationMessenger implements MessengerAdapter<...> { ... }
class LocalFindUserClient extends FindUserClient implements ClientAdapter<...> { ... }
```
Implement test shell: Phase 1 is complete when this test passes

```java
@DisplayName("Find User Use Case Shell")
class FindUserShellComponentTest {
    @Test
    @DisplayName("Test Find User Use Case")
    void testFindUser() {
        
    }
}
```

### Phase 2: Shell

**Shell Naming Rule**: `{project}-{framework}-{type}-shell` (e.g., `hermi-spring-rest-shell`)

#### Step 6: Implement Real Adapters
Implement production-ready adapters. These implementations handle the technical details of communication and data transformation.

**Package Naming Rule**: `{org}.{resource}.{action}.shell` (e.g., `org.hermi.user.find.shell`)

**I/O Contract Implementation Naming Rule**: `{Tech|Vendor}{action}{resource}{type}` (e.g., `LexisNexisFindUserClient`, `KafkaNotificationMessenger`, `JdbcSaveUserRepository`)

```java
@Component
public class LexisNexisFindUserClient extends FindUserClient
    implements ClientAdapter<ApiRequest, ApiResponse, FindUserClient.Command, FindUserClient.Result> {

  private   RestTemplate restTemplate;

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
~~~java
// Spring Service handle database transaction.
@Service
@Transactional
public class FindUserService {
    private final FindUserUseCase findUserUseCase;
    @Autowired
    public FindUserService(LexisNexisFindUserClient findUserClient, JdbcSaveUserRepository saveUserRepository, KafkaUserNotificationMessenger notificationMessenger) {
        this.findUserUseCase = new DefaultFindUserUseCase(
            findUserClient,
            saveUserRepository,
            notificationMessenger
        );
    }

    public FindUserUseCase.Result findUser(FindUserUseCase.Command command) {
        return findUserUseCase.execute(command);
    }
}
~~~
~~~java
// Spring Controller, handle data conversion.
@RestController
@RequestMapping("/users")
public class FindUserController {
    private final FindUserService findUserService;

    @GetMapping
    public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Command command) {
        return findUserService.findUser(command);
    }
}
~~~

## Project Structure: a Maven Multi-Module Project

Organize the project into separate modules to enforce the Phase 1/2 phasical separation and dependency rules, stop leaking framework details into use cases.

```text
hermi-user (Parent)
├── pom.xml
├── use-cases/hermi-find-user-usecase (Phase 1 Layer)
│   ├── pom.xml
│   ├── src/main/java/org/hermi/user/find/usecase
│   │   ├── FindUserUseCase.java                    (Use Case Contract)
│   │   ├── DefaultFindUserUseCase.java             (Use Case Implementation)
│   │   ├── User.java                               (Scoped Domain Model)
│   │   ├── FindUserClient.java                     (I/O Contract)
│   │   ├── SaveUserRepository.java                 (I/O Contract)
│   │   └── UserNotificationMessenger.java          (I/O Contract)
│   └── src/test/java/org/hermi/user/find/shell
│       ├── FindUserUseCaseTest.java                (JUnit Test)
│       ├── LocalFindUserClient.java                (I/O Contract Implementation)
│       ├── InMemorySaveUserRepository.java         (I/O Contract Implementation)
│       └── ConsoleUserNotificationMessenger.java   (I/O Contract Implementation)
│
└── hermi-spring-api-shell (Phase 2 Layer)
    ├── pom.xml
    └── src/main/java/org/hermi/user/find/shell
        ├── FindUserController.java                 (Spring RestController)
        ├── FindUserService.java                    (Spring Service)
        ├── LexisNexisFindUserClient.java           (I/O Contract Implementation)
        ├── JpaSaveUserRepository.java              (I/O Contract Implementation)
        └── KafkaUserNotificationMessenger.java     (I/O Contract Implementation)
```

## Validation Rules
Data going through the use case boundary must be validatable.

- **Entering Use Case**: `UseCase.Command`, `Client.Result`, `Repository.Result`, and `Messenger.Result` **must** implement `Validatable`.
- **Leaving Use Case**: `UseCase.Result`, `Client.Command`, `Repository.Command`, and `Messenger.Command` are optional `Validatable`.