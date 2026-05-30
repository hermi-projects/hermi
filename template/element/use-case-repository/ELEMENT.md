---
name: use-case-repository
description: Defines a persistence contract for data storage. JIT-discovered when use case logic requires saving, updating, or retrieving data. Keywords: use case repository, persistence, data access, storage, save, retrieve.
metadata:
  class: "org.hermi.usecase.standard.Repository"
  phase: "use case"
  priority: high
---

# Use Case Repository

## Role & Design Intent

**Use this element when `doExecute` reveals a need to persist or retrieve data.** A Use Case Repository is a JIT-discovered I/O contract representing a single data storage operation. It:

- Defines **one** persistence boundary (save a user, find an order)
- Is generated **inline** in the use case source directory — not a separate module
- Remains technology-agnostic until Phase 2 (JPA, JDBC, Mongo adapters)
- Returns data that crosses INTO the use case — `Result` **MUST** be `Validatable`

## Generation

The Repository lives alongside the use case that discovered it.

### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
└── {Action}{Resource}Repository.java
```

### Contract Structure

```java
public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
  public record Context(String name, String email) {}
  public record Result(String id) implements Validatable {}
}
```

### Validatable Rules

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (flows OUT to infra) | No | Data leaving the core |
| `Result` (flows INTO the core) | **Yes** | Data entering the core must be validated |

### Naming

`{Action}{Resource}Repository` — e.g., `SaveUserRepository`, `FindUserRepository`

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this repository
- [shell-repository](template/element/shell-repository/ELEMENT.md) — Phase 2 wraps this contract with technology
