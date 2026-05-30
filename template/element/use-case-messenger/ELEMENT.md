---
name: use-case-messenger
description: Defines a notification contract for async messaging and events. JIT-discovered when use case logic requires sending notifications or publishing events. Keywords: use case messenger, notification, event, message, publish, notify.
metadata:
  class: "org.hermi.usecase.standard.Messenger"
  phase: "use case"
  priority: high
---

# Use Case Messenger

## Role & Design Intent

**Use this element when `doExecute` reveals a need to send a notification or publish an event.** A Use Case Messenger is a JIT-discovered I/O contract representing a single notification operation. It:

- Defines **one** messaging boundary (notify a user, publish order-placed event)
- Is generated **inline** in the use case source directory — not a separate module
- Remains technology-agnostic until Phase 2 (Kafka, email, SMS adapters)
- Returns data that crosses INTO the use case — `Result` **MUST** be `Validatable`

## Generation

The Messenger lives alongside the use case that discovered it.

### Directory

```
src/main/java/{org}/{resource}/{action}/usecase/
└── Notify{Fact}Messenger.java
```

### Contract Structure

```java
public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
  public record Context(String email, String message) {}
  public record Result(String messageId) implements Validatable {}
}
```

### Validatable Rules

| Boundary | Validatable? | Why |
|---|---|---|
| `Context` (flows OUT to infra) | No | Data leaving the core |
| `Result` (flows INTO the core) | **Yes** | Data entering the core must be validated |

### Naming

`Notify{Fact}Messenger` — e.g., `NotifyUserFoundMessenger`, `NotifyOrderPlacedMessenger`

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — discovers and owns this messenger
- [shell-messenger](template/element/shell-messenger/ELEMENT.md) — Phase 2 wraps this contract with technology
