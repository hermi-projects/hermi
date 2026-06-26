# Testability Rule Template

Use this template to convert any testability anti-pattern description into a structured rule consistent with this skill's format.

---

## Prompt

Paste the following into any AI model, replacing the placeholder at the bottom:

---

```
Convert the following testability anti-pattern description into a structured skill rule.

Use EXACTLY this format (include only sections that have content — IGNORE IF is optional):

#### T-XX — [Anti-Pattern Name]
```
IF: [detection condition, be specific and measurable where possible]
AND: [additional condition, if applicable]
THEN: flag as [WARNING | CRITICAL | INFO]

[Add a second IF/THEN block if there are two severity tiers]

WHY: [Root cause — why this hurts testability and why it matters for design. 2-4 lines.]

SIGNAL: [One concrete, actionable heuristic a developer can apply in practice
         to confirm whether the anti-pattern is present. Start with an action verb.]

FIX (choose based on [context]):
  1. [Refactoring technique] — [when to apply it]
  2. [Refactoring technique] — [when to apply it]
  3. [Refactoring technique] — [when to apply it]

PAYOFF:
  - [Benefit of fixing for testability]
  - [Benefit of fixing for design/maintainability]

IGNORE IF: [when NOT to fix this anti-pattern — one line]
```

Rules:
- Keep everything inside a single fenced code block (triple backticks)
- Use imperative language in FIX steps
- SIGNAL must be concrete and specific — not a rephrasing of WHY
- SIGNAL must start with an action verb: "Try...", "Search...", "Count...", "Look at..."
- IGNORE IF is only included when there is a legitimate exception
- Preserve the exact keywords: IF, THEN, WHY, SIGNAL, FIX, PAYOFF, IGNORE IF
- The ID prefix is `T-` for general rules, `JT-` for Java-specific rules

---

Input anti-pattern description:
[PASTE THE TESTABILITY ANTI-PATTERN DESCRIPTION HERE]
```

---

## Section Reference

| Section | Required | Rule |
|---|---|---|
| `IF / THEN` | ✅ Yes | Must be measurable (detectable from code) OR behavioral (pattern description). Use `AND` for compound conditions. Add a second `IF/THEN` block for multi-tier severity. |
| `WHY` | ✅ Yes | Explain the root cause, not just the consequence. Connect testing difficulty to the underlying design flaw. 2–4 lines. |
| `SIGNAL` | ✅ Yes | One concrete heuristic to confirm the anti-pattern. Must start with an action verb: "Try writing a test without...", "Search for...", "Count the...", "Look at..." |
| `FIX` | ✅ Yes | Ordered list — most common/impactful fix first. Each item includes the technique name and when-to-apply context. Use dependency injection, extraction, or making state explicit — the three fundamental fixes for testability. |
| `PAYOFF` | ✅ Yes | Direct benefits only. At least one must be about testability specifically. No marketing language. |
| `IGNORE IF` | ⚡ Optional | Include only when there is a genuine exception where the pattern is acceptable. Be specific — "when the class is small" is not specific enough. |

---

## Anti-Pattern to Rule Mapping

When converting a testability anti-pattern to a rule, identify which of these root causes it belongs to:

| Root Cause | Key Question | Typical Fix |
|---|---|---|
| **Hidden Dependency** | Can you control what the code depends on? | Constructor injection |
| **Non-Determinism** | Does the same input always produce the same output? | Inject Clock/RandomProvider |
| **Shared Mutable State** | Can tests run in any order without interference? | Make immutable, inject config |
| **Coupled Concerns** | Can you test business logic without I/O? | Extract pure logic, inject I/O clients |
| **Invisible Failure** | Can you observe success and failure? | Return result type, let exceptions propagate |
| **Lifecycle Complexity** | Can you use the object without a sequence of setup calls? | Constructor establishes invariants |
| **Excessive Scope** | Does the class do one thing, with few collaborators? | Split by responsibility |

The SIGNAL should let a developer confirm the root cause in seconds.

---

## Severity Decision

| If the anti-pattern... | Use severity |
|---|---|
| Makes isolated testing **impossible** — every test path hits real infrastructure | **CRITICAL** |
| Makes testing **painful, flaky, or fragile** — tests are possible but with excessive setup or risk of interference | **WARNING** |
| Is a **minor concern or speculative** — doesn't currently block testing | **INFO** |

---

## Examples

### Good Rule (CRITICAL)

```
#### T-01 — Hard-Coded Dependency
```
IF: a method creates its own dependency via `new` inside the method body
AND: the created object is infrastructure (database, HTTP, filesystem, message queue, email)
THEN: flag as CRITICAL

WHY: Hard-coded dependencies make it impossible to substitute fakes or mocks.
     Every test path hits real infrastructure. The class is coupled to a specific
     implementation and cannot be tested in isolation.

SIGNAL: Try writing a unit test for the method without a real database, network,
        or filesystem. If you can't, a hard-coded dependency is the cause.

FIX (choose based on context):
  1. Constructor Injection — pass the dependency through the constructor. Use for
     required dependencies that the class always needs.
  2. Method Parameter Injection — pass the dependency as a method parameter. Use
     when the dependency varies per call or is only needed by one method.

PAYOFF:
  - Tests can pass mocks or fakes instead of real infrastructure.
  - The class's dependency graph is explicit in its constructor signature.

IGNORE IF: the created object is pure business logic with no side effects —
           `new DiscountCalculator()` is fine.
```

### Good Rule (WARNING with two-tiers)

```
#### JT-05 — Excessive Constructor Parameters
```
IF: a Java class constructor has 5 or more parameters that are injected dependencies
THEN: flag as WARNING

IF: a Java class constructor has 8 or more parameters
THEN: flag as CRITICAL

WHY: Too many constructor parameters signal that the class has too many
     responsibilities. Tests must mock many collaborators to test one behavior.

SIGNAL: Count non-primitive constructor parameters. If most tests mock more
        collaborators than they assert on, the class is too large.

FIX (choose based on context):
  1. Extract Sub-Service — group related dependencies into a new service with
     a single responsibility.
  2. Introduce Facade — wrap a frequently co-occurring group of dependencies
     in a higher-level abstraction.

PAYOFF:
  - Shorter, more focused constructors.
  - Each class has a single, nameable responsibility.
  - Test setup is short — mock only what matters.

IGNORE IF: the class is generated code or a pure configuration holder with no
           behavior to test.
```
