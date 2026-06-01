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

**Vendor Client is the protocol-layer wrapper for third-party API calls.** It extends `org.hermi.shell.Client<P, R>` and is responsible only for protocol transmission — REST, gRPC, SOAP, or any other wire format.

It handles:
- Protocol transport and serialization
- Authentication (API Key, OAuth, Basic Auth)
- Configuration binding (base URL, timeouts)
- Resilience (Retries, Timeout management)
- Standardized Error Handling: Transforming wire-level errors into domain-aware exceptions.

It does NOT handle:
- Business logic — vendor clients are gateways only
- Type conversion between domain and vendor types — use [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- Orchestration or composition — use [use-case-client](template/element/use-case-client/ELEMENT.md)

## Required Inputs

The following inputs are needed to generate a Vendor Client:

| Input | Description | Example |
|---|---|---|
| Vendor | Third-party service name | `LexisNexis` |
| Resource | Business object | `User` |
| Endpoint | HTTP method + path | `POST /api/users` |
| Request Type | Data structure sent to the API | `LexisNexisPayload(ssn: String)` |
| Response Type | Data structure returned by the API | `LexisNexisResponse(name, email)` |
| Auth | Authentication mechanism | API Key in header, OAuth2 |
| Config | Externalized configuration keys | `base-url`, `timeout`, `api-key` |

## Output Specification

When generating the code, please adhere to the following structure:

1. **Class Definition**: Use the naming convention `{Vendor}{Resource}Client`.
2. **Constructor**: Inject necessary configuration and Auth providers.
3. **Transmission Method**: Implement the primary method to invoke the API, strictly using the provided Request/Response types.
4. **Error Handling**: Include a `try-catch` block that maps protocol-specific errors (e.g., HTTP 4xx/5xx) to a unified error handling strategy.
5. **No Logic**: Ensure the method body contains no business logic or data transformation code. Use comments to indicate where `shell-mapper` should be invoked.

## Generation

### Base Class Contract

`org.hermi.shell.Client<P, R>` provides:
- `exchange(P payload)` — public API, triggers the full lifecycle
- Audit lifecycle: `recordContext` → `doExchange` → `recordResult` / `recordError`
- Subclass only needs to implement `doExchange(P payload)`

The base class has two constructors:
- `Client(PersistentAuditor<P, R>)` — for production auditing
- `Client()` — uses `NoopPersistentAuditor` as default

### Implementation Steps

1. Extend `org.hermi.shell.Client<{RequestType}, {ResponseType}>`
2. Inject dependencies (HTTP client, configuration properties, etc.)
3. Implement `doExchange` — make the API call and return the response
4. Use `@Component` (or equivalent) to register as a Spring bean

### Code Skeleton

```java
@Component
public class {Vendor}{Resource}Client extends Client<{RequestType}, {ResponseType}> {

  public {Vendor}{Resource}Client({Dependencies} dependencies) {
    // Use super() for NoopPersistentAuditor, or super(auditor) for production auditing
  }

  @Override
  protected {ResponseType} doExchange({RequestType} payload) {
    // Protocol transmission only
  }
}
```

### Directory

```
src/main/java/{org}/{resource}/shell/client/
└── {Vendor}{Resource}Client.java
```

## Naming

| Component | Pattern | Example |
|---|---|---|
| Vendor Client | `{Vendor}{Resource}Client` | `LexisNexisUserClient` |

Prefix Isolation applies: the vendor/technology name MUST be the first word of the class name.

## Examples

### Local Client — In-Memory Testing

`org.hermi.shell.util.LocalClient` is a reusable `Client` implementation backed by `ConcurrentHashMap`. Use it for testing without network:

```java
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
| Auditor | `NoopPersistentAuditor` (default) |
| API | `put()`, `get()`, `remove()`, `containsKey()`, `clear()`, `size()` |

### LexisNexis — REST API Integration

```java
@Component
public class LexisNexisUserClient extends Client<LexisNexisPayload, LexisNexisResponse> {
  private final RestTemplate restTemplate;
  private final String baseUrl;

  public LexisNexisUserClient(RestTemplate restTemplate,
                              @Value("${lexisnexis.base-url}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
  }

  @Override
  protected LexisNexisResponse doExchange(LexisNexisPayload payload) {
    return restTemplate.postForObject(baseUrl + "/api/users", payload, LexisNexisResponse.class);
  }
}
```

The base class catches any exception thrown by `doExchange`, records it via the auditor, and rethrows it — vendor clients do not need try-catch in `doExchange`.

## Forbidden Patterns

- ❌ No business logic — vendor clients are protocol gateways only
- ❌ No type conversion — domain↔vendor mapping belongs in [shell-mapper](template/element/shell-mapper/ELEMENT.md)
- ❌ No hardcoded configuration — all configurable values must be injected via constructor
- ❌ No vendor technology imports in use-case modules — shell must be a separate module

## Related Elements

- [shell-mapper](template/element/shell-mapper/ELEMENT.md) — type conversion between domain and vendor types
- [use-case-client](template/element/use-case-client/ELEMENT.md) — composition of vendor client + mapper into use case contract
