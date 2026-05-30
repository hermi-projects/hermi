---
name: use-case-client
description: Defines an external API contract for data lookup and remote service calls. JIT-discovered when use case logic requires calling an external service or API. Keywords: use case client, external API, remote call, service client, http.
metadata:
  class: "org.hermi.usecase.standard.Client"
  phase: "use case"
  priority: high
---

# Use Case Client

## Role & Design Intent

**Use this element when `doExecute` reveals a need to call an external API or remote service.** A Use Case Client is a JIT-discovered I/O contract representing a single external operation. It:

- Defines **one** remote boundary (fetch user data, validate address, lookup credit score)
- Is generated **inline** in the use case source directory — not a separate module
- Remains technology-agnostic until Phase 2 (REST, gRPC, SOAP adapters)
- Returns data that crosses INTO the use case — `Result` **MUST** be `Validatable`

## Generation

The Client lives alongside the use case that discovered it.

### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
└── {Action}{Resource}Client.java
```

### Contract Structure

```java
public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
  public record Context(String ssn) {}
  public record Result(String name, String email) implements Validatable {}
}
```

### Validatable Rules

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (flows OUT to infra) | Optional | Data leaving the core |
| `Result` (flows INTO the core) | **Yes** | Data entering the core must be validated |

### Naming

`{Action}{Resource}Client` — e.g., `FindUserClient`, `ValidateAddressClient`, `LookupCreditScoreClient`

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this client
- [shell-client](template/element/shell-client/ELEMENT.md) — Phase 2 wraps this contract with technology
