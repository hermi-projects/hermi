# Side Effects

> **Core Rule:** Command-Query Separation (CQS) - Functions must either change the state of an object (command) or return some information about the object (query), never both.

## 1. Command-Query Separation (CQS)
- **Rule:** Clearly separate state-mutating operations from data-retrieving operations.
- **Constraint:** A function that returns data must not alter the system state. If a method modifies state, its return type must be `void` (or indicate status without returning primary data).

## 2. Hidden Side Effects
- **Rule:** Functions must not conceal unexpected state modifications or asynchronous triggers.
- **Constraint:** Side effects must be explicitly named in the function identifier. If a function initializes a session during a routine check, the name must reflect it or the side effect must be isolated.

## 3. Temporal Coupling
- **Rule:** Avoid hidden temporal dependencies where method calls must happen in a strict, undocumented order.
- **Constraint:** If methods must be called in sequence, use patterns (like the Builder pattern) or return state objects to make the sequence explicit and safe.
