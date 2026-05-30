---
name: shell-messenger
description: Provides technology-specific messaging or event adapters for notification and event publishing. Use when a use case requires sending notifications or publishing events. Keywords: shell messenger, kafka producer, event publisher, message broker, notification adapter, vendor messenger, console messenger.
metadata:
  class: "org.hermi.shell.Messenger"
  phase: "shell"
  priority: high
---

# Shell Messenger

## Role & Design Intent

**Use this element in Phase 2 to provide the production implementation for a [use-case-messenger](template/element/use-case-messenger/ELEMENT.md).** The Shell Messenger materializes the abstract messaging contract with concrete technology. It follows a two-layer adapter architecture:

- **Vendor Messenger** — raw technology I/O wrapper (Kafka producer). Extends `org.hermi.shell.Messenger`.
- **Mapper** — converts between use-case domain types (`Context`, `Result`) and vendor-specific message types (`ProducerRecord`, `RecordMetadata`). Implements `org.hermi.shell.Mapper`.
- **Production Implementation** — extends the use-case messenger contract (e.g., `NotifyUserFoundMessenger`) and delegates to a Vendor Messenger + Mapper. This is what gets injected into the use case.

The Shell Messenger follows **Prefix Isolation**: every infrastructure class MUST have the technology name as its first word.

## Generation

### Directory

```
src/main/java/{org}/{resource}/{action}/shell/messenger/
├── KafkaNotifyUserFoundMessenger.java   # Production Implementation
├── KafkaUserMapper.java                 # Vendor Mapper
└── KafkaUserMessenger.java              # Vendor Messenger
```

### Contract Structure

#### 1. Vendor Messenger — Technology I/O Base

```java
import org.hermi.shell.Messenger;

@Component
public class KafkaUserMessenger extends Messenger<ProducerRecord<String, String>, RecordMetadata> {
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  public KafkaUserMessenger(KafkaTemplate<String, String> kafkaTemplate) {
    super(null);
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  protected RecordMetadata doPublish(ProducerRecord<String, String> payload) {
    return kafkaTemplate.send(payload).get().getRecordMetadata();
  }
}
```

#### 2. Mapper — Type Conversion

```java
import org.hermi.shell.Mapper;

@Component
public class KafkaUserMapper implements Mapper<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result, ProducerRecord<String, String>, RecordMetadata> {
  @Override
  public ProducerRecord<String, String> toPayload(NotifyUserFoundMessenger.Context context) {
    return new ProducerRecord<>("user.notifications", context.message());
  }

  @Override
  public Result toResult(RecordMetadata result) {
    return new Result(result.toString());
  }
}
```

#### 3. Production Implementation — Use Case Contract Adapter

```java
@Component
public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger {
  private final Messenger<ProducerRecord<String, String>, RecordMetadata> messenger;
  private final Mapper<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result, ProducerRecord<String, String>, RecordMetadata> mapper;

  @Autowired
  public KafkaNotifyUserFoundMessenger(
      KafkaUserMessenger kafkaUserMessenger,
      KafkaUserMapper kafkaUserMapper) {
    this.messenger = kafkaUserMessenger;
    this.mapper = kafkaUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    ProducerRecord<String, String> record = mapper.toPayload(context);
    RecordMetadata metadata = messenger.publish(record);
    return mapper.toResult(metadata);
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
| Production Implementation | `{Tech\|Vendor}{ActualContractName}` | `KafkaNotifyUserFoundMessenger` |
| Vendor Messenger | `{Tech}{Resource}Messenger` | `KafkaUserMessenger` |
| Mapper | `{Vendor}{Resource}Mapper` | `KafkaUserMapper` |

### Phase 1 Testing

For Phase 1 verification without infrastructure, use the reusable `org.hermi.shell.util.ConsoleMessenger` — a generic in-memory messenger backed by `ConcurrentHashMap` with a `NoopPersistentAuditor`:

```java
import org.hermi.shell.util.ConsoleMessenger;

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
| Auditor | `NoopPersistentAuditor` (no-op) |
| API | `put()`, `get()`, `remove()`, `contains()`, `clear()`, `size()` |

## Forbidden Patterns

- ❌ **No business logic** in Vendor Messenger or Production Implementation — they are gateways only
- ❌ **No use-case domain types** leaking into Vendor Messenger (`Context`, `Result` must stay in Production Implementation)
- ❌ **No synchronous blocking** in `doPublish` without timeout handling — messaging infrastructure may hang
- ❌ **No direct Kafka/technology imports** in use-case module — Shell must be a separate module

## Complete Example

```java
// ==== 1. Vendor Messenger — Raw Kafka Producer ====
@Component
public class KafkaUserMessenger extends Messenger<ProducerRecord<String, String>, RecordMetadata> {
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  public KafkaUserMessenger(KafkaTemplate<String, String> kafkaTemplate) {
    super(null);
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  protected RecordMetadata doPublish(ProducerRecord<String, String> payload) {
    return kafkaTemplate.send(payload).get().getRecordMetadata();
  }
}

// ==== 2. Mapper — Use Case ↔ Kafka Types ====
@Component
public class KafkaUserMapper implements Mapper<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result, ProducerRecord<String, String>, RecordMetadata> {
  @Override
  public ProducerRecord<String, String> toPayload(NotifyUserFoundMessenger.Context context) {
    return new ProducerRecord<>("user.notifications", context.email() + ": " + context.message());
  }

  @Override
  public Result toResult(RecordMetadata result) {
    return new Result(result.toString());
  }
}

// ==== 3. Production Implementation — Use Case Contract Adapter ====
@Component
public class KafkaNotifyUserFoundMessenger extends NotifyUserFoundMessenger {
  private final Messenger<ProducerRecord<String, String>, RecordMetadata> messenger;
  private final Mapper<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result, ProducerRecord<String, String>, RecordMetadata> mapper;

  @Autowired
  public KafkaNotifyUserFoundMessenger(
      KafkaUserMessenger kafkaUserMessenger,
      KafkaUserMapper kafkaUserMapper) {
    this.messenger = kafkaUserMessenger;
    this.mapper = kafkaUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    ProducerRecord<String, String> record = mapper.toPayload(context);
    RecordMetadata metadata = messenger.publish(record);
    return mapper.toResult(metadata);
  }
}
```

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — the use case this shell messenger supports
- [use-case-messenger](template/element/use-case-messenger/ELEMENT.md) — abstract contract this shell messenger implements
