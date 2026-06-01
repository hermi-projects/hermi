---
name: shell-messenger
description: Provides technology-specific messengers for notification and event publishing. Use when a use case requires sending notifications or publishing events. Keywords: shell messenger, kafka producer, event publisher, message broker, notification adapter, vendor messenger, console messenger.
metadata:
  class: "org.hermi.shell.Messenger"
  phase: "shell"
  priority: high
---

# Shell Messenger

## Role & Design Intent

**Vendor Messenger is the protocol-layer wrapper for messaging and event publishing.** It extends `org.hermi.shell.Messenger<P, R>` and is responsible only for message transmission — Kafka, JMS, SQS, or any other messaging protocol.

It handles:
- Message publishing to brokers and queues
- Serialization
- Configuration binding (bootstrap servers, topic names)

It does NOT handle:
- Business logic — vendor messengers are gateways only
- Type conversion between domain and vendor types — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Orchestration or composition — use [use-case-messenger](template/element/use-case-messenger/ELEMENT.md)

## Required Inputs

The following inputs are needed to generate a Vendor Messenger:

| Input | Description | Example |
|---|---|---|
| Vendor | Messaging technology name | `Kafka` |
| Resource | Business object | `User` |
| Topic / Queue | Destination name | `user.notifications` |
| Payload Type | Data sent to the broker | `ProducerRecord<String, String>` |
| Response Type | Data received from the broker | `RecordMetadata` |
| Config | Externalized configuration keys | `bootstrap-servers`, `acks` |

## Output Specification

When generating the code, please adhere to the following structure:

1. **Class Definition**: Use the naming convention `{Vendor}{Resource}Messenger`.
2. **Constructor**: Inject necessary configuration and dependencies.
3. **Publish Method**: Implement the primary method to publish the message, strictly using the provided Payload/Response types.
4. **No Logic**: Ensure the method body contains no business logic or data transformation code.

## Generation

### Base Class Contract

`org.hermi.shell.Messenger<P, R>` provides:
- `publish(P payload)` — public API, triggers the full lifecycle
- Audit lifecycle: `recordContext` → `doPublish` → `recordResult` / `recordError`
- Subclass only needs to implement `doPublish(P payload)`

The base class has two constructors:
- `Messenger(PersistentAuditor<P, R>)` — for production auditing
- `Messenger()` — uses `NoopPersistentAuditor` as default

### Implementation Steps

1. Extend `org.hermi.shell.Messenger<{PayloadType}, {ResponseType}>`
2. Inject dependencies (KafkaTemplate, JMS client, configuration, etc.)
3. Implement `doPublish` — send the message and return the broker's response
4. Use `@Component` (or equivalent) to register as a Spring bean

### Code Skeleton

```java
@Component
public class {Vendor}{Resource}Messenger extends Messenger<{PayloadType}, {ResponseType}> {

  public {Vendor}{Resource}Messenger({Dependencies} dependencies) {
    // Use super() for NoopPersistentAuditor, or super(auditor) for production auditing
  }

  @Override
  protected {ResponseType} doPublish({PayloadType} payload) {
    // Protocol transmission only
  }
}
```

### Directory

```
src/main/java/{org}/{resource}/shell/messenger/
└── {Vendor}{Resource}Messenger.java
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Vendor Messenger | `{Vendor}{Resource}Messenger` | `KafkaUserMessenger` |

Prefix Isolation applies: the vendor/technology name MUST be the first word of the class name.

## Examples

### ConsoleMessenger — Testing

`org.hermi.shell.util.ConsoleMessenger` is a reusable `Messenger` implementation backed by `ConcurrentHashMap`. Use it for testing without infrastructure:

```java
var messenger = new ConsoleMessenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result>()
    .put(new NotifyUserFoundMessenger.Context("john@example.com", "User found"),
         new NotifyUserFoundMessenger.Result("msg-123"));

NotifyUserFoundMessenger.Result result = messenger.publish(
    new NotifyUserFoundMessenger.Context("john@example.com", "User found"));
```

| Feature | Detail |
|---|---|
| Class | `ConsoleMessenger<P, R> extends Messenger<P, R>` |
| Package | `org.hermi.shell.util` |
| Backing Store | `ConcurrentHashMap<P, R>` |
| Auditor | `NoopPersistentAuditor` (default) |
| API | `put()`, `get()`, `remove()`, `contains()`, `clear()`, `size()` |

### Kafka — Production

```java
@Component
public class KafkaUserMessenger extends Messenger<ProducerRecord<String, String>, RecordMetadata> {
  private final KafkaTemplate<String, String> kafkaTemplate;

  public KafkaUserMessenger(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  protected RecordMetadata doPublish(ProducerRecord<String, String> payload) {
    try {
      return kafkaTemplate.send(payload).get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Kafka publish failed for topic: " + payload.topic(), e);
    }
  }
}
```

The base class catches any exception thrown by `doPublish`, records it via the auditor, and rethrows it — vendor messengers do not need try-catch in `doPublish` unless vendor-specific error wrapping is required.

## Forbidden Patterns

- ❌ No business logic — vendor messengers are gateways only
- ❌ No type conversion — domain↔vendor mapping belongs in [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- ❌ No hardcoded configuration — all configurable values must be injected via constructor
- ❌ No synchronous blocking without timeout handling — messaging infrastructure may hang
- ❌ No vendor technology imports in use-case modules — shell must be a separate module

## Related Elements

- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and vendor types
- [use-case-messenger](template/element/use-case-messenger/ELEMENT.md) — abstract messaging contract
