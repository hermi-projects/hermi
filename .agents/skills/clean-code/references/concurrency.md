# Concurrency

> **Core Rule:** Concurrency is a decoupling strategy, but it is also a notorious source of bugs. Clean concurrent code requires strict isolation and thread-safe design.

## 1. Concurrency Isolation
- **Rule:** Keep concurrency-related code separate from domain business logic.
- **Constraint:** Do not mix thread management, synchronization (`synchronized`, `locks`), or executor pools directly into regular business execution methods. Encapsulate them.

## 2. Limit Shared Data & Mutability
- **Rule:** Minimize the sharing of mutable state across threads.
- **Constraint:** Prefer immutable objects for shared states. If state must be shared, centralize synchronization and protect critical sections strictly.

## 3. Avoid Thread-Blocked Anti-Patterns
- **Rule:** Prevent hidden deadlocks and starvation.
- **Constraint:** Avoid circular dependencies between locks. Use thread-safe collections (`java.util.concurrent`) instead of writing custom raw locking mechanisms.
