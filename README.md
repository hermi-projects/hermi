# Hermi Use Case Framework

A lightweight framework for building applications following Clean Architecture principles, focusing specifically on the Use Case and Shell architectural patterns.

## Architectural Responsibilities

- **Use Case Layer (Core)**: Responsible for "What" the system does. Contains business rules, orchestration, and domain models. **Domain models are scoped specifically to the use case** and are defined within the use case package.
- **Shell Layer (Infrastructure)**: Responsible for "How" the system does it. Implements I/O contracts using specific technologies (e.g., JPA, REST, Kafka).

## Implementation Life Cycle

To ensure a clean separation of concerns, follow this two-phase implementation process:

### Phase 1: Use Case Layer (Core Business Logic)
- **Step 1: Define Use Case Contract**: Create the abstract use case class and its `Command`/`Result` records.
- **Step 2 & 3: Implementation & JIT I/O Contracts**: Implement the business logic iteratively in a default use case implementation. If you need to talk to an external system, define the contract (Client, Repository, or Messenger) right then. Do not define them upfront.
- **Step 4: Finalize Orchestration**: Complete the logic that coordinates these discovered components.
- **Step 5: Verify with JUnit Shell**: Create minimal, technology-prefixed implementations (e.g., `InMemorySaveUserRepository`) in your test suite. **Do not use Mocks**; use In-memory or Console implementations to verify the logic. Completion of tests marks the end of Phase 1.

### Phase 2: Shell Layer (Infrastructure)
- **Step 6: Implement Real Adapters**: Create the production implementations in the Shell layer prefixed with the technology name (e.g., `JpaSaveUserRepository`, `KafkaUserNotificationMessenger`, `RestFindUserClient`).

---

## Example: Find User Use Case

**Description**: Find a user by SSN from a 3rd party API, save them to our database, and notify the system via a message broker.

**Package Naming Rule**: `{org}.{resource}.{action}.usecase/shell` (e.g., `org.hermi.user.find.usecase`)

### Phase 1: Use Case Layer (`org.hermi.user.find.usecase`)

#### Step 1: Define Use Case Contract
> **Tip**: Data **entering** the use case MUST implement `Validatable`.

```java
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Command, FindUserUseCase.Result> {
    public static record Command(@NotNull @NotBlank String ssn) implements Validatable {}
    public static record Result(String name, String email) {}
}
```

#### Step 2 & 3: Implementation & JIT I/O Contracts (Process)
As we implement `DefaultFindUserUseCase`, we discover I/O needs iteratively. We define the contracts **at the moment of discovery**:

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

public abstract class FindUserClient extends Client<FindUserClient.Command, FindUserClient.Result> {
    public static record Command(String ssn) {}
    public static record Result(String name, String email) implements Validatable {}
}

public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;

    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }

    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        return new Result(null, null);
    }
}
```

**2. Need to save user?** Define `SaveUserRepository` contract:
```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient findUserClient;
    public DefaultFindUserUseCase(FindUserClient findUserClient) {
        this.findUserClient = findUserClient;
    }
    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        // 2. Save user to database
        return new Result(null, null);
    }
}

public abstract class SaveUserRepository extends Repository<SaveUserRepository.Command, SaveUserRepository.Result> {
    public static record Command(String name, String email) {}
    public static record Result(String id) implements Validatable {}
}

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
        FindUserClient.Result clientResult = findUserClient.send(new FindUserClient.Command(command.ssn()));
        User user = new User(clientResult, command.ssn());
        // 2. Save user to database
        saveUserRepository.send(new SaveUserRepository.Command(user.name(), user.email()));
        return new Result(null, null);
    }
}
```

**3. Need to notify?** Define `UserNotificationMessenger` contract:
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
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        User user = new User(result, command.ssn());
        // 2. Save user to database
        var saveCommand = new SaveUserRepository.Command(user.name(), user.email());
        SaveUserRepository.Result saveResult = saveUserRepository.send(saveCommand);
        // 3. Send notification message
        return new Result(null, null);
    }
}

public abstract class UserNotificationMessenger extends Messenger<UserNotificationMessenger.Command, UserNotificationMessenger.Result> {
    public static record Command(String email, String message) {}
    public static record Result(String messageId) implements Validatable {}
}

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
        User user = new User(result, command.ssn());
        // 2. Save user to database
        var saveCommand = new SaveUserRepository.Command(user.name(), user.email());
        SaveUserRepository.Result saveResult = saveUserRepository.send(saveCommand);
        // 3. Send notification message
        var notificationCommand = new UserNotificationMessenger.Command(user.email(), "User found: " + user.name());
        UserNotificationMessenger.Result notificationResult = messenger.send(notificationCommand);
        return new Result(null, null);
    }
}
```

#### Step 4: Final Orchestration
```java

public class DefaultFindUserUseCase extends FindUserUseCase {
    private final SaveUserRepository userRepository;
    private final FindUserClient findUserClient;
    private final UserNotificationMessenger messenger;

    @Override
    protected Result doExecute(Command command) {
        // 1. Find user from 3rd party API
        FindUserClient.Result result = findUserClient.send(new FindUserClient.Command(command.ssn()));
        User user = new User(result, command.ssn());
        // 2. Save user to database
        var saveCommand = new SaveUserRepository.Command(user.name(), user.email());
        SaveUserRepository.Result saveResult = saveUserRepository.send(saveCommand);
        // 3. Send notification message
        var notificationCommand = new UserNotificationMessenger.Command(user.email(), "User found: " + user.name());
        UserNotificationMessenger.Result notificationResult = messenger.send(notificationCommand);
        
        return new Result(user.name(), user.email());
    }
}
```

#### Step 5: Verify with JUnit Shell (Phase 1 Gate)
> **Tip**: Use technology-prefixed In-Memory or Console implementations for the JUnit Shell; avoid Mocks.

```java
// Testing using technology-prefixed Shells (In-Memory/Console)
class InMemorySaveUserRepository extends SaveUserRepository implements RepositoryAdapter<...> { ... }
class ConsoleNotificationMessenger extends UserNotificationMessenger implements MessengerAdapter<...> { ... }

@Test
void testFindUser() {
    // Phase 1 is complete when this test passes with JUnit Shells
}
```

### Phase 2: Shell Layer (`org.hermi.user.find.shell`)

#### Step 6: Implement Real Adapters
Implement production-ready adapters with technology prefixes. These implementations handle the technical details of communication and data transformation.

```java
@Component
public class RestFindUserClient extends FindUserClient
    implements ClientAdapter<ApiRequest, ApiResponse, FindUserClient.Command, FindUserClient.Result> {

  private final RestTemplate restTemplate;

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
public class JpaSaveUserRepository extends SaveUserRepository
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
@Service
@Transactional
public class FindUserService {
    private final FindUserUseCase findUserUseCase;
    @Autowired
    public FindUserService(RestFindUserClient restFindUserClient, JpaSaveUserRepository jpaSaveUserRepository, KafkaUserNotificationMessenger kafkaUserNotificationMessenger) {
        this.findUserUseCase = new DefaultFindUserUseCase(
            restFindUserClient,
            jpaSaveUserRepository,
            kafkaUserNotificationMessenger
        );
    }

    public FindUserUseCase.Result findUser(FindUserUseCase.Command command) {
        return findUserUseCase.execute(command);
    }
}
~~~

~~~java
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
## Maven Multi-Module Project Structure

Organize your project into separate modules to enforce the Phase 1/2 separation and dependency rules.

```text
hermi-user (Parent)
├── pom.xml
├── use-cases/hermi-user-usecase (Phase 1 Layer)
│   ├── pom.xml
│   ├── src/main/java/org/hermi/user/find/usecase
│   │   ├── FindUserUseCase.java          (Contract)
│   │   ├── DefaultFindUserUseCase.java   (Implementation)
│   │   ├── User.java                     (Scoped Domain Model)
│   │   ├── FindUserClient.java           (I/O Contract)
│   │   ├── SaveUserRepository.java       (I/O Contract)
│   │   └── UserNotificationMessenger.java (I/O Contract)
│   └── src/test/java/org/hermi/user/find/shell
│       ├── FindUserUseCaseTest.java      (JUnit Test)
│       ├── LocalFindUserClient.java           (Adapter Implementation)
│       ├── InMemorySaveUserRepository.java       (Adapter Implementation)
│       └── ConsoleUserNotificationMessenger.java (Adapter Implementation)
│
└── hermi-user-spring-shell (Phase 2 Layer)
    ├── pom.xml
    └── src/main/java/org/hermi/user/find/shell
        ├── FindUserController.java        (RestController)
        ├── FindUserService.java     (Spring Service Wrapper/Support)
        ├── RestFindUserClient.java        (Adapter Implementation)
        ├── JpaSaveUserRepository.java     (Adapter Implementation)
        └── KafkaUserNotificationMessenger.java (Adapter Implementation)
```

## Validation Rules
- **Entering Use Case**: `UseCase.Command`, `Client.Result`, `Repository.Result`, and `Messenger.Result` **must** implement `Validatable`.
- **Leaving Use Case**: `UseCase.Result`, `Client.Command`, `Repository.Command`, and `Messenger.Command` are typically **not** `Validatable`.