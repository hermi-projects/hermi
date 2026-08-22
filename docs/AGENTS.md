# AGENTS.md — The documentation standard

This document defines the structure, Markdown tiers, and writing rules for human-facing documentation and AI agent context in this repository. All contributors (humans and AI agents) must strictly adhere to this standard when writing or refactoring documentation.

---

## I. Document Structure and Tier Principles

Each fact has **one home** (One home per fact): the tier whose job it is; elsewhere, link directly to its single source of truth using a relative path.

### Tier Taxonomy

| Tier | Job | Does NOT belong there |
| --- | --- | --- |
| **Root `AGENTS.md**` | **Standing Orders**: Core rules an AI agent and developer need in context in every session (1 to 3 lines each), linking to their true homes. | Background stories, worked examples, situational procedures. |
| **Subtree `AGENTS.md**` | Sub-package or module-specific standing orders. | Repo-wide rules already carried by the root file. |
| **`architecture.md`** | **System Architecture Map**: Domain model layering, core module relations, component interactions, and extension points. Read before changing business code. | Detailed DTO/interface definitions (→ subsystems), decision history (→ decision records). |
| **`subsystems/`** | **Subsystem Reference Manuals**: Single-page references for core modules/microservices (defining API contracts, core interfaces, domain events, and semantics). | Narrative control-flow step-by-step descriptions (→ architecture map). |
| **Decision Records (`notes/`)** | **Active Decision Records**: The "why", what was given up, and required verification conditions. | Outdated migration plans, lengthy code walkthrough histories. |
| **`cookbook/`** | **Practical Guides**: Step-by-step how-tos with clear, numbered verification steps (e.g., "How to add a new endpoint"). | Underlying architecture design rationale (→ decision records). |
| **Module `README.md**` | Package-level external contract: configuration item descriptions, core capabilities, boundaries, and limits. | Code comment duplication, other modules' concerns. |

---

## II. Core Writing Rules

### 1. Document Current State, Never Change History

* Strictly avoid words like `"previously"`, `"now"`, `"no longer"`, `"was renamed"`, PR numbers, or Git commit references in durable prose and code comments.
* **Describe only the present reality.** Change history belongs in Git commit logs and dedicated decision records.

### 2. Strict Separation of Tutorials and References

* **Tutorials**: Must guide readers to a specific outcome in a strict step-by-step order (easy to advanced, prerequisites before dependent concepts), introducing only what each step needs.
* **References**: Define lookup scope and current behavior without any teaching sequence or narration.

### 3. One Physical Line Per Paragraph

* Use editor soft-wrap for prose paragraphs. Code blocks, tables, and list structures retain their standard Markdown formatting.

### 4. Eliminate AI Slop and Reasoning Transcripts

* **No code restatement**: Do not comment on facts obvious from the code itself.
* **Delete reasoning paths**: Remove step-by-step narration like "first we considered... then we called...", keeping only the final contract, failure conditions, concurrency constraints, and business consequences.
* **No fuzzy metaphors**: Use precise, concrete names (e.g., specific Java class names, interface names, Bean names, database table names, or API paths like `UserController` or `user_id`), avoiding metaphors like "abstract gateway", "surface", or "data channel".

### 5. Code Snippets Must Compile and Run

* Embedded Java code snippets (e.g., enclosed in ````java`) must conform to syntax rules. Hand-written pseudo-code that cannot compile is strictly prohibited to prevent drift from actual code.

---

## III. The Slop Checklist

Audit and eradicate the following anti-patterns in any document:

1. **Information Duplication**: The same rule or configuration note appearing in multiple places. Grep a distinctive phrase, keep one home, and replace the rest with relative links.
2. **Narrative History**: Words like `"previously"`, `"now"`, `"used to"`, `"renamed"`, or `"was moved"`.
3. **Rotting Status Annotations**: Implementation status tags in prose or diagrams like `"implemented!"` or `"future: ..."`. The repository code is the sole source of truth for status.
4. **Paragraph Walls**: A single paragraph carrying multiple rules and parenthetical asides. Split them up or demote details to their proper home.
5. **Emphasis Inflation**: Widespread bolding, ALL CAPS, or words like `"critically"` everywhere. Reserve emphasis strictly for clauses that alter runtime behavior.

---

## IV. Cross-Reference Standard

All internal repository links **must use relative Markdown paths** (e.g., `[User Service Reference](../subsystems/user-service.md)`). Bare filenames or absolute paths are strictly prohibited to ensure link integrity.
