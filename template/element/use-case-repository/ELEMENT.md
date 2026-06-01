---
name: use-case-repository
description: Defines an abstract persistence contract within the use case module. Use when a use case requires persisting or retrieving data. Keywords: use case repository, persistence, data access, storage, save, retrieve, abstract contract.
metadata:
  class: "org.hermi.usecase.standard.Repository"
  phase: "use case"
  priority: high
---

# Use Case Repository

## Role & Design Intent

**Use Case Repository is the abstract contract for data persistence within the use case module.** It defines the input (`Context`) and output (`Result`) types for a single storage operation, remaining technology-agnostic — no JPA, no SQL, no vendor imports.

It handles:
- Defining the I/O boundary for a persistence operation
- Input validation (via `Validatable` on `Result`)
- Inversion of control — use cases depend on this abstract class, not concrete implementations

It does NOT handle:
- Data storage and retrieval — use [shell-repository](template/element/shell-repository/ELEMENT.md)
- Type conversion — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Any vendor-specific logic

## Required Inputs

The following inputs are needed to generate a Use Case Repository:

| Input | Description | Example |
|---|---|---|
| Technology | Persistence technology | `JPA` |
| Resource | Business object | `User` |
| Action | Operation name | `Save` |
| Context Fields | Data to persist | `name: String, email: String` |
| Result Fields | Data returned after persistence | `id: String` |

## Output Specification

When generating the code, please adhere to the following structure:

1. **Abstract Class**: Extends `org.hermi.usecase.standard.Repository<Context, Result>`
2. **Context record**: Input parameters flowing out to infrastructure
3. **Result record**: Output data flowing into the use case, implementing `Validatable`
4. **Vendor Implementation**: A concrete class that composes shell-repository + mapper to fulfill the contract
5. **No implementation in abstract class**: Abstract class has no method bodies

## Generation

### Abstract Contract

#### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
└── {Action}{Resource}Repository.java
```

#### Code Skeleton

```java
public abstract class {Action}{Resource}Repository extends Repository<{Action}{Resource}Repository.Context, {Action}{Resource}Repository.Result> {
  public record Context({contextFields}) {}
  public record Result({resultFields}) implements Validatable {}
}
```

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (flows OUT to infra) | Optional | Data leaving the core |
| `Result` (flows INTO the core) | **Yes** | Data entering the core must be validated |

### Vendor Implementation

The concrete implementation wires a [shell-repository](template/element/shell-repository/ELEMENT.md) and [shell-mapper](template/element/shell-mapper/ELEMENT.md) together to fulfill the abstract contract:

#### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
├── {Action}{Resource}Repository.java                 # Abstract contract
└── {Tech}{Action}{Resource}Repository.java           # Vendor-specific implementation
```

#### Code Skeleton

```java
@Component
public class {Tech}{Action}{Resource}Repository extends {Action}{Resource}Repository {
  private final {Tech}{Resource}Repository vendorRepo;
  private final {Action}{Resource}Mapper mapper;

  public {Tech}{Action}{Resource}Repository({Tech}{Resource}Repository vendorRepo,
                                              {Action}{Resource}Mapper mapper) {
    this.vendorRepo = vendorRepo;
    this.mapper = mapper;
  }

  @Override
  protected Result doExecute(Context context) {
    var entity = mapper.toPayload(context);
    var saved = vendorRepo.save(entity);
    return mapper.toResult(saved);
  }
}
```

The implementation flow:

```
Context → [Mapper.toPayload] → entity → [Vendor Repository.save] → saved entity → [Mapper.toResult] → Result
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Use Case Repository | `{Action}{Resource}Repository` | `SaveUserRepository` |
| Vendor Implementation | `{Tech}{Action}{Resource}Repository` | `JpaSaveUserRepository` |

## Forbidden Patterns

- ❌ No vendor imports in abstract contract — must stay technology-agnostic
- ❌ No JPA, SQL, or persistence annotations — no `@Entity`, `@Table`, `@Id`
- ❌ No business logic in vendor implementation — it only delegates to mapper and shell-repository
- ❌ No `Result` without `Validatable` — data entering the core must be validated

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this repository
- [shell-repository](template/element/shell-repository/ELEMENT.md) — technology-specific implementation of this contract
- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and entity types
