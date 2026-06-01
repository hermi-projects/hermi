---
name: shell
description: Defines the Shell layer — verification harnesses and delivery entry points for the Hermi architecture. Covers verification shells (MainShell, local utilities) for continuous use case testing and delivery shells (REST, Kafka, CLI, AI MCP) for production exposure. Keywords: shell, main shell, play button, entry point, rest controller, kafka consumer, service wiring, delivery mechanism, verification.
metadata:
  class: "org.hermi.shell.EntryPoint"
  phase: "shell"
  priority: high
---

# Shell

## Role & Design Intent

**The Shell connects pure business logic to the outside world through two categories:**

**Verification Shells:** Provide continuous validation during development without infrastructure. Includes the MainShell "Play Button" and stateful local utilities (LocalClient, InMemoryRepository, ConsoleMessenger) for each JIT-discovered contract.

**Delivery Shells:** Expose the use case through production delivery mechanisms. Includes Service wiring, REST API, Kafka Consumer, CLI, and AI MCP entry points.

## Verification Shells

Verification shells live in the test source tree and enable continuous execution without mocking frameworks, databases, or network.

### Directory

```
src/test/java/{org}/{resource}/{action}/shell/
├── {Action}{Resource}MainShell.java           # Play Button
├── Local{Action}{Resource}Client.java         # Programmable local utility
├── InMemory{Action}{Resource}Repository.java  # Stateful in-memory utility
└── Console{Action}{Resource}Messenger.java    # Tracing utility
```

### MainShell — Play Button

The MainShell is the primary execution harness. It instantiates the use case with stateful local utilities and provides a `main` method for immediate execution:

```java
import org.hermi.shell.util.LocalClient;
import org.hermi.shell.util.InMemoryRepository;
import org.hermi.shell.util.ConsoleMessenger;

public class FindUserMainShell {
  public static void main(String[] args) {
    var client = new LocalClient<FindUserClient.Context, FindUserClient.Result>()
        .put(new FindUserClient.Context("123-45-6789"),
             new FindUserClient.Result("John Doe", "john@example.com"));
    var repo = new InMemoryRepository<SaveUserRepository.Context, SaveUserRepository.Result>()
        .put(new SaveUserRepository.Context("John", "john@example.com"),
             new SaveUserRepository.Result("id-001"));
    var messenger = new ConsoleMessenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result>()
        .put(new NotifyUserFoundMessenger.Context("john@example.com", "User found"),
             new NotifyUserFoundMessenger.Result("msg-123"));
    var useCase = new DefaultFindUserUseCase(client, repo, messenger);

    var result = useCase.execute(new FindUserUseCase.Context("123-45-6789"));
    if (result == null) throw new AssertionError("Result cannot be null");
    System.out.println("✅ Verified: " + result.name());
  }
}
```

### Reusable Utilities

Hermi provides pre-built in-memory utilities in `org.hermi.shell.util` — no manual implementation classes needed:

| Utility | Base Class | Package | Details |
|---|---|---|---|
| `LocalClient<P, R>` | `Client<P, R>` | `org.hermi.shell.util` | See [shell-client](template/element/shell-client/ELEMENT.md) |
| `InMemoryRepository<E, R>` | standalone | `org.hermi.shell.util` | See [shell-repository](template/element/shell-repository/ELEMENT.md) |
| `ConsoleMessenger<P, R>` | `Messenger<P, R>` | `org.hermi.shell.util` | See [shell-messenger](template/element/shell-messenger/ELEMENT.md) |

These replace the inline `LocalFindUserClient` / `InMemorySaveUserRepository` / `ConsoleNotifyUserFoundMessenger` pattern shown in earlier documentation.

### Naming

| Component | Pattern | Example |
|---|---|---|
| Local Client | `{ContractName}` or `LocalClient<C, R>` | `LocalFindUserClient` or `LocalClient<>` |
| InMemory Repository | `{ContractName}` or `InMemoryRepository<E, R>` | `InMemorySaveUserRepository` or `InMemoryRepository<>` |
| Console Messenger | `{ContractName}` or `ConsoleMessenger<P, R>` | `ConsoleNotifyUserFoundMessenger` or `ConsoleMessenger<>` |

## Delivery Shells

Delivery shells live in the production source tree and expose the use case through technology-specific entry points.

### Directory

```
src/main/java/{org}/{resource}/{action}/shell/
├── {Action}{Resource}Service.java            # Service wiring
├── {Action}{Resource}ApiShell.java           # REST entry point
├── {Action}{Resource}ConsumerShell.java      # Message-driven entry point
└── {Action}{Resource}CliShell.java           # CLI entry point
```

### Service — Wire Dependencies into the Use Case

```java
@Service
@Transactional
public class FindUserService {
  private final FindUserUseCase findUserUseCase;

  @Autowired
  public FindUserService(LexisNexisFindUserClient client,
                         JpaSaveUserRepository repo,
                         KafkaNotifyUserFoundMessenger messenger) {
    this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
  }

  public FindUserUseCase.Result findUser(FindUserUseCase.Context context) {
    return findUserUseCase.execute(context);
  }
}
```

### REST Entry Point

```java
@RestController
@RequestMapping("/users")
public class FindUserApiShell {
  private final FindUserService findUserService;

  @Autowired
  public FindUserApiShell(FindUserService findUserService) {
    this.findUserService = findUserService;
  }

  @GetMapping
  public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Context context) {
    return findUserService.findUser(context);
  }
}
```

### Kafka Consumer Entry Point

```java
@Component
public class FindUserConsumerShell {
  private final FindUserService findUserService;

  @Autowired
  public FindUserConsumerShell(FindUserService findUserService) {
    this.findUserService = findUserService;
  }

  @KafkaListener(topics = "user.find.requests", groupId = "user-service-group")
  public void consume(String ssn) {
    findUserService.findUser(new FindUserUseCase.Context(ssn));
  }
}
```

### Naming

| Component | Pattern | Example |
|---|---|---|
| Service | `{Action}{Resource}Service` | `FindUserService` |
| REST Entry Point | `{Action}{Resource}ApiShell` | `FindUserApiShell` |
| Consumer Entry Point | `{Action}{Resource}ConsumerShell` | `FindUserConsumerShell` |
| CLI Entry Point | `{Action}{Resource}CliShell` | `FindUserCliShell` |

## Forbidden Patterns

- ❌ **No business logic** in any shell component — shells are wiring and delivery only
- ❌ **No direct instantiation** of shell dependencies in entry points — always delegate via Service
- ❌ **No use-case module imports** from delivery frameworks — Shell depends on use-case, not vice versa
- ❌ **No mocking frameworks** in verification — use `org.hermi.shell.util` reusable utilities

## Complete Example

### Verification — MainShell with Reusable Utilities

```java
// ==== Reusable Utilities (org.hermi.shell.util) ====
import org.hermi.shell.util.LocalClient;
import org.hermi.shell.util.InMemoryRepository;
import org.hermi.shell.util.ConsoleMessenger;

// ==== MainShell ====
public class FindUserMainShell {
  public static void main(String[] args) {
    var client = new LocalClient<FindUserClient.Context, FindUserClient.Result>()
        .put(new FindUserClient.Context("123-45-6789"),
             new FindUserClient.Result("John Doe", "john@example.com"));
    var repo = new InMemoryRepository<SaveUserRepository.Context, SaveUserRepository.Result>()
        .put(new SaveUserRepository.Context("John", "john@example.com"),
             new SaveUserRepository.Result("id-001"));
    var messenger = new ConsoleMessenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result>()
        .put(new NotifyUserFoundMessenger.Context("john@example.com", "User found"),
             new NotifyUserFoundMessenger.Result("msg-123"));
    var useCase = new DefaultFindUserUseCase(client, repo, messenger);
    var result = useCase.execute(new FindUserUseCase.Context("123-45-6789"));
    System.out.println("✅ Verified: " + result.name());
  }
}
```

### Production Delivery

```java
// ==== Service ====
@Service
@Transactional
public class FindUserService {
  private final FindUserUseCase findUserUseCase;

  @Autowired
  public FindUserService(LexisNexisFindUserClient client,
                         JpaSaveUserRepository repo,
                         KafkaNotifyUserFoundMessenger messenger) {
    this.findUserUseCase = new DefaultFindUserUseCase(client, repo, messenger);
  }

  public FindUserUseCase.Result findUser(FindUserUseCase.Context context) {
    return findUserUseCase.execute(context);
  }
}

// ==== REST Entry Point ====
@RestController
@RequestMapping("/users")
public class FindUserApiShell {
  private final FindUserService findUserService;

  @Autowired
  public FindUserApiShell(FindUserService findUserService) {
    this.findUserService = findUserService;
  }

  @GetMapping
  public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Context context) {
    return findUserService.findUser(context);
  }
}

// ==== Kafka Consumer Entry Point ====
@Component
public class FindUserConsumerShell {
  private final FindUserService findUserService;

  @Autowired
  public FindUserConsumerShell(FindUserService findUserService) {
    this.findUserService = findUserService;
  }

  @KafkaListener(topics = "user.find.requests", groupId = "user-service-group")
  public void consume(String ssn) {
    findUserService.findUser(new FindUserUseCase.Context(ssn));
  }
}
```

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — the use case this shell verifies and delivers
- [shell-client](template/element/shell-client/ELEMENT.md) — client implementation wired into the service
- [shell-repository](template/element/shell-repository/ELEMENT.md) — repository implementation wired into the service
- [shell-messenger](template/element/shell-messenger/ELEMENT.md) — messenger implementation wired into the service
