---
name: use-case-client
description: Defines an abstract external API contract within the use case module. Use when a use case requires calling an external service. Keywords: use case client, external API, remote call, service client, abstract contract.
metadata:
  class: "org.hermi.usecase.standard.Client"
  phase: "use case"
  priority: high
---

# Use Case Client

**Use Case Client is an element for external service calls within the use case module.** It defines the input (`Context`) and output (`Result`) types for a single external operation, remaining technology-agnostic — no vendor imports, no protocol details.

## When to Use This Element
- Defining the I/O boundary for an external call
- Input validation (via `Validatable` on `Result`)
- Inversion of control — use case element depends on use case client abstract class, not concrete implementations

## When Not to Use This Element
- Protocol transmission — use [shell-client](template/element/shell-client/ELEMENT.md)
- Type conversion — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Any vendor-specific logic

## Required Inputs

### Contract

| Input | Description | Example |
|---|---|---|
| Resource | Business object | `User` |
| Action | Operation name | `Find` |
| Context Fields | Data sent to the external service | `ssn: String` |
| Result Fields | Data returned from the external service | `name: String, email: String` |

### Vendor Implementation

| Input | Description | Example |
|---|---|---|
| Vendor | Third-party service name | `LexisNexis` |

## Output Spec

`Client<Context, Result>` uses the **Template Method** pattern: the base class provides `execute(Context)` which handles cross-cutting concerns (logging, validation, error handling). Specifically, `execute()` automatically runs Jakarta Bean Validation on any `Context` or `Result` that implements `Validatable` — subclasses get validation for free, no explicit call needed. Subclasses only implement `doExecute(Context)` containing the operation-specific logic. Never override `execute()` directly.

When generating the code, adhere to this structure:

1. **Abstract Class**: Extends `org.hermi.usecase.standard.Client<Context, Result>`
2. **Nested records**: Inner records `Context` and `Result` carry the I/O data. Both implement `Validatable` (marker interface from `org.hermi.constraint.validation`, no methods). Fields use `jakarta.validation.constraints` annotations (`@NotNull`, `@NotEmpty`, etc.) to declare validation rules. The `Client` base class automatically validates any record implementing `Validatable` — no manual validation code needed.

## Naming

| Component | Pattern | Example |
|---|---|---|
| Use Case Client Contract | `{Action}{Resource}Client` | `FindUserClient` |
| Use Case Client Vendor Implementation | `{Vendor}{Action}{Resource}Client` |`LocalFindUserClient`, `LexisNexisFindUserClient` |

## Examples

### Example 1: Contract
```java
import org.hermi.constraint.validation.Validatable;
import org.hermi.usecase.standard.Client;
import jakarta.validation.constraints.NotNull;

public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
  public record Context(@NotNull String ssn) implements Validatable {}
  public record Result(@NotNull String name, String email) implements Validatable {}
}
```

### Example 2: Local implementation

A stub that returns hardcoded data — used during use case testing/validation, never in production.

```java
public class LocalFindUserClient extends FindUserClient {
  private Result result = new Result("John Doe", "john@example.com");
  @Override
  protected Result doExecute(Context context) {
    return result;
  }
}
```

### Example 3: Vendor implementation

A concrete vendor implementation using Spring boot dependency injection. Constructor injection of the vendor-specific shell-client (for transport) and shell-mapper (for type conversion). No business logic — pure delegation.

```java
@Component
public class LexisNexisFindUserClient extends FindUserClient {
  private final LexisNexisUserClient vendorClient;
  private final LexisNexisFindUserMapper mapper;

  public LexisNexisFindUserClient(LexisNexisUserClient vendorClient,
                                   LexisNexisFindUserMapper mapper) {
    this.vendorClient = vendorClient;
    this.mapper = mapper;
  }

  @Override
  protected Result doExecute(Context context) {
    var payload = mapper.toPayload(context);
    var response = vendorClient.exchange(payload);
    return mapper.toResult(response);
  }
}
```

The implementation flow:

```
Context → [Mapper.toPayload] → vendor payload → [Vendor Client.exchange] → vendor response → [Mapper.toResult] → Result
```

## Forbidden Patterns

- ❌ No vendor imports in abstract contract — must stay technology-agnostic
- ❌ No protocol details — no HTTP, REST, gRPC references
- ❌ No business logic in vendor implementation — it only delegates to mapper and vendor client
- ❌ No `Result` without `Validatable` — data entering the core must be validated
- ❌ Never override `execute()` — override `doExecute()` instead (Template Method pattern)

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this client
- [shell-client](template/element/shell-client/ELEMENT.md) — technology-specific implementation of this contract
- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and vendor types
