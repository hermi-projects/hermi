# Functions

> **Core Rule:** Functions should do one thing, do it well, and do it only. They should be small, highly cohesive, and restricted to a single level of abstraction.

## 1. Small Size
- **Rule:** Functions must be small, highly cohesive, and strictly respect their visibility scope to balance API clarity with internal encapsulation.
- **Constraints:**
  - **Public methods:** Must not exceed **15 lines** of code. Public APIs must remain thin, concise, and focused solely on high-level orchestration.
  - **Protected & Default methods:** Must not exceed **25 lines** of code.
  - **Private methods:** Must not exceed **35 lines** of code. Helper or internal low-level implementation logic is allowed slightly more room, but must still remain small.
  - **Nesting Control:** Keep block nesting minimal. Avoid deeply nested control structures (maximum 3 levels). Extract inner blocks into dedicated private methods or flatten them using guard clauses.

## 2. Single Responsibility (Do One Thing)
- **Rule:** A function must execute a single, cohesive task. 
- **Constraint:** If a function's steps can be extracted into another function whose name is just a restatement of its implementation, it is doing too much.

## 3. One Level of Abstraction per Function
- **Rule:** Statements within a function must share the exact same level of abstraction.
- **Constraint:** Do not mix high-level business concepts with low-level implementation details. Extract low-level details into separate helper methods.

## 4. Descriptive Names
- **Rule:** Use long, descriptive names for complex functions. 
- **Constraint:** Clarity beats brevity. A clear, descriptive name is always better than a short name plus an explanatory comment.

## 5. Function Arguments
- **Rule:** Minimize the number of arguments.
- **Limits:** 
  - **Ideal:** 0 arguments (niladic).
  - **Acceptable:** 1 (monadic) or 2 (dyadic).
  - **Avoid:** 3 (triadic).
  - **Forbidden:** 4+ (polyadic) — group parameters into a dedicated object.
- **Flag Arguments:** **Forbidden.** Passing a boolean flag indicates the function does more than one thing. Split it into separate functions.

## 6. Prefer Exceptions to Error Codes
- **Rule:** Throw exceptions instead of returning error codes.
- **Constraint:** Isolate `try`/`catch` blocks into dedicated functions to prevent deep nesting and keep error handling separate from normal business logic.
