# Formatting

> **Core Rule:** Code formatting is about communication, and communication is the professional developer's first line of defense. A well-formatted file tells a consistent, professional story.

## 1. Vertical Formatting & The Newspaper Metaphor
- **Rule:** A source file must be readable from top to bottom like a high-quality newspaper article: high-level intent first, granular details last.
- **Structural Blueprint:**
  1. **Headline (Class Declaration):** Name and type signature at the top reveal the domain concept immediately.
  2. **Synopsis (Public APIs / High-Level Entrypoints):** The primary public orchestrator methods appear first. They narrate the complete workflow at the highest abstraction level without leaking implementation details.
  3. **Body Paragraphs (Intermediate Sub-processes):** The private helper methods appear directly below the public method that calls them, ordered by invocation.
  4. **Granular Footnotes (Lowest-Level Plumbing):** Low-level technical details (e.g., regex patterns, cryptography, boundary math, string parsing) sink naturally toward the bottom of the file.
- **Constraints:**
  - **Early Drop-off:** A reader should grasp the entire business workflow just by reading the first public method (the synopsis) without needing to inspect low-level implementation details.
  - **No Inverted Narrative:** Never place low-level private helper methods above the high-level public methods that invoke them.
  - **Dependent Order & The Stepdown Rule:** Callers must be placed *immediately or closely above* their direct callees. Reading should be a continuous downward flow with zero back-and-forth jumping.
  - **Vertical Openness:** Separate distinct thoughts, concepts, and method declarations with single blank lines.
  - **File Size:** Source files should be small and focused—ideally under 200 lines, rarely exceeding 500 lines.

## 2. Horizontal Formatting
- **Rule:** Control the width of lines and horizontal spacing to avoid visual fatigue.
- **Constraints:**
  - **Line Length:** Restrict line length to a maximum of **100–120 characters**. Avoid horizontal scrolling.
  - **Horizontal Openness & Density:** Use spaces to associate tightly coupled things and disassociate weakly related ones (e.g., space around assignment operators: `int a = b + c;`, but no space between function names and their opening parentheses: `doSomething();`).
  - **Indentation:** Use consistent indentation (spaces or tabs per project standard) to clearly show scope boundaries. Never use unindented control structures.

## 3. Team Consistency
- **Rule:** A team must agree upon a single formatting style standard.
- **Constraint:** All team members must use the exact same formatting rules and automated formatter configurations. Style arguments are subjective; consistency across the codebase trumps personal preference.
