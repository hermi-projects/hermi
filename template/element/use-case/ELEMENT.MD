---
name: use-case
description: Describes a business operation and orchestrates domain logic through external contracts (use-case-client, use-case-repository, use-case-messenger). Use when implementing a business feature or domain operation. Keywords: use case, business logic, feature, domain service, operation.
metadata:
  class: "org.hermi.usecase.standard.UseCase"
  phase: "use case"
  priority: high
---
# Use Case
## Role & Design Intent

**Use this element when implementing a business feature or domain operation.** The Use Case is the central unit of business logic in the Hermi framework. It:

- Encapsulates **one** business capability (e.g., register a user, process a payment)
- Orchestrates external calls through **JIT-discovered contracts** — [use-case-client](template/element/use-case-client/ELEMENT.md), [use-case-repository](template/element/use-case-repository/ELEMENT.md), [use-case-messenger](template/element/use-case-messenger/ELEMENT.md)
- Remains **infrastructure-agnostic** — no framework, no protocol, no persistence technology
- Follows **Blueprint-First Orchestration** — build the holistic narrative before implementing details
- Defines **Scoped Domain Models** — localized records belonging to this use case only
- Is **statically verified** in Phase 1 via stateful local adapter shells

The Use Case is protected by **The Three Pillars of Integrity**:
- **Boundary Integrity** — strict physical separation (Core vs Shell modules); localized scoped models prevent domain coupling
- **Protocol Integrity** — mandatory `Validatable` on every boundary crossing into the Core
- **Semantic Integrity** — rigid naming conventions (`Action-Resource`, `Notify-Fact`) and JIT discovery keep code aligned to business intent

## Directory Structure

A use case implementation follows this structure:

```
src/main/java/{org}/{resource}/{action}/usecase/
├── {Action}{Resource}UseCase.java          # Abstract contract (Context, Result, doExecute)
├── Default{Action}{Resource}UseCase.java   # Default implementation with injected contracts
├── {Action}{Resource}Client.java           # JIT-discovered: external API contract
├── {Action}{Resource}Repository.java       # JIT-discovered: persistence contract
├── {Action}{Resource}Messenger.java        # JIT-discovered: messaging contract
├── {Resource}.java                         # Scoped domain model
└── {domain}DomainException.java            # Domain exception(s)
```

See [shell](template/element/shell/ELEMENT.md) for the test directory structure containing MainShell and local adapters. The package follows the pattern `{org}.{resource}.{action}.usecase` — e.g., `org.hermi.user.find.usecase`.

## Generation Rules

### 0. Extract Required Inputs from Task

Before generating code, extract these inputs from the feature requirements:

| Input | Determined by | Example |
|---|---|---|
| Action (verb) | What the use case does | `Find`, `Register`, `Process` |
| Resource (noun) | What it operates on | `User`, `Payment`, `Order` |
| Domain package | Module grouping | `user`, `payment`, `order` |
| Context fields | Input data needed by the operation — **ask human for validation constraints** | ssn, email, amount |
| Result fields | Output data produced | name, status, id, email |
| External system calls | Business logic steps | Lookup external API, persist data, send notification |

These determine the naming prophecy, file structure, and JIT-discovered contract needs.

### Element Chain Dependencies

When `doExecute` reveals a need to call an external system, **generate the corresponding contract element in the same step**:

| Need | Element to generate | Extends |
|---|---|---|
| External API call | [use-case-client](template/element/use-case-client/ELEMENT.md) | `Client<C, R extends Validatable>` |
| Data persistence | [use-case-repository](template/element/use-case-repository/ELEMENT.md) | `Repository<C, R extends Validatable>` |
| Async notification | [use-case-messenger](template/element/use-case-messenger/ELEMENT.md) | `Messenger<C, R extends Validatable>` |

### 1. Establish the Boundary — Context, Result, and Scoped Model

Define `Context` and `Result` as inner records. Add a **scoped domain model** when the use case operates on a domain entity:

```java
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {

  public record Context(@NotNull @NotBlank String ssn) implements Validatable {}

  public record Result(String name, String email) {}
}
```

```java
/** Scoped domain model — belongs to this use case only. */
public record User(String ssn, String name, String email) {}
```

**Rules:**
- `Context` **MUST** implement `Validatable` — the base class validates before `doExecute`
- For each `Context` field, **ask the human for validation constraints** (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Email`, etc.) — do not invent constraints
- `Result` uses plain Java types; does NOT need `Validatable` (it's output, not input)
- **Contract Results** (`Client.Result`, `Repository.Result`, `Messenger.Result`) **MUST** implement `Validatable` — they cross the boundary INTO the use case
- **Contract Contexts** (`Client.Context`, `Repository.Context`, `Messenger.Context`) do NOT need `Validatable` — they flow OUT to infrastructure
- Scoped models are plain records, package-private to the use case module

### 2. Skeletal Implementation

Create the default implementation before writing business logic:

```java
public class DefaultFindUserUseCase extends FindUserUseCase {
  @Override
  protected Result doExecute(Context context) {
    return new Result(null, null);
  }
}
```

See [shell](template/element/shell/ELEMENT.md) for the MainShell "Play Button" execution harness and stateful local adapter patterns.

### 3. Stateless — Constructor Injection Only

```java
public class DefaultFindUserUseCase extends FindUserUseCase {
  private final FindUserClient findUserClient;               // final, injected
  private final SaveUserRepository saveUserRepository;       // final, injected
  private final NotifyUserFoundMessenger messenger;          // final, injected

  public DefaultFindUserUseCase(
      FindUserClient findUserClient,
      SaveUserRepository saveUserRepository,
      NotifyUserFoundMessenger messenger) {
    this.findUserClient = findUserClient;
    this.saveUserRepository = saveUserRepository;
    this.messenger = messenger;
  }
}
```

- No request-specific instance variables
- All dependencies are `final` and injected via constructor
- Dependencies are abstract contracts, not concrete implementations

### 4. JIT Contract Discovery and Holistic Orchestration

Do NOT design contracts upfront. Let the business logic reveal its needs:

1. Write `doExecute` with the business steps as a **holistic narrative**
2. For each external call, discover the contract at that moment
3. Each contract extends one of: `Client`, `Repository`, or `Messenger`
4. Finalize the orchestration flow after all contracts are discovered

**Example discovery sequence:**

```java
@Override
protected Result doExecute(Context context) {
  // Step 1: I need to fetch user data → JIT discover FindUserClient
  FindUserClient.Result apiResult = findUserClient.execute(
      new FindUserClient.Context(context.ssn()));

  // Step 2: I need to persist → JIT discover SaveUserRepository
  saveUserRepository.execute(
      new SaveUserRepository.Context(apiResult.name(), apiResult.email()));

  // Step 3: I need to notify → JIT discover NotifyUserFoundMessenger
  messenger.execute(
      new NotifyUserFoundMessenger.Context(apiResult.email(), "User found"));

  return new Result(apiResult.name(), apiResult.email());
}
```

**Holistic Orchestration** means completing the full narrative flow before refining any single contract. Build the whole, then perfect the details.

### 5. Wrap Infrastructure Exceptions

Every infrastructure call can fail. Catch and rethrow as a named domain exception:

```java
@Override
protected Result doExecute(Context context) {
  FindUserClient.Result apiResult;
  try {
    apiResult = findUserClient.execute(new FindUserClient.Context(context.ssn()));
  } catch (Exception e) {
    throw new UserLookupException("Failed to find user with SSN: " + context.ssn(), e);
  }
  // ... continue
}
```

### 6. Naming Prophecy

| Artifact | Pattern | Example |
|---|---|---|
| Module / Project | `{project}-{action}-{resource}-use-case` | `cdn-find-user-use-case` |
| Package | `{org}.{resource}.{action}.usecase` | `org.hermi.user.find.usecase` |
| Abstract contract | `{Action}{Resource}UseCase` | `FindUserUseCase` |
| Default impl | `Default{Action}{Resource}UseCase` | `DefaultFindUserUseCase` |
| Client contract | `{Action}{Resource}Client` | `FindUserClient` |
| Repository contract | `{Action}{Resource}Repository` | `SaveUserRepository` |
| Messenger contract | `Notify{Fact}Messenger` | `NotifyUserFoundMessenger` |
| Scoped model | `{Resource}` | `User` |

### 7. The Three Golden Rules

1. **Tense Integrity** — Every I/O component is an Action. Names MUST start with a Verb (`Find`, `Save`, `Notify`). Total execution is a single `.execute()` call within a specific `Context`, yielding a `Result`.

2. **Prefix Isolation** — Core is pure; Shell carries technology. Infrastructure classes MUST have the technology name as the first word (`JpaSaveUserRepository`). No prefix means Pure Java.

3. **Single Action Prophecy** — One class, one action. Never create `Utility`, `Manager`, or `Service` classes in the Core that group unrelated behaviors. Each new action gets a new class.

## Forbidden Patterns

- ❌ **No infrastructure types** in Context or Result (no `HttpHeaders`, `JPA`, `Kafka`, etc.)
- ❌ **No framework annotations** in Phase 1 (`@Service`, `@Transactional`, `@Component`)
- ❌ **No infrastructure exceptions** leaking out of `doExecute` — always wrap
- ❌ **No mocks** for Phase 1 verification — use `org.hermi.shell.util` reusable utilities
- ❌ **No business logic** in Client/Repository/Messenger — they are gateways only
- ❌ **No thread safety concerns** — use case instances are single-threaded in Phase 1

## Complete Example

### Contract & Implementation

```java
// ==== Abstract Use Case ====
public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {
  public record Context(@NotNull @NotBlank String ssn) implements Validatable {}
  public record Result(String name, String email) {}
}

// ==== Scoped Domain Model ====
public record User(String ssn, String name, String email) {}

// ==== JIT-Discovered Contracts ====
public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
  public record Context(String ssn) {}
  // Result crosses INTO the use case — MUST be Validatable
  public record Result(String name, String email) implements Validatable {}
}

public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
  public record Context(String name, String email) {}
  public record Result(String id) implements Validatable {}
}

public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
  public record Context(String email, String message) {}
  public record Result(String messageId) implements Validatable {}
}

// ==== Domain Exception ====
public class UserLookupException extends RuntimeException {
  public UserLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}

// ==== Default Implementation ====
public class DefaultFindUserUseCase extends FindUserUseCase {
  private final FindUserClient findUserClient;
  private final SaveUserRepository saveUserRepository;
  private final NotifyUserFoundMessenger messenger;

  public DefaultFindUserUseCase(
      FindUserClient findUserClient,
      SaveUserRepository saveUserRepository,
      NotifyUserFoundMessenger messenger) {
    this.findUserClient = findUserClient;
    this.saveUserRepository = saveUserRepository;
    this.messenger = messenger;
  }

  @Override
  protected Result doExecute(Context context) {
    FindUserClient.Result apiResult;
    try {
      apiResult = findUserClient.execute(new FindUserClient.Context(context.ssn()));
    } catch (Exception e) {
      throw new UserLookupException("Failed to find user: " + context.ssn(), e);
    }

    var user = new User(context.ssn(), apiResult.name(), apiResult.email());
    saveUserRepository.execute(new SaveUserRepository.Context(user.name(), user.email()));

    messenger.execute(new NotifyUserFoundMessenger.Context(
        user.email(), "User found: " + user.name()));

    return new Result(user.name(), user.email());
  }
}
```


## Phase 2 Integration (Shell Layer)

The same use case receives production adapters via constructor injection — zero changes to domain code. See [shell](template/element/shell/ELEMENT.md) for Service wiring, entry points (REST, Kafka, CLI), and full Phase 2 assembly.

Production adapters are provided by:
- [shell-client](template/element/shell-client/ELEMENT.md) — external API implementation
- [shell-repository](template/element/shell-repository/ELEMENT.md) — persistence implementation
- [shell-messenger](template/element/shell-messenger/ELEMENT.md) — messaging implementation

## Related Elements

- [use-case-client](template/element/use-case-client/ELEMENT.md) — external API contract (Client)
- [use-case-repository](template/element/use-case-repository/ELEMENT.md) — persistence contract (Repository)
- [use-case-messenger](template/element/use-case-messenger/ELEMENT.md) — notification contract (Messenger)
- [shell-client](template/element/shell-client/ELEMENT.md) — Phase 2 infrastructure client wrapper
- [shell](template/element/shell/ELEMENT.md) — Phase 1 verification shells and Phase 2 delivery entry points
