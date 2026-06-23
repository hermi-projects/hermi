# Java-Specific Code Smell Rules

This file extends the general code smell catalog with Java-specific patterns.
Read this file whenever the target language is Java, **before** producing a report or fix.

## Threshold Overrides for Java

These override the general defaults in `SKILL.md`:

| Smell | General Threshold | Java Threshold | Reason |
|---|---|---|---|
| Long Method (S01) | > 20 lines | > 25 lines | Java is verbose; getters/setters inflate line count |
| Large Class (S02) | > 300 lines | > 400 lines | Java requires boilerplate (constructors, annotations, imports) |
| Long Parameter List (S03) | > 3 params WARNING | > 4 params WARNING | Builder pattern common in Java; 3-param constructors are normal |

---

## Java-Specific Smells

### JS01 — Raw Types
```
IF: a generic type is used without its type parameter (e.g., List, Map, Set instead of List<T>)
THEN: flag as CRITICAL
WHY: Raw types bypass compile-time type safety — ClassCastException at runtime.
FIX: Add the correct type parameter: List → List<String>, Map → Map<String, Order>
```

### JS02 — Checked Exception Swallowing
```
IF: a catch block is empty
OR: a catch block contains only a comment or a logger.error() with no rethrow
THEN: flag as WARNING
WHY: Silent exception swallowing hides failures. Bugs become impossible to trace.
FIX: Either handle the exception meaningfully, rethrow it, or wrap in an unchecked exception.
```
**Example:**
```java
// BAD
try {
    process();
} catch (IOException e) {
    // TODO: handle this later
}

// GOOD
try {
    process();
} catch (IOException e) {
    throw new ProcessingException("Failed to process input", e);
}
```

### JS03 — Checked Exceptions for Control Flow
```
IF: a checked exception is used to signal a normal, expected condition
   (e.g., throwing FileNotFoundException to mean "optional file not present")
THEN: flag as WARNING
WHY: Checked exceptions for flow control make callers write ugly try/catch for non-errors.
FIX: Use Optional<T>, return a null-object, or a result type instead.
```

### JS04 — Public Fields
```
IF: a class field is declared public (non-final)
THEN: flag as CRITICAL
WHY: Public mutable fields break encapsulation. Any caller can corrupt object state.
FIX: Make the field private. Add a getter if read access is needed. Only add a setter if mutation is intentional.
```

### JS05 — instanceof Chains
```
IF: code uses instanceof to branch on type (if x instanceof A ... else if x instanceof B ...)
THEN: flag as CRITICAL
WHY: Same as S06 (Switch Statements) but specific to Java's instanceof pattern.
FIX: Use polymorphism, the Visitor pattern, or sealed classes + pattern matching (Java 17+).
```

### JS06 — Finalizer Usage
```
IF: a class overrides finalize()
THEN: flag as WARNING
WHY: Finalizers are unpredictable, slow, and deprecated in Java 9+. They cause GC pauses.
FIX: Use try-with-resources and implement AutoCloseable instead.
```

### JS07 — Static Mutable State
```
IF: a class has a static non-final mutable field
THEN: flag as CRITICAL
WHY: Static mutable state is shared across all instances and threads.
     It causes hidden coupling, race conditions, and test pollution.
FIX: Move state into instance fields, use thread-local variables, or inject dependencies.
```

### JS08 — Missing @Override
```
IF: a method appears to override a parent class/interface method
AND: it does not have the @Override annotation
THEN: flag as WARNING
WHY: Without @Override, a signature mismatch (typo, wrong params) silently creates a new method
     instead of overriding — a common source of subtle bugs.
FIX: Add @Override to the method declaration.
```

### JS09 — Catching Exception or Throwable
```
IF: a catch block catches Exception or Throwable (not a specific subtype)
THEN: flag as WARNING
WHY: Catching the base type hides unexpected failures and makes error diagnosis hard.
FIX: Catch only the specific checked exceptions you can handle. Let unchecked exceptions propagate.
```

### JS10 — Null Return from Collection Methods
```
IF: a method that is expected to return a collection (List, Set, Map) returns null
THEN: flag as CRITICAL
WHY: Callers must null-check before iterating — easy to forget, causing NullPointerException.
FIX: Return Collections.emptyList() / emptySet() / emptyMap() instead. Or use Optional if absence is meaningful.
```

### JS11 — Mutable Object in equals/hashCode
```
IF: a field used inside equals() or hashCode() is mutable (no final keyword)
THEN: flag as CRITICAL
WHY: If an object's field changes after it's been put into a HashMap/HashSet, it becomes
     "lost" — the hash changes and the object can no longer be found.
FIX: Make fields used in equals/hashCode final, or exclude mutable fields from these methods.
```

---

## Java Design Pattern Signals

These are not smells on their own, but flag them as INFO when they are used incorrectly:

| Pattern | Misuse Signal | Flag As |
|---|---|---|
| Singleton | Singleton holds mutable state accessed from multiple threads without synchronization | WARNING |
| Builder | Builder has no validation in the `build()` method — invalid objects can be constructed | INFO |
| Factory | Factory method uses `new` directly for every type instead of a registry/map | INFO |
| Repository | Repository method returns entities with lazy-loaded collections outside a transaction | WARNING |

---

## Java Output Format Extension

When reporting Java smells, include the annotation context. Example:

```
### CRITICAL
- [JS07] Static Mutable State — `private static List<Session> activeSessions` in SessionManager
  FILE: src/session/SessionManager.java:L14
  FIX: Move to instance field and inject SessionManager as a Spring @Bean (singleton scope)
```

When fixing Java code, always preserve:
- Existing Javadoc (do not delete or shorten)
- Existing annotations (@Override, @Autowired, @Transactional, etc.)
- Package declarations and import organization
