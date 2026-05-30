---
name: shell-client
description: Provides technology-specific clients for calling external or third-party APIs. Use when a use case requires integrating with an external system. Keywords: shell client, vendor client, rest template, external api, http adapter, api client, vendor mapper, local client.
metadata:
  class: "org.hermi.shell.Client"
  phase: "shell"
  priority: high
---

# Shell Client

## Role & Design Intent

**Use this element in Phase 2 to provide the production implementation for a [use-case-client](template/element/use-case-client/ELEMENT.md).** The Shell Client materializes the abstract external API contract with concrete technology. It follows a two-layer adapter architecture:

- **Vendor Client** — raw technology I/O wrapper (REST client). Extends `org.hermi.shell.Client`. May include a `PersistentAuditor` for audit tracking.
- **Mapper** — converts between use-case domain types (`Context`, `Result`) and vendor-specific types (`Payload`, `Response`). Implements `org.hermi.shell.Mapper`.
- **Production Implementation** — extends the use-case client contract (e.g., `FindUserClient`) and delegates to a Vendor Client + Mapper. This is what gets injected into the use case.

The Shell Client follows **Prefix Isolation**: every infrastructure class MUST have the technology name as its first word.

## Generation

### Directory

```
src/main/java/{org}/{resource}/{action}/shell/client/
├── LexisNexisFindUserClient.java       # Production Implementation
├── LexisNexisUserMapper.java           # Vendor Mapper
├── LexisNexisUserAuditor.java          # Vendor Auditor
└── LexisNexisUserClient.java           # Vendor Client
```

### Contract Structure

#### 1. Vendor Client — Technology I/O Base

```java
import org.hermi.shell.Client;
import org.hermi.shell.audit.PersistentAuditor;

@Component
public class LexisNexisUserClient extends Client<LexisNexisPayload, LexisNexisResponse> {
  private final RestTemplate restTemplate;

  @Autowired
  public LexisNexisUserClient(RestTemplate restTemplate, LexisNexisUserAuditor auditor) {
    super(auditor);
    this.restTemplate = restTemplate;
  }

  @Override
  protected LexisNexisResponse doExchange(LexisNexisPayload payload) {
    return restTemplate.postForObject("/api/users", payload, LexisNexisResponse.class);
  }
}
```

```java
@Component
public class LexisNexisUserAuditor extends PersistentAuditor<LexisNexisPayload, LexisNexisResponse> {
  @Override
  protected UUID doRecordContext(LexisNexisPayload payload) { return UUID.randomUUID(); }
  @Override
  protected void doRecordResult(UUID trackingId, LexisNexisResponse response) {}
  @Override
  protected void doRecordError(UUID trackingId, LexisNexisPayload payload, Exception exception) {}
}
```

#### 2. Mapper — Type Conversion

```java
import org.hermi.shell.Mapper;

@Component
public class LexisNexisUserMapper implements Mapper<FindUserClient.Context, FindUserClient.Result, LexisNexisPayload, LexisNexisResponse> {
  @Override
  public LexisNexisPayload toPayload(FindUserClient.Context context) {
    return new LexisNexisPayload(context.ssn());
  }

  @Override
  public Result toResult(LexisNexisResponse response) {
    return new Result(response.getName(), response.getEmail());
  }
}
```

#### 3. Production Implementation — Use Case Contract Adapter

```java
@Component
public class LexisNexisFindUserClient extends FindUserClient {
  private final Client<LexisNexisPayload, LexisNexisResponse> client;
  private final Mapper<FindUserClient.Context, FindUserClient.Result, LexisNexisPayload, LexisNexisResponse> mapper;

  @Autowired
  public LexisNexisFindUserClient(
      LexisNexisUserClient lexisNexisUserClient,
      LexisNexisUserMapper lexisNexisUserMapper) {
    this.client = lexisNexisUserClient;
    this.mapper = lexisNexisUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    LexisNexisPayload apiRequest = mapper.toPayload(context);
    LexisNexisResponse apiResponse = client.exchange(apiRequest);
    return mapper.toResult(apiResponse);
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
| Production Implementation | `{Vendor}{ActualContractName}` | `LexisNexisFindUserClient` |
| Vendor Client | `{Vendor}{Resource}Client` | `LexisNexisUserClient` |
| Mapper | `{Vendor}{Resource}Mapper` | `LexisNexisUserMapper` |
| Auditor | `{Vendor}{Resource}Auditor` | `LexisNexisUserAuditor` |

### Phase 1 Testing

For Phase 1 verification without infrastructure, use the reusable `org.hermi.shell.util.LocalClient` — a generic in-memory client backed by `ConcurrentHashMap` with a `NoopPersistentAuditor`:

```java
import org.hermi.shell.util.LocalClient;

var client = new LocalClient<FindUserClient.Context, FindUserClient.Result>()
    .put(new FindUserClient.Context("123-45-6789"),
         new FindUserClient.Result("John Doe", "john@example.com"));

FindUserClient.Result result = client.exchange(new FindUserClient.Context("123-45-6789"));
```

| Feature | Detail |
|---|---|
| Class | `LocalClient<P, R> extends Client<P, R>` |
| Package | `org.hermi.shell.util` |
| Backing Store | `ConcurrentHashMap<P, R>` |
| Auditor | `NoopPersistentAuditor` (no-op) |
| API | `put()`, `get()`, `remove()`, `containsKey()`, `clear()`, `size()` |

## Forbidden Patterns

- ❌ **No business logic** in Vendor Client or Production Implementation — they are gateways only
- ❌ **No use-case domain types** leaking into Vendor Client (`Context`, `Result` must stay in Production Implementation)
- ❌ **No direct vendor/REST imports** in use-case module — Shell must be a separate module
- ❌ **No raw HTTP client usage** outside of the Vendor Client layer — I/O must stay encapsulated

## Complete Example

```java
// ==== 1. Vendor Client — REST API Wrapper ====
@Component
public class LexisNexisUserClient extends Client<LexisNexisPayload, LexisNexisResponse> {
  private final RestTemplate restTemplate;

  @Autowired
  public LexisNexisUserClient(RestTemplate restTemplate, LexisNexisUserAuditor auditor) {
    super(auditor);
    this.restTemplate = restTemplate;
  }

  @Override
  protected LexisNexisResponse doExchange(LexisNexisPayload payload) {
    return restTemplate.postForObject("/api/users", payload, LexisNexisResponse.class);
  }
}

// ==== 2. Mapper — Use Case ↔ Vendor Types ====
@Component
public class LexisNexisUserMapper implements Mapper<FindUserClient.Context, FindUserClient.Result, LexisNexisPayload, LexisNexisResponse> {
  @Override
  public LexisNexisPayload toPayload(FindUserClient.Context context) {
    return new LexisNexisPayload(context.ssn());
  }

  @Override
  public Result toResult(LexisNexisResponse response) {
    return new Result(response.getName(), response.getEmail());
  }
}

// ==== 3. Production Implementation — Use Case Contract Adapter ====
@Component
public class LexisNexisFindUserClient extends FindUserClient {
  private final Client<LexisNexisPayload, LexisNexisResponse> client;
  private final Mapper<FindUserClient.Context, FindUserClient.Result, LexisNexisPayload, LexisNexisResponse> mapper;

  @Autowired
  public LexisNexisFindUserClient(
      LexisNexisUserClient lexisNexisUserClient,
      LexisNexisUserMapper lexisNexisUserMapper) {
    this.client = lexisNexisUserClient;
    this.mapper = lexisNexisUserMapper;
  }

  @Override
  protected Result doExecute(Context context) {
    LexisNexisPayload apiRequest = mapper.toPayload(context);
    LexisNexisResponse apiResponse = client.exchange(apiRequest);
    return mapper.toResult(apiResponse);
  }
}
```

## Related Elements

- [use-case](template/element/use-case/ELEMENT.md) — the use case this shell client supports
- [use-case-client](template/element/use-case-client/ELEMENT.md) — abstract contract this shell client implements
