# Simple Design (Kent Beck's 4 Rules)

> **Core Rule:** Design emerges and stays clean by adhering strictly to the four rules of simple design, evaluated in strict order of priority.

## Priority 1: Runs All Tests (Safety & Verifiability)
- **Rule:** Code that cannot be automatically verified is not clean. Never refactor without test coverage.
- **Constraint:** Tests provide the safety net that makes continuous refinement possible. Code without tests is dead legacy code.

## Priority 2: Contains No Duplication (True DRY vs. Accidental Duplication)
- **Rule:** Eliminate duplication of *knowledge* and *reasons to change*, not mere coincidental code shape.
- **Constraints:**
  - **Same Reason to Change:** Duplication only exists if two code blocks share the exact same reason to change (same business actor / domain rule).
  - **Accidental Duplication:** If two code blocks currently look identical but evolve for different reasons (different actors or workflows), they are **NOT** duplicates. Do NOT artificially couple them with shared helpers or base classes.
  - **Beware False DRY:** Premature abstraction over coincidental similarities creates harmful coupling. Duplication is far cheaper than the wrong abstraction.

## Priority 3: Expresses Intent of the Programmer (Clarity)
- **Rule:** Code must read like well-crafted prose. Express intent clearly so readers never have to guess.
- **Constraints:**
  - Reveal intent through descriptive, unambiguous names.
  - Keep functions focused and cohesive with clear boundaries.
  - Unit tests must act as living, executable specifications.

## Priority 4: Minimizes Elements (The Anti-Overengineering Brake)
- **Rule:** Keep the total number of classes, interfaces, and methods as small as possible while satisfying Rules 1–3.
- **Constraints:**
  - **The Golden Brake:** Resist dogmatic fragmentation. Do not create single-use abstractions, premature interfaces, or micro-classes just to check a design pattern box.
  - Code should be as simple as possible, but no simpler.
