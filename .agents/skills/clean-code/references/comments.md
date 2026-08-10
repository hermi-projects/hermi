# Comments

> **Core Rule:** Don't comment bad code — rewrite it. Comments are usually failures to express ourselves clearly in code.

## 1. Good Comments
- **Rule:** The only truly good comment is the one you figured out how not to write.
- **Constraint:** Use comments only when code cannot express intent alone. Acceptable uses include:
  - **Legal/Copyright notices:** Necessary regulatory or licensing headers.
  - **Explanation of intent:** Clarifying the *why* behind an unconventional or business-critical decision.
  - **Amplification:** Warning about the consequences of a specific action or highlighting the importance of something that might otherwise seem trivial.

## 2. Bad Comments
- **Rule:** Eliminate noise, redundancy, and outdated text.
- **Constraints:** 
  - **Redundant comments:** Do not state the obvious (e.g., `// returns the instance`).
  - **Mumbling & Misleading:** Do not write speculative, inaccurate, or outdated notes. If a comment drifts from the code it describes, it is worse than useless.
  - **Position markers:** Do not use banner lines or comment blocks to mark sections or separate methods (e.g., `// --- Business Logic ---`). Let well-named functions and classes organize the code.
  - **Commented-out code:** Delete dead code immediately. Never leave commented-out blocks lingering in production files (version control remembers).
  - **Nonlocal information:** Do not provide system-wide context or irrelevant details far away from the code segment being described.
