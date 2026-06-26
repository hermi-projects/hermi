# Java-Specific Testability Rules

This file extends the general testability rules in `SKILL.md` with Java-specific detection heuristics, severity adjustments, and additional rules.

When reviewing Java code, apply the rules in this file **in addition to** the general rules. Where this file specifies a different threshold or severity, it overrides the general rule.

---

## Severity Adjustments for Java

| General Rule | Java Adjustment | Reason |
|---|---|---|
| T-17 Excessive Collaborators | Flag at **5+** constructor parameters (general: 6+) | Java DI frameworks encourage constructor injection; 5 params is already a strong SRP signal |
| T-02 Static Side-Effect Call | Upgrade to **CRITICAL** if calling `System.exit()` or `Runtime.getRuntime().halt()` in business logic | These kill the JVM — untestable by definition |
| T-07 Global Mutable State | Flag `System.getenv()` and `System.getProperty()` as **CRITICAL** (already covered by T-07) | These are the Java-specific forms of global mutable state |

---

## Java-Specific Detection Patterns

### Infrastructure Classes (for T-01 Hard-Coded Dependency)

When `new` is called on any of these types inside business logic, flag as T-01 — CRITICAL:

**JDK Standard Library:**
- `java.net.URL`, `java.net.HttpURLConnection`
- `java.net.http.HttpClient`, `java.net.http.HttpRequest`
- `java.sql.Connection`, `java.sql.Statement`, `java.sql.ResultSet`
- `java.io.FileInputStream`, `java.io.FileOutputStream`, `java.io.FileReader`, `java.io.FileWriter`
- `java.nio.file.Files` (static methods, covered by T-02)
- `java.util.Timer`, `java.util.Random`, `java.security.SecureRandom`
- `javax.sql.DataSource` (should be injected, not created)

**Spring Framework:**
- `org.springframework.web.client.RestTemplate`
- `org.springframework.web.reactive.function.client.WebClient`
- `org.springframework.jdbc.core.JdbcTemplate`
- `org.springframework.mail.javamail.JavaMailSender`

**Jakarta EE / Javax:**
- `javax.mail.Session`, `javax.mail.Transport`
- `javax.jms.Connection`, `javax.jms.Session`, `javax.jms.MessageProducer`

**Any class ending in:** `*Client`, `*Repository`, `*Gateway`, `*Sender`, `*Publisher`, `*Connector`, `*Adapter`

### Static Side-Effect Methods (for T-02)

Flag calls to these static methods in business logic:

- `java.nio.file.Files.write()`, `.copy()`, `.move()`, `.delete()`, `.createFile()`, `.createDirectory()`
- `java.nio.file.Files.readAllBytes()`, `.readAllLines()`, `.readString()`, `.lines()`
- `System.getenv()`, `System.getProperty()` (also T-07)
- `System.currentTimeMillis()`, `System.nanoTime()` (also T-05)
- `Thread.sleep()` (also T-06)
- `javax.mail.Transport.send()` (static overload)

### Service Locator Patterns (for T-03)

Flag these patterns in business logic:

- `ApplicationContext.getBean()` / `BeanFactory.getBean()` (Spring)
- `CDI.current().select()` (Jakarta CDI)
- `jakarta.ws.rs.client.ClientBuilder.newClient()` without injection
- `ServiceLoader.load()` called inside a service method (acceptable at composition root)
- `InitialContext.doLookup()` (JNDI) called outside of @PostConstruct
- Any static method returning an interface type where the implementation is registered elsewhere

### Global Mutable State (for T-07)

Flag these Java-specific patterns:

- `public static` non-final field in any class
- `protected static` non-final field (subclass mutation)
- `static { }` initializer that assigns to non-final static fields from external sources
- `System.setProperty()` call in business logic
- Mutable `static` collection (`static List<String>`, `static Map<K,V>`)

### Framework-Entangled Logic (for T-11)

Flag business logic (conditions, calculations, validations) inside:

- `@RestController` / `@Controller` classes (Spring MVC)
- `HttpServlet.doGet()`, `.doPost()`, `.service()` methods
- `@Path` / `@GET` / `@POST` annotated methods (Jakarta RS)
- `@Scheduled` methods that contain business logic beyond scheduling
- `@EventListener` / `@TransactionalEventListener` methods with inline business rules
- `HandlerInterceptor.preHandle()` / `Filter.doFilter()` with business decisions

### Java-Specific Severity Rules for T-11

```
IF: a @RestController method body exceeds 10 lines (excluding blank lines and comments)
THEN: flag as WARNING — likely contains business logic that should be extracted

IF: a @RestController method body exceeds 20 lines
THEN: flag as CRITICAL — definitely contains business logic entangled with HTTP handling
```

### Constructor Side Effects (for T-14)

Flag constructors that do any of:

- Call `Files.read*()`, `Files.write*()`, or any file I/O
- Open network connections or sockets
- Call `Class.forName()` (classloading as side effect)
- Start threads (`new Thread().start()`, `executorService.submit()`)
- Call `System.load()` / `System.loadLibrary()`
- Acquire locks (`synchronized` on non-final fields, `Lock.lock()`)
- Call external APIs or databases
- Throw checked exceptions other than validation-related ones (IOException in a constructor = red flag)

---

## Java-Specific Rules

These rules apply only to Java and have no equivalent in the general catalog.

---

### JT-01 — @Autowired Field Injection

```
IF: a class uses @Autowired (or @Inject, @Resource) on a non-final field
AND: the field is not in a @Configuration class or framework-mandated test
THEN: flag as WARNING

WHY: Field injection hides dependencies. The class's required collaborators are
     invisible from the public API — you must read the class body to know what
     it depends on. Testing requires either a DI container or reflection to set
     fields. Constructor injection makes dependencies explicit, allows final
     fields, and makes the class testable with plain `new`.

SIGNAL: Search for @Autowired on fields. If you can't instantiate the class with
        `new MyClass(dep1, dep2)` because dependencies are injected via reflection,
        field injection is the problem.

FIX (choose based on context):
  1. Convert to Constructor Injection — move the @Autowired field to a constructor
     parameter. Make the field final. Delete the @Autowired on the field. If there
     is only one constructor, @Autowired on the constructor is optional (Spring 4.3+).
  2. Use Setter Injection for Optional Dependencies — if the dependency is genuinely
     optional, use a setter with @Autowired(required = false). This is rare —
     prefer constructor injection with an Optional or default implementation.

PAYOFF:
  - Dependencies are explicit in the constructor signature.
  - Fields can be final — the compiler enforces immutability.
  - Tests construct the class with `new` — no DI container, no reflection.

IGNORE IF: the field is in a @Configuration class where field injection is a pattern
           for receiving framework-managed beans, or in a test class where @MockBean
           injection is the intended testing pattern.
```

---

### JT-02 — Static Mutable State

```
IF: a class has a static non-final field of a mutable type
OR: a static collection (List, Map, Set, array) is exposed without defensive wrapping
THEN: flag as CRITICAL

WHY: Static mutable state in Java is shared across all threads, all classloaders
     (within the same loader), and all tests. It causes hidden coupling between
     unrelated tests, race conditions in parallel execution, and non-reproducible
     failures. A static `List<String>` that one test adds to and another test
     iterates is a classic source of order-dependent test failures.

SIGNAL: Search for `static` fields that are not declared `final`. Also search for
        static final collections (`static final List`) — these can still be mutated
        via add/put/remove. Run tests in random order; if failures appear, static
        mutable state is the likely cause.

FIX (choose based on context):
  1. Make Instance Field — move the state to an instance field. Manage lifecycle
     per instance. Tests create their own instances.
  2. Make Immutable — use Collections.unmodifiableList(), List.of(), or Guava
     ImmutableList for static final collections. Use final for simple fields.
  3. Inject the State — treat it as a dependency. Create a configuration class
     that holds the state and inject it where needed.

PAYOFF:
  - Tests can't interfere with each other through static state.
  - Parallel test execution is safe.
  - Thread safety is achieved through instance isolation, not synchronization.

IGNORE IF: the static field is a primitive or immutable type (String, Integer,
           enum, LocalDate) AND declared final AND initialized with a constant
           expression or at class-load time from a pure source.
```

---

### JT-03 — Business Logic in @Scheduled Method

```
IF: a method annotated with @Scheduled (Spring) or @Schedule (Jakarta) contains
    business logic, calculations, or decisions beyond scheduling concerns
AND: the business logic cannot be tested without triggering the scheduler
THEN: flag as WARNING

WHY: @Scheduled methods couple business logic to the scheduling infrastructure.
     Testing the logic requires either waiting for the schedule to fire (flaky),
     calling the method directly (bypasses scheduling, but tests the logic), or
     mocking the scheduler. The scheduling annotation conflates "when to run"
     with "what to run."

SIGNAL: Look at the @Scheduled method body. If it does more than call a single
        service method, it likely contains inline business logic. Extract it.

FIX (choose based on context):
  1. Extract Service Method — the @Scheduled method should be a one-liner that
     delegates to an injected service. The service is testable without scheduling.
  2. Separate Scheduling Policy from Business Logic — create a SchedulerPolicy
     class that handles timing, retry, and error handling. The business logic
     class has no scheduling concern.

PAYOFF:
  - Business logic is testable with direct method calls — no scheduler.
  - Scheduling policy (interval, retry, error handling) is testable separately.
  - The @Scheduled method reads as "call service.method()" — trivially correct.

IGNORE IF: the @Scheduled method is a one-line delegation to an injected service
           with no inline logic.
```

---

### JT-04 — Java Time Source

```
IF: business logic calls LocalDateTime.now(), ZonedDateTime.now(), Instant.now(),
    LocalDate.now(), LocalTime.now(), or System.currentTimeMillis() directly
AND: the time value affects the method's output or control flow
THEN: flag as CRITICAL

WHY: Direct time calls make behavior non-deterministic. Java provides java.time.Clock
     specifically to make time injectable. Tests use Clock.fixed(); production uses
     Clock.systemUTC(). The pattern is built into the standard library — not using it
     is a missed opportunity with zero cost.

SIGNAL: Search for `.now()` with no arguments on any java.time type. Also search
        for `System.currentTimeMillis()` in business logic. The fix is always the
        same: inject a Clock.

FIX (choose based on context):
  1. Inject java.time.Clock — pass Clock to the constructor. Replace
     LocalDateTime.now() with LocalDateTime.now(clock). Tests use
     Clock.fixed(instant, zone); production uses Clock.systemUTC().
  2. Pass Instant as Parameter — if only one method needs the time, pass the
     Instant directly as a parameter. The caller obtains it from the injected Clock.

PAYOFF:
  - Every time-dependent output is predictable and assertable.
  - Time-travel tests: Clock.fixed() at any instant in the past or future.
  - Zero performance cost — Clock is a value object.

IGNORE IF: the time call is in a log statement, metrics collection, or diagnostic
           output where the exact value is not asserted in tests.
```

---

### JT-05 — Excessive Constructor Parameters (Java Threshold)

```
IF: a Java class constructor has 5 or more parameters that are injected dependencies
    (excluding configuration values, primitive types, and standard library types)
THEN: flag as WARNING

IF: a Java class constructor has 8 or more parameters
THEN: flag as CRITICAL

WHY: Java's verbosity amplifies the cost of too many dependencies. Each new dependency
     requires a constructor parameter, a final field, and an assignment line — 3 lines
     of boilerplate per dependency before any logic. A 5-parameter constructor signals
     that the class is doing too much, and tests must mock 5 things to test 1 behavior.

SIGNAL: Count non-primitive, non-String constructor parameters. If the count is 5+,
        check whether most tests mock more collaborators than they assert on.

FIX (choose based on context):
  1. Extract Sub-Service — group related dependencies into a new service. If 3 of 5
     dependencies are about payment (paymentGateway, fraudDetector, receiptGenerator),
     extract a PaymentProcessor that depends on those 3. The original class now
     depends on 1 instead of 3.
  2. Use Lombok @RequiredArgsConstructor — reduces boilerplate but does not reduce
     the design problem. Only use this if the class genuinely needs all dependencies.
  3. Introduce Domain Events — replace direct calls to N notification services with
     a single EventPublisher. Each subscriber registers independently.

PAYOFF:
  - Shorter, more focused constructors.
  - Each class has a single, nameable responsibility.
  - Test setup is short — mock only what matters for the behavior under test.

IGNORE IF: the class is generated code (Lombok, MapStruct, Immutables), a pure
           data/configuration holder with no behavior, or a framework-mandated
           class where parameter count is controlled by the framework.
```
