---
name: shell-repository
description: Provides technology-specific persistence adapters for data storage and retrieval. Use when a use case requires persisting or reading data. Keywords: shell repository, spring data jpa, persistence adapter, data access, orm mapper, vendor repository, in memory repository.
metadata:
  class: "org.springframework.data.jpa.repository.JpaRepository"
  phase: "shell"
  priority: high
---

# Shell Repository

## Role & Design Intent

**Use this element in Phase 2 to provide the production implementation for a [use-case-repository](template/element/use-case-repository/ELEMENT.md).** The Shell Repository materializes the abstract persistence contract with concrete technology. It follows a two-layer adapter architecture:

- **Vendor Repository** — raw technology I/O wrapper (JPA repository). Extends the chosen ORM framework's `Repository` interface.
- **Mapper** — converts between use-case domain types (`Context`, `Result`) and vendor entity types. Implements `org.hermi.shell.Mapper`.
- **Production Implementation** — extends the use-case repository contract (e.g., `SaveUserRepository`) and delegates to a Vendor Repository + Mapper. This is what gets injected into the use case.

The Shell Repository follows **Prefix Isolation**: every infrastructure class MUST have the technology name as its first word.

## Generation

### Directory

```
src/main/java/{org}/{resource}/{action}/shell/repository/
├── JpaSaveUserRepository.java        # Production Implementation
├── JpaUserMapper.java                # Vendor Mapper
└── JpaUserRepository.java            # Vendor Repository
```

### Contract Structure

#### 1. Vendor Repository — Technology I/O Base

```java
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
}
```

#### 2. Mapper — Type Conversion

```java
import org.hermi.shell.Mapper;

@Component
public class JpaUserMapper implements Mapper<SaveUserRepository.Context, SaveUserRepository.Result, UserEntity, UserEntity> {
  @Override
  public UserEntity toPayload(SaveUserRepository.Context context) {
    return new UserEntity(context.name(), context.email());
  }

  @Override
  public Result toResult(UserEntity entity) {
    return new Result(entity.getId());
  }
}
```

#### 3. Production Implementation — Use Case Contract Adapter

```java
@Component
public class JpaSaveUserRepository extends SaveUserRepository {
  private final JpaUserRepository jpaRepository;
  private final Mapper<SaveUserRepository.Context, SaveUserRepository.Result, UserEntity, UserEntity> mapper;

  @Autowired
  public JpaSaveUserRepository(
      JpaUserRepository jpaRepository,
      JpaUserMapper jpaUserMapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = jpaUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    UserEntity entity = mapper.toPayload(context);
    UserEntity savedEntity = jpaRepository.save(entity);
    return mapper.toResult(savedEntity);
  }
}
```

### Validatable Rules

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (received from use case) | No | Data already validated before entering the use case |
| `Result` (returned to use case) | **Yes** (implicit) | Abstract contract declares `Result implements Validatable`, enforcement is in the base class |

### Naming

| Component | Pattern | Example |
|---|---|---|
| Production Implementation | `{Tech\|Vendor}{ActualContractName}` | `JpaSaveUserRepository` |
| Vendor Repository | `{Tech}{Resource}Repository` | `JpaUserRepository` |
| Mapper | `{Vendor}{Resource}Mapper` | `JpaUserMapper` |

### Phase 1 Testing

For Phase 1 verification without infrastructure, use the reusable `org.hermi.shell.util.InMemoryRepository` — a generic in-memory data store backed by `ConcurrentHashMap`:

```java
import org.hermi.shell.util.InMemoryRepository;

var repo = new InMemoryRepository<SaveUserRepository.Context, SaveUserRepository.Result>()
    .put(new SaveUserRepository.Context("John", "john@example.com"),
         new SaveUserRepository.Result("id-001"));

SaveUserRepository.Result result = repo.process(
    new SaveUserRepository.Context("John", "john@example.com"));
```

> `InMemoryRepository` is a standalone utility — it does not extend a Hermi base class, making it a lightweight option for any persistence testing.

| Feature | Detail |
|---|---|
| Class | `InMemoryRepository<E, R>` |
| Package | `org.hermi.shell.util` |
| Backing Store | `ConcurrentHashMap<E, R>` |
| API | `put()`, `get()`, `remove()`, `process()`, `containsKey()`, `clear()`, `size()` |

## Forbidden Patterns

- ❌ **No business logic** in Vendor Repository or Production Implementation — they are data access only
- ❌ **No use-case domain types** leaking into Vendor Repository (`Context`, `Result` must stay in Production Implementation)
- ❌ **No vendor-specific annotations** (`@Table`, `@Column`) on use-case domain models — mapping belongs in Mapper + entity
- ❌ **No direct JPA/Spring imports** in use-case module — Shell must be a separate module

## Complete Example

```java
// ==== 1. Vendor Repository — Spring Data JPA ====
@Entity
@Table(name = "users")
public class UserEntity {
  @Id @GeneratedValue
  private Long id;
  private String name;
  private String email;

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
}

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
}

// ==== 2. Mapper — Use Case ↔ JPA Entity ====
@Component
public class JpaUserMapper implements Mapper<SaveUserRepository.Context, SaveUserRepository.Result, UserEntity, UserEntity> {
  @Override
  public UserEntity toPayload(SaveUserRepository.Context context) {
    var entity = new UserEntity();
    entity.setName(context.name());
    entity.setEmail(context.email());
    return entity;
  }

  @Override
  public Result toResult(UserEntity entity) {
    return new Result(entity.getId().toString());
  }
}

// ==== 3. Production Implementation — Use Case Contract Adapter ====
@Component
public class JpaSaveUserRepository extends SaveUserRepository {
  private final JpaUserRepository jpaRepository;
  private final Mapper<SaveUserRepository.Context, SaveUserRepository.Result, UserEntity, UserEntity> mapper;

  @Autowired
  public JpaSaveUserRepository(
      JpaUserRepository jpaRepository,
      JpaUserMapper jpaUserMapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = jpaUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    UserEntity entity = mapper.toPayload(context);
    UserEntity savedEntity = jpaRepository.save(entity);
    return mapper.toResult(savedEntity);
  }
}
```

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — the use case this shell repository supports
- [use-case-repository](template/element/use-case-repository/ELEMENT.md) — abstract contract this shell repository implements
