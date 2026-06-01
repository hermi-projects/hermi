---
name: use-case-messenger
description: Defines an abstract notification contract within the use case module. Use when a use case requires sending notifications or publishing events. Keywords: use case messenger, notification, event, message, publish, notify, abstract contract.
metadata:
  class: "org.hermi.usecase.standard.Messenger"
  phase: "use case"
  priority: high
---

# Use Case Messenger

## Role & Design Intent

**Use Case Messenger is the abstract contract for notifications and event publishing within the use case module.** It defines the input (`Context`) and output (`Result`) types for a single messaging operation, remaining technology-agnostic — no Kafka, no JMS, no vendor imports.

It handles:
- Defining the I/O boundary for a notification operation
- Input validation (via `Validatable` on `Result`)
- Inversion of control — use cases depend on this abstract class, not concrete implementations

It does NOT handle:
- Message publishing — use [shell-messenger](template/element/shell-messenger/ELEMENT.md)
- Type conversion — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Any vendor-specific logic

## Required Inputs

The following inputs are needed to generate a Use Case Messenger:

| Input | Description | Example |
|---|---|---|
| Vendor | Messaging technology | `Kafka` |
| Resource / Fact | Business event being notified | `UserFound` |
| Context Fields | Data sent with the notification | `email: String, message: String` |
| Result Fields | Data returned after publishing | `messageId: String` |

## Output Specification

When generating the code, please adhere to the following structure:

1. **Abstract Class**: Extends `org.hermi.usecase.standard.Messenger<Context, Result>`
2. **Context record**: Input parameters flowing out to infrastructure
3. **Result record**: Output data flowing into the use case, implementing `Validatable`
4. **Vendor Implementation**: A concrete class that composes shell-messenger + mapper to fulfill the contract
5. **No implementation in abstract class**: Abstract class has no method bodies

## Generation

### Abstract Contract

#### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
└── Notify{Fact}Messenger.java
```

#### Code Skeleton

```java
public abstract class Notify{Fact}Messenger extends Messenger<Notify{Fact}Messenger.Context, Notify{Fact}Messenger.Result> {
  public record Context({contextFields}) {}
  public record Result({resultFields}) implements Validatable {}
}
```

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (flows OUT to infra) | Optional | Data leaving the core |
| `Result` (flows INTO the core) | **Yes** | Data entering the core must be validated |

### Vendor Implementation

The concrete implementation wires a [shell-messenger](template/element/shell-messenger/ELEMENT.md) and [shell-mapper](template/element/shell-mapper/ELEMENT.md) together to fulfill the abstract contract:

#### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
├── Notify{Fact}Messenger.java                         # Abstract contract
└── {Vendor}Notify{Fact}Messenger.java                 # Vendor-specific implementation
```

#### Code Skeleton

```java
@Component
public class {Vendor}Notify{Fact}Messenger extends Notify{Fact}Messenger {
  private final {Vendor}{Resource}Messenger vendorMessenger;
  private final Notify{Fact}Mapper mapper;

  public {Vendor}Notify{Fact}Messenger({Vendor}{Resource}Messenger vendorMessenger,
                                         Notify{Fact}Mapper mapper) {
    this.vendorMessenger = vendorMessenger;
    this.mapper = mapper;
  }

  @Override
  protected Result doExecute(Context context) {
    var payload = mapper.toPayload(context);
    var response = vendorMessenger.publish(payload);
    return mapper.toResult(response);
  }
}
```

The implementation flow:

```
Context → [Mapper.toPayload] → vendor payload → [Vendor Messenger.publish] → vendor response → [Mapper.toResult] → Result
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Use Case Messenger | `Notify{Fact}Messenger` | `NotifyUserFoundMessenger` |
| Vendor Implementation | `{Vendor}Notify{Fact}Messenger` | `KafkaNotifyUserFoundMessenger` |

## Forbidden Patterns

- ❌ No vendor imports in abstract contract — must stay technology-agnostic
- ❌ No messaging protocol details — no Kafka, JMS, SQS references
- ❌ No business logic in vendor implementation — it only delegates to mapper and shell-messenger
- ❌ No `Result` without `Validatable` — data entering the core must be validated

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this messenger
- [shell-messenger](template/element/shell-messenger/ELEMENT.md) — technology-specific implementation of this contract
- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and vendor types
