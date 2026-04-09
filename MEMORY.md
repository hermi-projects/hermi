# Hermi: AI Software Factory Memory Log

**Timestamp**: 2026-04-05
**Topic**: Establishing the "AI Software Factory" Architectural Standard based on Hermi Framework.

## 1. Core Architectural Meta-Data

The Hermi framework's core philosophy is **Intent-Driven Architecture (IDA)**, where the **Use Case is the Sovereign**. Infrastructure (Shell) is a subordinate detail that must fulfill the revelation of the Core logic.

### Refined Strategy: Strategic Intent Sweep
To adapt Hermi for AI-native development, we evolved the "Top-Down, Breadth-First" strategy into the **Strategic Intent Sweep**:
*   **Top-Down** = **Blueprint-First Orchestration**: High-level intent dictates low-level infrastructure.
*   **Breadth-First** = **Narrative-First Discovery**: All I/O contracts are discovered and locked in Phase 1 before a single line of technology-specific code is written in Phase 2.

---

## 2. The AI Software Factory Model

We successfully factorized the development lifecycle into two distinct, non-overlapping phases to maximize AI efficiency.

### Phase 1: Strategic Discovery (Collaborative)
*   **Execution**: Human Architect + Architect Agent.
*   **Process**: **Narrative-First Discovery** via JIT contract identification.
*   **Goal**: Create a verified **Core Blueprint**. 
*   **Verification**: **Phase 1 Gate (Test Shell)**. The logic must be proven 100% correct using in-memory state simulators before implementation.

### Phase 2: Tactical Implementation (Autonomous)
*   **Execution**: **Specialized Agent Pool** (RepoAgent, ClientAgent, MsgAgent).
*   **Input**: **Work Order 2.0** (High-precision task definitions).
*   **Process**: **Dual-Phase Verification** (Worker Construction -> Review & Test Audit).
*   **Outcome**: Technology Adapters (Shell Layer).

---

## 3. High-Precision Work Order System (Work Order 2.0)

For seamless AI handover, the "Blueprint" must be translated into structured Work Orders containing:
1.  **Constraint Diffusion**: Propagating `@Validatable` metadata (e.g., `@NotBlank`) directly to SQL DDL/DTO schemas.
2.  **Doc-Aware Context Enhancement**: JIT retrieval of external documentation (OpenAPI, DB Schemas) attached to the prompt.
3.  **Semantic Error Mapping**: Mapping technical exceptions (e.g., `404`, `500`) to business exceptions (e.g., `UserNotFound`, `SystemUnavailable`).

---

## 4. Engineering Rationale for Future AI Discussions

*   **Context Isolation**: By breaking projects into small, isolated tasks (Sandboxes), we stay under 2k tokens of context, maximizing AI accuracy and reducing cost.
*   **JIT Contract Discovery**: Contracts are never "pre-designed"; they are "discovered" as logic reveals a need, matching the organic flow of building business narrative.
*   **One-Way Dependency**: Physics (Maven modules) enforce that Shell depends on Core. This ensures that AI "hallucinations" in the implementation layer cannot corrupt the business core.

---

## 5. Summary of Key Terminology (The "Language" of this Project)

*   **UseCase Sovereignty**: The core logic is the boss.
*   **Blueprint-First Orchestration**: Strategy of building the plan before the parts.
*   **Narrative-First Discovery**: Strategy of letting the story find the needs.
*   **Strategic Intent Sweep**: The broad scanning of business requirements.
*   **Dual-Phase Verification**: Autonomous Build -> Independent Audit.

> [!NOTE]
> This memory file serves as the "System Prompt Expansion" for any future AI Agent interacting with this project. It ensures that the "Engineering-First" standards established on 2026-04-05 are maintained.
