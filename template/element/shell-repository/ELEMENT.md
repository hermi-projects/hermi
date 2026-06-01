---
name: shell-repository
description: Provides technology-specific repositories for data storage and retrieval. Use when a use case requires persisting or reading data. Keywords: shell repository, spring data jpa, persistence adapter, data access, orm mapper, vendor repository, in memory repository.
metadata:
  class: "org.springframework.data.jpa.repository.JpaRepository"
  phase: "shell"
  priority: high
---

# Shell Repository

## Role & Design Intent

**Vendor Repository is the technology-specific data access wrapper.** Unlike Client and Messenger which have shell-layer base classes, Repository directly uses the chosen persistence technology's API:

- **JPA**: a Spring Data interface extending `JpaRepository<Entity, Id>`
- **Other technologies**: the corresponding data access API (JDBC template, MongoDB repository, etc.)

It handles:
- Data storage and retrieval
- Query construction and execution
- Transaction management

It does NOT handle:
- Business logic — vendor repositories are data access only
- Type conversion between domain and entity types — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Orchestration or composition — use [use-case-repository](template/element/use-case-repository/ELEMENT.md)

## Required Inputs

The following inputs are needed to generate a Vendor Repository:

| Input | Description | Example |
|---|---|---|
| Technology | Persistence technology | `JPA`, `MongoDB` |
| Resource | Business object | `User` |
| Entity Type | Entity class mapped to the data store | `UserEntity` |
| ID Type | Entity identifier type | `Long` |
| Query Methods | Custom queries beyond standard CRUD | `findByEmail(String)` |

## Output Specification

When generating the code, please adhere to the following structure:

1. **Entity Definition**: Create the entity class with table mapping and fields.
2. **Repository Interface**: Use the naming convention `{Tech}{Resource}Repository` extending the technology's base interface.
3. **Query Methods**: Add custom query methods only when standard CRUD is insufficient.
4. **No Logic**: Ensure the repository contains no business logic or data transformation code.

## Generation

No shell-layer base class exists for Repository. The vendor repository directly uses the chosen persistence technology's API.

### JPA Pattern

```java
@Entity
@Table(name = "{resource}s")
public class {Resource}Entity {
  @Id @GeneratedValue
  private {IdType} id;
  // fields, constructors, getters, setters
}

public interface {Tech}{Resource}Repository extends JpaRepository<{Resource}Entity, {IdType}> {
  // Custom query methods (only when needed beyond standard CRUD)
  // Optional<UserEntity> findByEmail(String email);
}
```

### Implementation Steps

1. Define the entity class with JPA annotations (`@Entity`, `@Table`, `@Id`)
2. Create the repository interface extending `JpaRepository<Entity, Id>`
3. Add custom query methods only when needed beyond standard CRUD

### Directory

```
src/main/java/{org}/{resource}/shell/repository/
├── {Resource}Entity.java
└── {Tech}{Resource}Repository.java
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Vendor Repository | `{Tech}{Resource}Repository` | `JpaUserRepository` |
| Entity | `{Resource}Entity` | `UserEntity` |

Prefix Isolation applies: the technology name MUST be the first word of the repository class name.

## Examples

### InMemoryRepository — Testing

`org.hermi.shell.util.InMemoryRepository` is a reusable in-memory data store backed by `ConcurrentHashMap`. Use it for testing without infrastructure:

```java
var repo = new InMemoryRepository<SaveUserRepository.Context, SaveUserRepository.Result>()
    .put(new SaveUserRepository.Context("John", "john@example.com"),
         new SaveUserRepository.Result("id-001"));

SaveUserRepository.Result result = repo.process(
    new SaveUserRepository.Context("John", "john@example.com"));
```

| Feature | Detail |
|---|---|
| Class | `InMemoryRepository<E, R>` |
| Package | `org.hermi.shell.util` |
| Backing Store | `ConcurrentHashMap<E, R>` |
| API | `put()`, `get()`, `remove()`, `process()`, `containsKey()`, `clear()`, `size()` |

### JPA — Production

```java
@Entity
@Table(name = "users")
public class UserEntity {
  @Id @GeneratedValue
  private Long id;
  private String name;
  private String email;

  public UserEntity() {}

  public UserEntity(String name, String email) {
    this.name = name;
    this.email = email;
  }

  // getters and setters...
  public Long getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public void setName(String name) { this.name = name; }
  public void setEmail(String email) { this.email = email; }
}

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
}
```

Custom queries (when needed):
```java
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByEmail(String email);
  List<UserEntity> findByNameContaining(String keyword);
}
```

## Forbidden Patterns

- ❌ No business logic — vendor repositories are data access only
- ❌ No type conversion — domain↔entity mapping belongs in [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- ❌ No vendor-specific annotations on domain models — JPA annotations stay on entities
- ❌ No hardcoded configuration — connection settings must be externalized

## Related Elements

- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and entity types
- [use-case-repository](template/element/use-case-repository/ELEMENT.md) — abstract persistence contract
