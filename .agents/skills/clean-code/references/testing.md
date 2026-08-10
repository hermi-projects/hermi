# Unit Testing & F.I.R.S.T

> **Core Rule:** Clean code requires clean tests. Test code is just as important as production code and must be kept readable, expressive, and maintainable.

## 1. The F.I.R.S.T. Principles
- **Fast:** Tests must run quickly so they can be executed frequently.
- **Independent:** Tests must not depend on each other. Any test can be run in any order without side effects.
- **Repeatable:** Tests must be runnable in any environment without external dependencies or network assumptions.
- **Self-Validating:** Tests must have a boolean output (pass/fail). No manual log inspection required.
- **Timely:** Tests must be written *just before* or concurrently with production code (TDD approach).

## 2. Clean Test Standards
- **Rule:** Maintain readability, simplicity, and low maintenance overhead in test suites.
- **Constraints:**
  - **Single Concept per Test:** Each test method should test one specific behavior or condition.
  - **Domain-Specific Testing Language:** Use helper methods, custom assertions, or builder patterns to build a readable testing DSL rather than cluttered setup/teardown blocks.
