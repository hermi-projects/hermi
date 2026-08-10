# Error Handling

> **Core Rule:** Error handling is important, but if it obscures logic, it is wrong. Use exceptions instead of return codes, and design clean boundaries.

## 1. Prefer Exceptions to Return Codes
- **Rule:** Throw exceptions instead of returning status error codes (e.g., `null` or `-1`).
- **Constraint:** Keep the normal business logic path clean of deep error-checking branches.

## 2. Write Your Try-Catch-Finally Statement First
- **Rule:** Define the scope of your transactions and error boundaries upfront.
- **Constraint:** Isolate `try`/`catch` blocks into dedicated functions. The `try` block should do one thing, and exception handling should not be mixed with execution logic.

## 3. Do Not Return Null
- **Rule:** Never return `null` from a method, as it forces callers to write defensive `null` checks.
- **Constraint:** Return empty collections, special case objects (Null Object Pattern), or throw an exception instead of returning `null`.

## 4. Do Not Pass Null
- **Rule:** Passing `null` as an argument is a code smell unless explicitly required by an API.
- **Constraint:** Guard against `null` inputs using assertions or explicit validations, and prohibit passing `null` in standard internal workflows.
