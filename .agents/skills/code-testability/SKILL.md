---
name: code-testability
description: Detects, reports, and fixes testability anti-patterns across all programming languages. Triggers on "testable", "hard to test", "can't test", "untestable", "testability", "write tests for", "mock", "unit test", or pasted code where the user asks why testing is difficult.
version: "1.0.0"
---

# Code Testability

Testing difficulty is never just a testing problem — it's a design signal. When a class is hard to test, the structure is the problem. This skill detects 18 testability anti-patterns organized by root cause, and provides concrete fixes that improve both testability and maintainability.

---

## Language-Specific Rules

This skill uses a two-layer approach:

1. **General rules (this file)** — Apply across all languages. Detects structural anti-patterns that hurt testability regardless of language: hard-coded dependencies, hidden non-determinism, global state, swallowed exceptions, constructor side effects, and others.

2. **Language-specific rules** — Live in `references/<lang>.md` and add language-specific detection heuristics, severity adjustments, and additional rules. See the reference file for your language.

Currently supported:
- `references/java.md` — Java-specific detection thresholds, infrastructure class patterns, and framework-specific anti-patterns

If no language reference exists for the target language, apply only the general rules in this file.

---

## Infrastructure Detection Checklist

Infrastructure is **any code that talks to something outside the current process** — the I/O boundary. When scanning a codebase for testability anti-patterns (T-01, T-02, T-10), run through this checklist. If any of these appear hard-coded (`new`) or called as a static method inside business logic, flag as CRITICAL.

This checklist is language-agnostic. The AI scanning the code should map each category to the concrete libraries, imports, and class names it finds in the project — don't rely on a pre-enumerated list of patterns.

---

1. **Databases & Persistence** — Connection pools, ORM sessions/contexts, query builders, migration tools, object/blob storage (S3, GCS, Azure Blob), search engines (Elasticsearch, Algolia, Meilisearch).

2. **Networking & HTTP** — Outbound HTTP clients (REST, GraphQL, gRPC), service discovery, WebSocket clients, RPC stubs/channels.

3. **Messaging & Event Streams** — Message producers/consumers (Kafka, RabbitMQ, SQS, Pub/Sub), event streams, job/task queues.

4. **File & Storage Systems** — Local filesystem access (read/write via OS paths), cloud storage, FTP/SFTP clients, static file I/O utilities.

5. **Email & Notifications** — SMTP clients, email APIs (SendGrid, Mailgun, SES), push notifications (FCM, APNs), SMS (Twilio), in-app real-time notifications (WebSocket, SSE).

6. **Security & Identity** — External auth providers (Auth0, Okta, Keycloak, Cognito), OAuth/OIDC token validation, LDAP/Active Directory, secrets vaults (Vault, Secrets Manager), certificate management.

7. **Observability** — Metrics collectors (StatsD, Prometheus push), tracing exporters (Jaeger, Zipkin, OpenTelemetry), error trackers (Sentry, Rollbar), remote logging backends.

8. **Scheduling & Background Processing** — Job schedulers (Quartz), workflow engines (Temporal, Cadence, Step Functions), background job enqueuers.

9. **Platform & Operations** — Feature flag services (LaunchDarkly, Unleash), remote configuration stores (Consul KV, Spring Cloud Config), circuit breakers/rate limiters created inline, container orchestration APIs (Kubernetes).

10. **External Business Services** — Payment gateways (Stripe, PayPal, Braintree), tax APIs (Avalara, TaxJar), shipping/logistics APIs, geolocation (Google Maps, Mapbox), fraud detection, KYC/identity verification.

---

### Quick-Check: Is It Infrastructure?

For any dependency, ask three questions. If the answer to **any** is yes, it's infrastructure — inject it, never hard-code with `new` or call as a static method.

| Question | If Yes → |
|---|---|
| 1. Does it talk to something outside the current **process**? (database, network, filesystem, external service) | Infrastructure → Inject (T-01, T-02) |
| 2. Does the same input ever produce **different output**? (time, randomness, external state) | Non-Determinism → Inject Clock/Provider (T-05) |
| 3. Would a **test need to mock it** to run in isolation? | Infrastructure → Inject |

---

## Mode Detection

This skill operates in three modes. Determine the mode from the user's request:

### Detect Mode

Triggered by: "review", "check", "audit", "scan", "find", "what testability issues", "why is this hard to test", "testability review".

**Behavior:** Read the target code and report every testability anti-pattern found. Do NOT edit any files. Report each finding with its rule ID, severity, file location, and the relevant code snippet. End with a summary count by severity.

### Fix Mode

Triggered by: "fix", "refactor", "make testable", "improve testability", "remove anti-pattern", "apply fix".

**Behavior:** Apply the fix described in the rule's FIX section. Show the before/after diff for each change. Only fix the anti-patterns the user explicitly asked about, or all detected issues if the user asked for a comprehensive fix.

### Default Mode

If no mode keyword is detected, default to Detect Mode. After reporting findings, offer: "Would you like me to fix any of these?"

---

## Testability Anti-Pattern Catalog

---

### Part 1: Dependency Problems

---

#### T-01 — Hard-Coded Dependency

```
IF: a method or constructor creates its own dependency via `new` inside the method body
AND: the created object is infrastructure (database connection, HTTP client, file I/O,
     message queue, cache, email sender, payment gateway, external API client)
THEN: flag as CRITICAL

WHY: Hard-coded dependencies make it impossible to substitute fakes or mocks in tests.
     Every test path hits real infrastructure — databases, networks, file systems.
     The class is coupled to a specific implementation and cannot be tested in isolation.
     Whether the infrastructure is internal or external, the design flaw is the same:
     the class cannot be tested without real side effects. This is the most common and
     most damaging testability anti-pattern.

SIGNAL: Try writing a unit test for the method without a real database, network, or
        filesystem. If you can't, a hard-coded dependency is the cause.

FIX (choose based on context):
  1. Constructor Injection — pass the dependency through the constructor and store it
     as a private final field. This is the preferred approach for required dependencies.
  2. Method Parameter Injection — pass the dependency as a method parameter. Use when
     the dependency varies per call or is only needed by one method.
  3. Wrap in Adapter — for third-party infrastructure, define an interface that matches
     your needs (not the vendor's) and implement it with the vendor SDK.
  4. Factory Method Override — extract `new` call to a protected factory method that
     tests can override. Use as a transitional step before full DI.

PAYOFF:
  - Tests can pass mocks or fakes instead of real infrastructure.
  - The class's dependency graph is explicit in the constructor signature.
  - Production code can swap implementations without changing the class.

IGNORE IF: the created object is pure business logic (calculation, validation, data
           transformation) with no side effects — `new DiscountCalculator()` is fine.
```

---

#### T-02 — Static Side-Effect Call

```
IF: a method calls a static method that performs I/O, network calls, file operations,
     or any side effect beyond pure computation
AND: the static method is not a pure utility (e.g., Math.max, Collections.sort)
THEN: flag as CRITICAL

WHY: Static methods performing I/O cannot be substituted in tests without heavyweight
     tooling (PowerMock, bytecode manipulation). Pure static helpers are harmless —
     the problem is specifically static calls that produce side effects or depend on
     external state. Tests are forced to either execute the real side effect or use
     invasive mocking frameworks. Internal or external, the impact on testability
     is the same — the static call is an unsubstitutable side effect.

SIGNAL: Search for static method calls in business logic. For each one, ask: "Does
        this method touch the filesystem, network, database, clock, or external system?"
        If yes, it's a testability problem.

FIX (choose based on context):
  1. Convert to Instance Method — if the static method is your own code, make it
     non-static and inject the owning class as a dependency.
  2. Wrap in an Adapter — if the static method is from a third-party library you
     cannot change, wrap it in an injectable adapter class.
  3. Pass Result as Parameter — if the static call is a pure lookup (e.g., reading
     an env var), call it at the composition root and pass the result in.

PAYOFF:
  - Tests can inject a mock or fake instead of calling the real static method.
  - No dependency on PowerMock or bytecode-manipulation frameworks.
  - The side effect is visible in the class's dependency list.

IGNORE IF: the static method is a pure function with no side effects and no external
           dependencies (e.g., Math.abs(), String.join(), Collections.emptyList()).
```

---

#### T-03 — Service Locator

```
IF: a method obtains a dependency by calling a global registry or service locator
    (e.g., ServiceLocator.get(), BeanFactory.getBean(), Context.getService(),
     Provider.get(), or any static/global lookup method that returns an instance)
THEN: flag as CRITICAL

WHY: Service locators are global mutable state in disguise. Any code can register
     or replace implementations at any time. Tests must set up the locator before
     each test and tear it down after — or risk cross-test contamination. Unlike
     constructor injection, the dependency is invisible from the class's public API.
     Service locator makes dependencies hidden, making the code harder to understand
     and tests order-dependent.

SIGNAL: Search for static methods that return objects (not primitives or strings).
        If you find `Something.getInstance()` or `Registry.lookup(...)` inside
        business logic, you've found a service locator.

FIX (choose based on context):
  1. Constructor Injection — pass the dependency through the constructor instead of
     looking it up. This makes the dependency explicit.
  2. Wrap in Provider — if the locator is framework-mandated (e.g., JNDI in application
     servers), wrap it in an injectable adapter that tests can replace.

PAYOFF:
  - Dependencies are explicit in the constructor signature.
  - Each test creates its own mock without touching global state.
  - No cross-test contamination. No setup/teardown of global registries.

IGNORE IF: the lookup happens exclusively at the composition root (application entry
           point, DI module) and never inside business logic or service classes.
```

---

#### T-04 — Singleton with Private Constructor

```
IF: a class enforces the Singleton pattern via a private constructor and static
    accessor (getInstance()), preventing instantiation by external code
AND: the class contains business logic that should be testable
THEN: flag as WARNING

WHY: The Singleton pattern enforces a single instance at the class-design level.
     Even when you want to substitute a test double, the private constructor and
     static accessor forbid it. The class is its own factory, its own registry,
     and its own lifecycle manager — all three responsibilities in one type.
     Singleton is a creation concern, not a class-design concern. It should be
     managed at the composition root, not enforced by the class itself.

SIGNAL: Look for `private constructor` + `static getInstance()` or `static final
        INSTANCE` field. If the class has both, it's a singleton anti-pattern.

FIX (choose based on context):
  1. Remove Singleton Enforcement — make the constructor public (or package-private),
     delete getInstance(), and manage the single instance at the composition root
     via your DI framework or manual wiring.
  2. Extract Interface + Keep Singleton — if the singleton is framework-mandated,
     extract an interface for the business logic and have the singleton implement it.
     Tests can create alternative implementations of the interface.

PAYOFF:
  - Tests can create fresh instances with test-appropriate configuration.
  - Production code still gets a single instance — but as a deployment decision,
    not a class-design constraint.
  - The class has one responsibility (business logic) instead of three.

IGNORE IF: the singleton class has no behavior worth testing (pure constant holder,
           trivial configuration reader without logic).
```

---

### Part 2: Non-Determinism

---

#### T-05 — Hidden Time and Randomness

```
IF: a method calls LocalDateTime.now(), System.currentTimeMillis(), new Random(),
    Math.random(), or any equivalent time/randomness source inline
AND: the result affects the method's output or control flow
THEN: flag as CRITICAL

WHY: Non-deterministic code produces different output on every call. Tests can only
     assert weak properties like "result is not null" instead of verifying exact
     behavior. Time-dependent logic is also a source of flaky tests — a test that
     passes today may fail tomorrow because of date boundaries. Random values make
     test failures unreproducible.

SIGNAL: Try asserting the exact output of the method. If you can't because the
        output depends on the current time or a random value, this anti-pattern
        is present.

FIX (choose based on context):
  1. Inject a Clock — use java.time.Clock (Java) or an equivalent time abstraction.
     Tests provide Clock.fixed(); production provides Clock.systemUTC().
  2. Inject a RandomProvider — wrap random generation in an injectable interface.
     Tests return a fixed value; production uses SecureRandom or equivalent.
  3. Pass as Parameter — if the value is only needed by one method, pass the
     Instant/Random as a method parameter instead of injecting it.

PAYOFF:
  - Every field of the output is predictable and assertable.
  - No flaky tests caused by date boundaries or random values.
  - Time-travel tests become trivial (fixed clock at any instant).

IGNORE IF: the time/random value is used only for logging, metrics, or diagnostics
           where correctness does not depend on the exact value.
```

---

#### T-06 — Non-Deterministic Concurrency

```
IF: a method creates threads, thread pools, or executors internally
OR: a method depends on thread scheduling behavior (Thread.sleep(), CountDownLatch
    timeouts, wait/notify timing, or thread-join with timeouts)
AND: the concurrency behavior affects correctness, not just performance
THEN: flag as WARNING

WHY: Code that depends on thread scheduling is inherently flaky. The JVM scheduler
     is not under test control. Tests that race against real threads produce sporadic
     failures that are impossible to reproduce consistently. Concurrency is an
     infrastructure concern — the business logic should not know about threads.

SIGNAL: Look for Executors.newFixedThreadPool(), new Thread(), Thread.sleep(),
        or awaitTermination() inside business logic. If the method creates its own
        threads or depends on timing, the design couples logic to scheduling.

FIX (choose based on context):
  1. Inject the ExecutorService — pass the executor as a constructor dependency.
     Tests use a same-thread executor; production uses a thread pool.
  2. Extract Async Boundary — move the threading decision to a separate orchestrator.
     Keep business logic synchronous and testable. The orchestrator handles
     parallelism as a deployment concern.
  3. Use CompletableFuture / async abstraction — if the language supports it,
     depend on an async abstraction rather than raw threads.

PAYOFF:
  - Tests run on a single thread with deterministic ordering.
  - Concurrency behavior is tested via integration tests, not unit tests.
  - Business logic is synchronous, simple, and debuggable.

IGNORE IF: the concurrency is the sole purpose of the class (e.g., a thread pool
           implementation, a scheduler, or a concurrency utility) and cannot be
           meaningfully tested without real threads.
```

---

### Part 3: State Problems

---

#### T-07 — Global Mutable State

```
IF: a class or module exposes mutable state through a public static non-final field
OR: a method mutates a global variable, static field, or module-level state
OR: business logic reads from System.getenv() or System.getProperty() directly
THEN: flag as CRITICAL

WHY: Global mutable state creates hidden coupling between unrelated parts of the
     system. Tests that mutate global state interfere with each other, producing
     order-dependent failures that are maddening to debug. Parallel test execution
     becomes impossible. Any code anywhere can change the value, so reasoning about
     program behavior requires global knowledge.

SIGNAL: Search for `public static` non-final fields, module-level mutable variables,
        or direct calls to System.getenv()/System.getProperty() in business logic.
        Run tests in random order — if they fail, global mutable state is likely.

FIX (choose based on context):
  1. Make Immutable — convert the static field to a final field initialized once
     at class load or via a static initializer.
  2. Inject Configuration — create an immutable AppConfig class and pass it via
     constructor injection. Each test creates its own config.
  3. Move to Instance Field — if the state is per-instance rather than global,
     move it to an instance field and manage lifecycle per instance.
  4. Wrap Environment Access — for System.getenv()/getProperty(), inject a
     configuration object rather than reading env vars inside business logic.

PAYOFF:
  - Each test creates its own configuration — no interference.
  - Parallel test execution is safe.
  - The config a class depends on is explicit in its constructor.

IGNORE IF: the static field is a pure constant (static final, immutable type) used
           as a named constant, not mutable state.
```

---

#### T-08 — Thread-Local / Ambient Context

```
IF: business logic accesses thread-local state (ThreadLocal, request context,
     security context, transaction context, session state) through a static method
OR: a method depends on invisible ambient context that must be set up before calling
AND: the ambient context affects the method's behavior or output
THEN: flag as WARNING

WHY: Ambient context is an invisible dependency. The method signature gives no hint
     that it depends on thread-local state. Tests must set up invisible context before
     each invocation and tear it down after. Parallel test execution causes cross-talk
     because thread-local state leaks between tests running in a thread pool. This is
     global mutable state scoped per thread — it has the same problems plus threading
     complexity.

SIGNAL: Search for ThreadLocal.get(), SecurityContext.getCurrentUser(),
        RequestContext.getCurrent(), or equivalent static accessors in business logic.
        If you must call a setup method (setting thread-locals) before you can test
        the target method, ambient context is the problem.

FIX (choose based on context):
  1. Inject a Context Provider — create an interface that provides the context,
     implement it with the thread-local accessor, and inject it. Tests provide a
     fake that returns fixed values.
  2. Pass Context as Parameter — if the context is the user/request/tenant, pass it
     explicitly as a method parameter. Functions become pure: f(context, input) → output.
  3. Extract Pure Logic — separate the pure business logic (no context dependency)
     from the context-dependent orchestration. Test the pure logic directly.

PAYOFF:
  - Dependencies are explicit in the constructor or method signature.
  - Tests don't need to set up invisible thread-local state.
  - Pure business logic is testable without any context at all.

IGNORE IF: the thread-local access is in framework-level infrastructure code (filters,
           interceptors, middleware) that is inherently coupled to the framework's
           request lifecycle. Business logic should never access it directly.
```

---

#### T-09 — Mutable Injected Dependency

```
IF: a class accepts a mutable dependency via injection (e.g., List, Map, mutable
    configuration object) and stores the reference without defensive copying
AND: the dependency's state affects the class's behavior
THEN: flag as WARNING

WHY: A dependency that is injected but mutable can still cause test pollution.
     If Test A modifies a shared list and Test B iterates over it, you get
     order-dependent failures — even though the class is correctly wired with DI.
     The class doesn't own its state; external code can mutate it after injection.
     This undermines the isolation that DI is supposed to provide.

SIGNAL: Check constructor bodies for assignments like `this.items = items` where
        `items` is a List, Map, Set, or mutable object. If the class does not
        defensively copy, external mutation will affect internal state.

FIX (choose based on context):
  1. Defensive Copy on Injection — copy mutable collections and objects in the
     constructor: `this.items = List.copyOf(items)` or `new ArrayList<>(items)`.
  2. Accept Immutable Types — change the constructor parameter to an immutable
     type (e.g., List.of(), ImmutableMap, or an unmodifiable wrapper).
  3. Use Builder/Factory — if the dependency is complex and mutable by design,
     use a builder that produces an immutable configuration object.

PAYOFF:
  - The class owns its state — no external code can mutate it after construction.
  - Tests can't interfere with each other through shared mutable references.
  - The class's invariants hold for its entire lifetime.

IGNORE IF: the dependency is a pure value object that is already immutable (String,
           Integer, BigDecimal, record, or a class documented as immutable).
```

---

### Part 4: Separation of Concerns

---

#### T-10 — Mixed Business Logic and I/O

```
IF: a single method or function performs both I/O (network, file, database, message
    queue) and business logic (calculation, decision, validation, transformation)
AND: the business logic cannot be exercised without executing the I/O
THEN: flag as CRITICAL

WHY: When business logic and I/O are intertwined, you cannot test any logic branch
     without real infrastructure. A discount calculation that depends on an HTTP
     call cannot be tested without a running server. Error handling for I/O failures
     cannot be tested without actually causing failures. Internal or external I/O,
     the separation of concerns violation is the same — business logic must be
     testable without any I/O at all.

SIGNAL: Try to test a specific business rule branch (e.g., "VIP gets 20% off").
        If you need a mock database, mock HTTP, or mock filesystem to reach that
        branch, business logic and I/O are mixed.

FIX (choose based on context):
  1. Extract I/O Client — move the I/O into a separate class (UserClient,
     InventoryClient). Inject it. Keep business logic pure and testable.
  2. Separate Query from Logic — call the I/O first, pass the result to a pure
     function for the business logic. The pure function is testable directly.
  3. Extract Pure Function — if the business logic is a calculation, extract it
     as a package-private static method. Test it with any input data.

PAYOFF:
  - Every business logic branch is testable with plain objects — no mocks.
  - I/O clients can be integration-tested separately.
  - The pure logic is reusable and composable.

IGNORE IF: the entire purpose of the method is I/O (e.g., a repository implementation,
           an HTTP client, a file writer) with no business logic to extract.
```

---

#### T-11 — Framework-Entangled Business Logic

```
IF: business logic (calculation, decision, validation) resides in a framework-mandated
    class (Controller, Handler, Servlet, Filter, Interceptor, Listener, Presenter)
AND: testing the logic requires setting up framework infrastructure (request objects,
     response objects, HTTP contexts, framework lifecycle)
THEN: flag as CRITICAL

WHY: Framework classes are designed for the framework, not for testing. When business
     logic lives in a @RestController, tests must construct MockMvc, build request
     parameters, inspect ResponseEntity, and assert on HTTP status codes — most
     assertions are about framework behavior, not business correctness. The framework
     lifecycle (filter chains, interceptors, serialization) adds noise to every test.

SIGNAL: Look at the test for a controller/handler. If more than 30% of the test setup
        is framework plumbing (mock requests, HTTP contexts, response wrappers) rather
        than business data, the logic is framework-entangled.

FIX (choose based on context):
  1. Extract Service Class — move the business logic to a plain class with no framework
     dependencies. The controller becomes a thin adapter that parses requests, delegates
     to the service, and returns responses.
  2. Extract Pure Function — if the logic is a transformation (request → domain object,
     domain object → response), extract it as a pure function testable with plain objects.
  3. Use Framework-Agnostic Domain — design the core domain to have zero framework imports.
     Framework classes depend on the domain, never the reverse.

PAYOFF:
  - Business logic is tested with plain method calls — no framework setup.
  - The controller becomes thin enough that it doesn't need unit tests.
  - The domain logic is portable across frameworks.

IGNORE IF: the controller/handler contains ONLY request parsing and response formatting
           with no business decisions, calculations, or validations.
```

---

#### T-12 — Business Logic Hidden in Large Workflow

```
IF: a single method orchestrates 3+ infrastructure concerns (inventory, payment,
     email, audit, shipping) and contains an inline business rule
AND: testing that business rule requires setting up mocks for all infrastructure
     concerns, even though only one rule is being tested
THEN: flag as WARNING

WHY: When a business rule is buried inside a large orchestration method, testing it
     requires mocking the entire world. A 5-line discount rule inside a 100-line
     checkout method forces every discount test to mock inventory, payment, email,
     and shipping — even though the rule only depends on Customer and subtotal.
     This is a Single Responsibility violation: the method both orchestrates and
     decides.

SIGNAL: Look for conditional logic (if/switch) inside orchestration methods. If the
        condition depends on business concepts (customer type, order amount, product
        category) rather than infrastructure results, it's a buried business rule.

FIX (choose based on context):
  1. Extract Policy Class — move the business rule to a separate class (DiscountPolicy,
     ShippingRule, FraudCheck). The orchestrator calls it; tests test it directly.
  2. Extract Pure Function — if the rule is stateless, extract it as a static method
     or pure function. Test with any inputs directly.
  3. Chain of Responsibility — if there are multiple rules that compose, extract each
     as a separate handler and compose them via a chain.

PAYOFF:
  - Each business rule is tested in isolation with 1-2 lines of setup.
  - The orchestrator reads as a sequence of named steps, not inline logic.
  - New business rules are added as new classes, not more lines in an already-long method.

IGNORE IF: the business logic is a single trivial condition that genuinely isn't worth
           extracting (e.g., `if (cart.isEmpty()) return;` as an early exit).
```

---

### Part 5: Observability

---

#### T-13 — Swallowed Exception

```
IF: a catch block catches an exception (or a broad type like Exception, Throwable,
     RuntimeException) and either: (a) does nothing, (b) only logs without rethrowing
     or returning failure, or (c) returns a value that hides the failure
AND: the caller cannot distinguish success from failure through the normal return path
THEN: flag as CRITICAL

WHY: Swallowed exceptions make failure invisible. The method claims to have performed
     an action but may have silently failed. Tests have nothing to assert — there is
     no exception to catch, no error code to check, no result object to inspect. The
     method's contract is dishonest. Callers proceed with invalid state because they
     were never told about the failure.

SIGNAL: Search for `catch (Exception e) { }` or `catch (Exception e) { log.warn(...); }`
        where execution continues normally after the catch. If the method returns void
        and catches all exceptions, the caller can never know if it worked.

FIX (choose based on context):
  1. Let It Propagate — remove the catch block. Declare the exception in the method
     signature. Let callers decide how to handle failure.
  2. Return a Result Type — wrap the return value in a Success/Failure discriminated
     union (Result, Either, Optional, or a custom result class with isSuccess()).
  3. Throw a Domain Exception — catch infrastructure exceptions and wrap them in
     meaningful domain exceptions (PaymentFailedException, EmailDeliveryException).
  4. Catch Specific Exceptions — if you must catch, catch only the specific exception
     types you can actually handle. Never catch Exception or Throwable.

PAYOFF:
  - Tests can assert on success and failure explicitly.
  - Callers are forced to handle failure — no silent data corruption.
  - The method's contract is honest about what can go wrong.

IGNORE IF: the catch is in a top-level event loop, job scheduler, or message consumer
           where the appropriate response to an exception IS to log and continue to
           the next item. Even then, prefer a dead-letter queue or retry mechanism
           over silent swallowing.
```

---

### Part 6: Object Lifecycle

---

#### T-14 — Constructor Side Effect

```
IF: a constructor performs I/O (file reading, network calls, database queries),
     starts threads, acquires resources, or computes extensively
OR: a constructor does anything beyond assigning fields and validating inputs
THEN: flag as WARNING

WHY: Constructors with side effects make object construction itself a testability
     barrier. You cannot create an instance in a test without triggering external
     dependencies, starting threads, or performing I/O. The constructor becomes
     a hidden method call that must be worked around. Every test that needs the
     class must also satisfy the constructor's dependencies — even if the test
     doesn't use the result of the constructor's work.

SIGNAL: Try `new MyClass()` in a test. If you need to set up files, databases,
        or network connections just to construct the object, the constructor
        is doing real work.

FIX (choose based on context):
  1. Defer Work to Method — move I/O and computation out of the constructor.
     The constructor assigns fields only. Callers invoke a separate method
     (e.g., initialize(), load(), start()) when they need the work done.
  2. Use Static Factory Method — the constructor takes pre-loaded data.
     A static factory method performs the I/O and returns a constructed instance.
     Tests call the constructor directly; production uses the factory.
  3. Inject Pre-Loaded Data — have the caller perform the I/O and pass the result
     to the constructor. The constructor receives data, not file paths or URLs.

PAYOFF:
  - Tests can construct the object with hand-built data — no I/O.
  - The constructor's contract is simple: assign fields, establish invariants.
  - Object creation and resource acquisition are separate concerns.

IGNORE IF: the constructor work is trivial validation (null checks, defensive copies)
           that doesn't touch external resources and completes in O(1) time.
```

---

#### T-15 — Temporal Coupling

```
IF: methods of a class must be called in a specific sequence for correct behavior
    (e.g., init() → process() → cleanup(), open() → read() → close())
AND: calling methods in the wrong order produces null pointer exceptions, illegal
     state errors, or silently incorrect results
THEN: flag as WARNING

WHY: Temporal coupling forces tests to replicate the exact lifecycle. Missing a call
     to init() produces a cryptic NullPointerException, not a clear error. Tests
     become longer because they must walk through the entire lifecycle to test one
     method in the middle. The class has invalid states that are representable in
     the type system — a design flaw that the compiler cannot catch.

SIGNAL: Look for methods named init/setup/open/start/begin and cleanup/close/stop/end.
        If you must call one before calling another meaningful method, temporal
        coupling is present.

FIX (choose based on context):
  1. Constructor Establishes Invariants — move initialization to the constructor.
     The object is valid from the moment it's created. No init() method.
  2. Make Invalid States Unrepresentable — if read() requires open(), ensure the
     only way to get a readable object is through a factory that returns an
     already-open instance. You literally cannot call read() on an unopened object.
  3. Return New State Object — instead of mutating state (open/close), have each
     lifecycle transition return a new object. OpenedConnection wraps Connection
     and only exposes read(); ClosedConnection exposes nothing.
  4. Use AutoCloseable / try-with-resources — if the language supports it, use
     deterministic resource management so the lifecycle is enforced by the compiler.

PAYOFF:
  - Tests never need to call init() before process() — the object is always ready.
  - The compiler enforces correct usage — invalid states are unrepresentable.
  - Shorter, simpler tests that focus on behavior, not lifecycle.

IGNORE IF: the class is an intentional state machine or workflow where the sequencing
           IS the business logic being tested (e.g., a checkout flow, a wizard).
```

---

### Part 7: Test Quality

---

#### T-16 — Implementation Testing (Over-Mocking)

```
IF: a test verifies internal method call order, internal state transitions, or exact
    sequences of collaborator interactions rather than observable behavior
AND: the test would fail after a refactoring that preserves correct behavior
THEN: flag as WARNING

WHY: Tests that verify implementation details resist refactoring. Change the internal
     algorithm, and the test breaks — even though the behavior is still correct.
     Over-mocked tests give false confidence: they pass when the implementation
     matches expectations, not when the system behaves correctly. They also make the
     codebase rigid — developers avoid refactoring because fixing tests is painful.

SIGNAL: Look at test assertions. If you see `verify(collaborator).someInternalMethod()`,
        `verify(collaborator, times(1)).doThing()`, or `inOrder.verify(...)`, ask:
        "Would a different correct implementation pass this test?" If no, the test
        is coupled to implementation.

FIX (choose based on context):
  1. Assert on Observable Outcome — replace interaction verification with state
     verification. Instead of `verify(repo).save(order)`, assert
     `assertEquals(OrderStatus.PROCESSED, repo.findById(id).getStatus())`.
  2. Use Fakes, Not Mocks — replace mocks with fakes (in-memory implementations)
     that accumulate state. Assert on the fake's accumulated state.
  3. Test at the Right Level — if the interaction IS the contract (e.g., an event
     publisher must call send()), verify only that contract, not internal helpers.
  4. Delete Brittle Tests — if a test verifies only implementation details and
     never catches real bugs, it has negative value. Delete it.

PAYOFF:
  - Refactoring internals doesn't break tests.
  - Tests document what the system does, not how it does it.
  - Higher signal-to-noise ratio in the test suite.

IGNORE IF: the interaction IS the observable behavior (e.g., testing that an email
           was sent, that a message was published to a queue, that an audit log was
           written). In these cases, verifying the interaction is correct.
```

---

#### T-17 — Excessive Collaborators

```
IF: a class has more than 5 constructor parameters (or more than 4 injected dependencies)
AND: the class is not a pure orchestrator/facade with a clear single purpose
THEN: flag as WARNING

WHY: A class with too many collaborators is doing too many things. Even if each
     dependency is properly injected, tests become a ceremony of mocking 6+
     irrelevant things to assert one line of logic. The constructor signature is a
     design feedback loop — if it's painful to write the test setup, the class has
     too many responsibilities. This is a Single Responsibility violation visible
     from the constructor.

SIGNAL: Count the constructor parameters. If there are 6 or more, check whether
        most tests need only 1-2 of them. If a test mocks 5 collaborators but only
        asserts on the behavior of 1, the class is too large.

FIX (choose based on context):
  1. Split by Responsibility — identify groups of collaborators that serve a single
     sub-responsibility. Extract each group into a new class. The original class
     becomes a thin orchestrator.
  2. Introduce Facade — if a group of collaborators is always used together (e.g.,
     payment + fraud + receipt), wrap them in a higher-level service. Depend on 1
     thing instead of 3.
  3. Use Domain Events — instead of calling N services directly, publish an event.
     Each service subscribes independently. The publisher depends on 1 event bus,
     not N services.

PAYOFF:
  - Each class has 1-3 focused dependencies that matter for every test.
  - Test setup is short and meaningful — no irrelevant mocks.
  - The class name can accurately describe what it does (not "Manager" or "Service").

IGNORE IF: the class is a pure orchestrator/facade where every collaborator is
           genuinely essential to its single responsibility, and there is no coherent
           sub-group to extract. This is rare — audit carefully.
```

---

#### T-18 — Leaky Abstraction

```
IF: an injected dependency requires the caller to know implementation details to use
    it correctly (e.g., must call flush() after write(), must set properties in a
    specific order, must catch implementation-specific exception types)
AND: the interface or contract does not guarantee the behavior the caller depends on
THEN: flag as WARNING

WHY: A dependency that leaks implementation details forces tests to encode knowledge
     they shouldn't have. The abstraction says `write(path, data)` but the callers
     know they must call `flush()` afterward or data is lost. Mocks can't capture
     this — only the real implementation exhibits the leak, so tests are forced to
     use real (heavy) implementations or write mocks that simulate the leak. Either
     way, the abstraction has failed at its one job: hiding complexity.

SIGNAL: Look for comments like "must call X before Y", "remember to close", "this
        throws ImplementationSpecificException". If the caller's code has workarounds
        or rituals for a specific dependency implementation, the abstraction leaks.

FIX (choose based on context):
  1. Fix the Abstraction — make the contract complete. If flush() is required after
     every write, make write() call flush() internally. The caller should never need
     to know about flushing.
  2. Fulfill the Contract — if the interface promises durability, the implementation
     must deliver it without extra caller steps. Move the missing behavior into the
     implementation.
  3. Rename to Expose Reality — if you can't fix the leak, rename the interface to
     make the contract honest. `FlushableFileStore` tells callers what they need to
     know.
  4. Use a Fake in Tests — if the real implementation has unavoidable complexity
     (e.g., async I/O, eventual consistency), use a fake that models the contract
     correctly without the real infrastructure.

PAYOFF:
  - Callers depend on the contract, not the implementation.
  - Tests use simple fakes that honor the same contract.
  - Implementation details (flushing, connection pooling, retry logic) stay inside
    the implementation where they belong.

IGNORE IF: the "leak" is a documented and intentional part of the contract (e.g.,
           an interface explicitly named Flushable, Closeable, or similar lifecycle
           marker where the sequencing IS the abstraction).
```

---

## Severity Guide

Three severity levels classify each finding. The criteria describe the decision rule; the rule IDs show the **default** assignment. Individual instances may be adjusted up or down based on local context (see INFO criteria below).

| Severity | Criteria | Assigned Rules |
|---|---|---|
| **CRITICAL** | Makes isolated testing impossible. Every test path hits real infrastructure — regardless of whether that infrastructure is internal or external. The rule is a **structural design flaw**: code must change before any test can exist. | T-01 (Hard-Coded Dependency), T-02 (Static Side-Effect Call), T-03 (Service Locator), T-05 (Hidden Time and Randomness), T-07 (Global Mutable State), T-10 (Mixed Business Logic and I/O), T-11 (Framework-Entangled Business Logic), T-13 (Swallowed Exception) |
| **WARNING** | Makes testing painful or flaky, or enables tests to interfere with each other. Code is technically testable but with excessive setup cost, flaky results, cross-test pollution, or brittleness under refactoring. | T-04 (Singleton with Private Constructor), T-06 (Non-Deterministic Concurrency), T-08 (Thread-Local / Ambient Context), T-09 (Mutable Injected Dependency), T-12 (Business Logic Hidden in Large Workflow), T-14 (Constructor Side Effect), T-15 (Temporal Coupling), T-16 (Implementation Testing / Over-Mocking), T-17 (Excessive Collaborators), T-18 (Leaky Abstraction) |
| **INFO** | Minor or speculative. Does not currently block or meaningfully burden testing, but may indicate a future problem. | No fixed default. Any WARNING-level rule where the impact is negligible in context — e.g., a constructor side effect that is only a log line, a trivial temporal coupling in a two-method class. Apply as a **downward adjustment** from WARNING per instance. |

---

## Output Formats

### Detect Mode Output

For each finding, output:

```
[SEVERITY] T-XX: Rule Name — file:line
  Found: <snippet of the offending code>
  Why: <one-line explanation specific to this instance>
```

End with a summary:

```
Testability Review Summary
  Files reviewed: N
  Findings: X CRITICAL, Y WARNING, Z INFO
  Top issue: <most common anti-pattern found>
```

### Fix Mode Output

For each fix applied, output:

```
🔧 T-XX: Rule Name — file:line
  Before: <original code snippet>
  After:  <fixed code snippet>
  Technique: <which FIX option was applied>
```

End with a summary:

```
Fixes Applied
  Files changed: N
  Anti-patterns fixed: X
```

---

## Self-Check

Before reporting findings, verify:

1. **Rule ID matches** — The reported rule ID corresponds to the pattern actually found. Don't report T-01 (Hard-Coded Dependency) for a `new DiscountCalculator()` call that creates pure business logic.

2. **Severity is calibrated** — CRITICAL (T-01, T-02, T-03, T-05, T-07, T-10, T-11, T-13) means tests CANNOT be written without real infrastructure — structural design flaw, code must change. WARNING (T-04, T-06, T-08, T-09, T-12, T-14, T-15, T-16, T-17, T-18) means tests are painful or flaky but possible. INFO means speculative — a WARNING instance where the impact is negligible in context.

3. **IGNORE IF checked** — Each rule has an IGNORE IF clause. Verify the finding doesn't match the ignore condition before reporting.

4. **Snippet is concrete** — The reported code snippet is the actual offending line, not a paraphrase.

5. **Fix is applicable** — In Fix Mode, the selected fix option matches the context. Don't suggest Constructor Injection when the dependency is a pure value object.
