---
name: element
description: Defines the Element specification for the Hermi architecture. An Element is a reusable architectural building block with a contract, rules, and code generation guidance. Use when defining a new architectural element type.
metadata:
  priority: high
---

# Specification

> The complete format specification for Hermi Elements.

## Directory structure

An element is a directory containing, at minimum, a `ELEMENT.md` file:

```
element-name/
├── ELEMENT.md         # Required: metadata + specification
├── scripts/           # Optional: executable code
├── references/        # Optional: documentation
├── assets/            # Optional: templates, resources
└── ...                # Any additional files or directories
```

## `ELEMENT.md` format

The `ELEMENT.md` file must contain YAML frontmatter followed by Markdown content.

### Frontmatter

| Field         | Required | Constraints                                                                                                      |
|---------------|----------|------------------------------------------------------------------------------------------------------------------|
| `name`        | Yes      | Max 64 characters. Lowercase letters, numbers, and hyphens only. Must not start or end with a hyphen.            |
| `description` | Yes      | Max 1024 characters. Non-empty. Describes what the element does and when to use it.                              |
| `class`      | Yes      | Max 512 characters. Fully qualified Java class name that this element extends.                                    |
| `phase`       | No       | Architecture phase identifier (e.g. "1", "2", "both").                                                            |
| `metadata`    | No       | Arbitrary key-value mapping for additional metadata.                                                             |

<Card>
  **Minimal example:**

  ```markdown ELEMENT.md
  ---
  name: element-name
  description: A description of what this element does and when to use it.
  ---
  ```

  **Example with optional fields:**

  ```markdown ELEMENT.md
  ---
  name: use-case
  description: Describes a business operation and orchestrates domain logic through external contracts.
  class: "org.hermi.usecase.standard.UseCase"
  phase: "1"
  metadata:
    priority: high
  ---
  ```
</Card>

#### `name` field

The required `name` field:

* Must be 1-64 characters
* May only contain lowercase alphanumeric characters (`a-z`, `0-9`) and hyphens (`-`)
* Must not start or end with a hyphen (`-`)
* Must not contain consecutive hyphens (`--`)
* Must match the parent directory name

<Card>
  **Valid examples:**

  ```yaml
  name: use-case
  ```

  ```yaml
  name: shell-client
  ```

  **Invalid examples:**

  ```yaml
  name: UseCase     # uppercase not allowed
  ```

  ```yaml
  name: -client     # cannot start with hyphen
  ```

  ```yaml
  name: shell--client  # consecutive hyphens not allowed
  ```
</Card>

#### `description` field

The required `description` field:

* Must be 1-1024 characters
* Should describe both what the element does and when to use it
* Should include specific keywords that help agents identify relevant tasks

<Card>
  **Good example:**

  ```yaml
  description: Describes a business operation and orchestrates domain logic through external contracts (Client, Repository, Messenger). Use when implementing a business feature or domain operation. Keywords: use case, business logic, feature, domain service.
  ```

  **Poor example:**

  ```yaml
  description: The use case element.
  ```
</Card>

#### `class` field

The required `class` field:

* Must be 1-512 characters
* A fully qualified Java class name that this element's contract extends
* Provides the type bound for code generation
* Example: `"org.hermi.usecase.standard.UseCase"`

<Card>
  **Example:**

  ```yaml
  class: "org.hermi.usecase.standard.UseCase"
  ```
</Card>

#### `phase` field

The optional `phase` field:

* Identifies which architecture phase this element belongs to
* `"1"` = Domain / Use Case layer (pure business logic, zero infrastructure)
* `"2"` = Shell / Infrastructure layer (technology adapters, framework wiring)
* `"both"` = Applies across phases

<Card>
  **Examples:**

  ```yaml
  phase: "1"
  ```

  ```yaml
  phase: "2"
  ```

  ```yaml
  phase: "both"
  ```
</Card>

#### `metadata` field

The optional `metadata` field:

* A map from string keys to string values
* Stores additional properties not defined by the Element spec
* Recommended keys: `priority`

<Card>
  **Example:**

  ```yaml
  metadata:
    priority: high
  ```
</Card>

### Body content

The Markdown body after the frontmatter defines **what information is needed to implement this element**. An AI agent reads these sections to gather the inputs required for code generation.

There are no format restrictions, but the following sections collectively serve as the implementation specification:

| Section | Purpose | What information it provides |
|---|---|---|
| **Role & Design Intent** | Establishes context | Why this element exists, what problem it solves |
| **Directory Structure** | Defines output artifacts | File layout, naming conventions, package structure |
| **Generation Rules** | Constrains generation | Numbered rules the AI must follow when generating code |
| **Forbidden Patterns** | Prevents mistakes | Anti-patterns, infrastructure leaks, common errors |
| **Complete Example** | Serves as reference | Full code showing all rules applied together |
| **Verification (Phase 1 Shell)** | Defines test strategy | How to verify the implementation without infrastructure |
| **Phase 2 Integration** | Connects to infra | How this element gets production adapters |
| **Related Elements** | Cross-references | Other elements this one depends on or generates |

An AI agent will load this entire file once it decides to use an element. Keep `ELEMENT.md` focused. Move detailed reference material (large code examples, full contract definitions) into the optional directories.

## Optional directories

### `scripts/`

Contains executable code that agents can run as part of this element:

* `generate.sh` - Code generation script
* `validate.sh` - Element validation script

### `references/`

Contains additional documentation that agents can read when needed:

* `REFERENCE.md` - Detailed technical reference
* `CONTRACTS.md` - JIT contract definitions (Client, Repository, Messenger)
* `EXAMPLES.md` - Extended or edge-case code examples

Keep individual reference files focused. Agents load these on demand, so smaller files mean less use of context.

### `assets/`

Contains static resources:

* Templates (file templates, configuration templates)
* Diagrams or images
* Data files (lookup tables, schemas)

## Progressive loading

Agents load elements progressively, pulling in more detail only as a task calls for it:

1. **Metadata** (~100 tokens): The `name` and `description` fields are loaded at discovery time
2. **Specification** (< 5000 tokens recommended): The full `ELEMENT.md` body is loaded when the element is activated
3. **Resources** (as needed): Files in `scripts/`, `references/`, or `assets/` are loaded only when required

Keep your main `ELEMENT.md` under 500 lines. Move detailed reference material to separate files.

## File references

When referencing other files in your element, use relative paths from the element root:

```markdown
See [the contract guide](references/CONTRACTS.md) for JIT contract definitions.

Run the generation script:
scripts/generate.sh
```

Keep file references one level deep from `ELEMENT.md`. Avoid deeply nested reference chains.

## Element chain

Elements can reference and depend on other elements:

- A `use-case` may generate `use-case-client` contracts during JIT discovery
- A `shell-client` wraps a `use-case-client` for Phase 2 infrastructure integration

Cross-link related elements in the **Related Elements** section of each `ELEMENT.md`.

## Validation

Ensure every element conforms to this specification:

1. `name` field matches the parent directory name
2. `ELEMENT.md` exists and has valid YAML frontmatter
3. Required fields (`name`, `description`, `class`) are present and non-empty
4. All referenced files exist relative to the element root
