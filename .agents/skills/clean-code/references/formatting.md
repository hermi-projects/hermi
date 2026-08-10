# Formatting

> **Core Rule:** Code formatting is about communication, and communication is the professional developer's first line of defense. A well-formatted file tells a consistent, professional story.

## 1. Vertical Formatting
- **Rule:** Control the visual flow and vertical distance between concepts in a source file.
- **Constraints:**
  - **File Size (Newspaper Metaphor):** Source files should be small—ideally under 200 lines, rarely exceeding 500 lines. The name should be simple yet descriptive enough to tell what the file contains.
  - **Vertical Openness:** Separate concepts, distinct thoughts, and variable declarations from execution steps with blank lines. Never cluster unrelated lines together.
  - **Vertical Proximity:** Keep closely related code concepts vertically close to each other. Do not scatter dependent functions or variables across distant parts of the file. 
  - **Dependent Order:** Callers should ideally be defined *above* the functions they call to mirror a top-down reading flow.

## 2. Horizontal Formatting
- **Rule:** Control the width of lines and horizontal spacing to avoid visual fatigue.
- **Constraints:**
  - **Line Length:** Restrict line length to a maximum of **100–120 characters**. Avoid horizontal scrolling.
  - **Horizontal Openness & Density:** Use spaces to associate tightly coupled things and disassociate weakly related ones (e.g., space around assignment operators: `int a = b + c;`, but no space between function names and their opening parentheses: `doSomething();`).
  - **Indentation:** Use consistent indentation (spaces or tabs per project standard) to clearly show scope boundaries. Never use unindented control structures.

## 3. Team Consistency
- **Rule:** A team must agree upon a single formatting style standard.
- **Constraint:** All team members must use the exact same formatting rules and automated formatter configurations. Style arguments are subjective; consistency across the codebase trumps personal preference.
