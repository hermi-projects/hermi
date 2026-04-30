---
name: hermi-doc
description: MANDATORY skill for all Java documentation (Javadoc), architectural hardening, and AI-executable contracts within the Hermi framework. Use this skill whenever the user mentions 'Javadoc', 'API documentation', 'Java comments', 'base classes', 'interfaces', 'architectural constraints', or 'hardening' code for AI implementation. Be proactive in enforcing the Double-Block structure (AI Contract + Standard Javadoc) for every Java component to ensure architectural integrity.
---

# Java AI-Native Contract Architect

This skill automates the creation and enforcement of "AI Architectural Contracts" within Java codebases using a **Formatter-Resistant Dangling Pattern**.

## Global Architectural Rules (MANDATORY for ALL Contracts)

The following rules represent the "Iron Core" of the Hermi framework. **You MUST inject these metadata and rules into EVERY contract you create:**

### 1. Mandatory @apiNote Metadata
Every contract MUST specify its lifecycle phase and implementation urgency:
- **Phase**: [1 (Initial/Blueprint) | 2 (Refinement/Hardening)]. Ensures AI understands the maturity of the design.
- **Priority**: [1 (Lowest) - 5 (Critical)]. Dictates the level of strictness required during generation.

### 2. Mandatory @implSpec Logic
Always prepend these rules to the logic section:
- **STATELESSNESS**: Implementations MUST be strictly stateless. No request-specific instance variables. Only final, immutable dependencies (via constructor injection) are allowed.

## Core Philosophy: The Double-Block Structure

A complete documentation set consists of two distinct blocks:

1.  **Block 1: AI Architectural Contract**: Targeted at AI execution via `[AI ARCHITECTURAL CONTRACT]`.
    - **`@apiNote`**: **Role & Intent**. Define the AI's persona and the design's "Why". Use `<p>` tags for metadata (`Phase`, `Priority`).
    - **`@implSpec`**: **Hard Constraints**. What the AI *must* do. Use `<ul><li>` to force vertical list integrity.
    - **`@implNote`**: **Negative Prompts**. What the AI *must not* do. Use `<ul><li>` to isolate forbidden patterns.
    - **`@example`**: **Few-shot Learning**. A minimal implementation to guide the AI's output structure.

2.  **Block 2: Standard Java Class Javadoc**: Targeted at human developers for business overview and API usage.

## Architectural Contract Template (Formatter-Resistant)

```java
/**
 * [AI ARCHITECTURAL CONTRACT]
 * @apiNote
 * <p>ROLE: [Describe component role, e.g., Secure Transporter]
 * <p>DESIGN INTENT: [Describe the architectural goal, e.g., Isolation]
 * <p>PURPOSE: [Business or technical rationale]
 * <p>Phase: [1|2]
 * <p>Priority: [1-5]
 *
 * @implSpec
 * GENERATION RULES FOR AI AGENTS:
 * <ul>
 *   <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific instance variables.</li>
 *   <li>2. [Component Specific Rule 1]...</li>
 * </ul>
 *
 * @implNote
 * FORBIDDEN PATTERNS:
 * <ul>
 *   <li>NEVER [Forbidden Action A]</li>
 *   <li>DO NOT [Forbidden Action B]</li>
 * </ul>
 *
 * @example
 * <pre>{@code
 * // Recommended implementation pattern
 * }</pre>
 */

/**
 * {Summary of the component: A concise one-sentence description ending with a period}.
 * {Standard Javadoc tags: @param, @return, @see, etc., as applicable}.
 */
```

## Best Practices

- **Vertical Integrity**: Each rule MUST be on its own line using `<li>` to assign high weight to AI's attention mechanism.
- **Formatter Resistance**: Always use `<p>` for metadata and `<ul><li>` for rules to prevent IDEs (like Google Java Format) from collapsing the block.
- **Explain the "Why"**: The AI performs better when it understands the reasoning (e.g., "to ensure zero runtime overhead").
- **Physical Distribution**: These contracts are distributed via `-sources.jar`, ensuring they reach the AI context window cross-project.