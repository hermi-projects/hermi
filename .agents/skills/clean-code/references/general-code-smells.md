# General Code Smells

> **Core Rule:** Leave the campground cleaner than you found it. Continuous small improvements prevent systemic code decay.

## 1. The Boy Scout Rule
- **Rule:** Always leave the code a little cleaner than you found it.
- **Constraint:** Whenever the AI touches an existing file (even for a small bug fix), it should clean up adjacent minor issues (e.g., dead variables, messy formatting, unclear names).

## 2. DRY & KISS (Don't Repeat Yourself / Keep It Simple, Stupid)
- **Rule:** Eliminate duplication and unnecessary complexity.
- **Constraint:** Duplicated logic must be extracted into shared helper functions or base components. Avoid over-engineering or premature abstraction layers that add needless complexity.

## 3. Encapsulate Boundary Conditions
- **Rule:** Boundary conditions (such as array limits, timeouts, or state boundaries) are hard to track and prone to errors.
- **Constraint:** Do not scatter boundary checks across the codebase; centralize boundary processing into dedicated functions or value objects.
