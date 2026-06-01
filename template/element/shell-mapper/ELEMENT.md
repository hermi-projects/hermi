---
name: shell-mapper
description: Provides type conversion between domain types and vendor-specific types. Use when a shell component needs to translate between business data and external system formats. Keywords: shell mapper, type converter, anti-corruption layer, domain translation, vendor mapper, field mapping.
metadata:
  class: "org.hermi.shell.Mapper"
  phase: "shell"
  priority: high
---

# Shell Mapper

## Role & Design Intent

**Mapper is a pure translation layer between domain types and vendor-specific types.** It implements `org.hermi.shell.Mapper<UContext, UResult, SPayload, SResponse>` with two conversion directions:

- `toPayload(UContext)` — domain input → vendor request
- `toResult(SResponse)` — vendor response → domain output

Mapper is the **Anti-Corruption Gateway**: it ensures vendor-specific structures never leak into business logic. A Mapper contains only field-level mapping — no I/O, no branching, no business decisions.

## Required Inputs

The following inputs are needed to generate a Mapper:

| Input | Description | Example |
|---|---|---|
| Vendor | Third-party service name | `LexisNexis` |
| Use Case Action | What the use case does | `Find` |
| Use Case Resource | What the use case operates on | `User` |
| Domain Context | Use case input type | `FindUserClient.Context(ssn: String)` |
| Domain Result | Use case output type | `FindUserClient.Result(name, email)` |
| Vendor Payload | Type sent to the external system | `LexisNexisPayload(ssn: String)` |
| Vendor Response | Type received from the external system | `LexisNexisResponse(fullName, email)` |

Field mappings are derived from matching field names and types between domain ↔ vendor types. When field names differ (e.g., `name` vs `fullName`), the mapping must be explicit.

## Generation

### Interface Contract

```java
public interface Mapper<UContext, UResult, SPayload, SResponse> {
  SPayload toPayload(UContext context);
  UResult toResult(SResponse output);
}
```

### Implementation Rules

1. **Pure translation** — field-level mapping only
2. **No I/O** — never call REST, DB, or any external service
3. **No branching** — no business logic, no conditional routing
4. **No exceptions** — use `Optional` or null-checks for absent fields
5. **No dependencies** — no constructor injection, Mapper is a pure utility

### Code Skeleton

```java
public class {Vendor}{Action}{Resource}Mapper
    implements Mapper<{DomainContext}, {DomainResult}, {VendorPayload}, {VendorResponse}> {

  @Override
  public {VendorPayload} toPayload({DomainContext} context) {
    return new {VendorPayload}(
        context.{field1}(),
        context.{field2}()
    );
  }

  @Override
  public {DomainResult} toResult({VendorResponse} response) {
    return new {DomainResult}(
        response.get{Field1}(),
        response.get{Field2}()
    );
  }
}
```

### Directory

```
src/main/java/{org}/{resource}/shell/mapper/
└── {Vendor}{Action}{Resource}Mapper.java
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Mapper | `{Vendor}{Action}{Resource}Mapper` | `LexisNexisFindUserMapper` |

## Examples

### LexisNexis — Domain to Vendor API Mapping

```java
public class LexisNexisFindUserMapper
    implements Mapper<FindUserClient.Context, FindUserClient.Result,
                      LexisNexisPayload, LexisNexisResponse> {

  @Override
  public LexisNexisPayload toPayload(FindUserClient.Context context) {
    return new LexisNexisPayload(context.ssn());
  }

  @Override
  public FindUserClient.Result toResult(LexisNexisResponse response) {
    return new FindUserClient.Result(
        response.getFullName(),
        response.getEmail()
    );
  }
}
```

### JPA — Domain to Entity Mapping

```java
public class JpaSaveUserMapper
    implements Mapper<SaveUserRepository.Context, SaveUserRepository.Result,
                      UserEntity, UserEntity> {

  @Override
  public UserEntity toPayload(SaveUserRepository.Context context) {
    var entity = new UserEntity();
    entity.setName(context.name());
    entity.setEmail(context.email());
    return entity;
  }

  @Override
  public SaveUserRepository.Result toResult(UserEntity entity) {
    return new SaveUserRepository.Result(entity.getId().toString());
  }
}
```

### Kafka — Domain to Message Mapping

```java
public class KafkaNotifyUserFoundMapper
    implements Mapper<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result,
                      ProducerRecord<String, String>, RecordMetadata> {

  @Override
  public ProducerRecord<String, String> toPayload(NotifyUserFoundMessenger.Context context) {
    return new ProducerRecord<>("user.notifications",
        context.email() + ": " + context.message());
  }

  @Override
  public NotifyUserFoundMessenger.Result toResult(RecordMetadata metadata) {
    return new NotifyUserFoundMessenger.Result(metadata.toString());
  }
}
```

## Forbidden Patterns

- ❌ No business logic — mapping only
- ❌ No I/O — no REST, DB, or network calls
- ❌ No branching — no if/else, switch, or conditional routing
- ❌ No exceptions — use `Optional` for absent fields, never throw
- ❌ No dependencies — no constructor injection, Mapper is stateless

## Related Elements

- [shell-client](template/element/shell-client/ELEMENT.md) — vendor client that sends the mapped payload
- [shell-repository](template/element/shell-repository/ELEMENT.md) — vendor repository that persists the mapped entity
- [shell-messenger](template/element/shell-messenger/ELEMENT.md) — vendor messenger that publishes the mapped event
