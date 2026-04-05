# Hermi Framework Manifesto: AI Reference Guide

This document is the authoritative, rule-driven protocol for building Intent-Driven Architecture (IDA). AI agents must prioritize these constraints over subjective preferences.

## 1. Core Definitions (Intent-Driven Architecture)
- **Action**: The immutable business intent (e.g., Create, Find, Cancel).
- **Context**: The exhaustive set of inbound facts required to execute the Action. MUST implement `Validatable`.
- **Result**: The definitive set of outbound facts representing the Action's outcome.
- **Intent**: An external协作 (Client, Repository, or Messenger) discovered during logic orchestration.

## 2. Three Integrity Pillars (Non-Negotiable)
1. **Boundary Integrity**: The **Core (Phase 1)** MUST NEVER import the **Shell (Phase 2)**. This is enforced through **physical separation via Maven modules**. Business logic must remain in a pure Java module, decoupled from infrastructure (HTTP, SQL, Spring).
2. **Protocol Integrity**: All data crossing the Core boundary via `Context` or `Result` (from Clients, Repositories, Messengers) MUST satisfy the `Validatable` contract.
3. **Semantic Integrity**: Code MUST mirror business intention. Terminology must be Action-Resource focused (e.g., `UserRepository.save()` vs `{Action}{Resource}Repository.execute()`).

## 3. The Discovery Lifecycle (The Flow)
### Phase 1: Discovery & Verification (The Core / Pure Java)
- **Establish Boundary**: Define `UseCase.Context` and `UseCase.Result`.
- **Just-In-Time (JIT) Discovery**: Define abstract I/O contracts (Client, Repository, Messenger) ONLY when business logic reveals a specific dependency.
- **Holistic Orchestration**: Write a complete narrative in `Default{Action}{Resource}UseCase.doExecute()`, treating collaborators as if they already exist.
- **Phase 1 Gate (Verification)**: Build local stateful test adapters (e.g., `InMemoryUserRepository`). **PROHIBIT Mockito/Mocking frameworks.**

### Phase 2: Realization & Delivery (The Shell / Infrastructure)
- **Production Materialization**: Build technology-specific adapters (Jdbc, Rest, Kafka).
- **Expose via Entry Points**: Wire the Core into entry mechanism (Spring RestController, Kafka Consumer, AI MCP Tool) using the `*Shell` suffix (e.g., `FindUserApiShell`).

## 4. Naming Regular Expressions & Patterns
| Component | Naming Pattern | Rule |
| :--- | :--- | :--- |
| **UseCase** | `{Action}{Resource}UseCase` | Use specific business verbs (Present Tense). |
| **Client** | `{Action}{Resource}Client` | Initiating actions/fetches from external systems. |
| **Repository**| `{Action}{Resource}Repository`| Persisting or loading state (CRUD). |
| **Messenger** | `{Notify}{Fact}Messenger` | Publishing events or notifications (Outbound). |
| **Implementation**| `Default{Action}{Resource}UseCase`| The standard Core logic provider. |
| **Test Adapter**| `{Local/InMemory}{ActualContractName}`| Implementation in `/test` folder (Phase 1). |
| **Prod Adapter**| `{Tech}{ActualContractName}` | Implementation in `/main` shell folder (Phase 2). |
| **Entry Point** | `{Action}{Resource}{Type}Shell` | Framework-specific entry point (Phase 2). |

## 5. Verification Constraints (The Test Shell)
- **No Mocks**: Mocks only verify interactions (fragile); IDA requires verifying **Empirical Proof** (state changes).
- **Assertion Mode**: Assertions MUST be made against the **Final Result** and the **Final State** of the test adapters.
- **Stateful Adapters**: Local adapters MUST reflect real-world state transitions using standard Java structures (e.g., `Map` for registries).

## 6. Implementation Checklist for AI Agents
1. Have I confirmed the Action and Resource with the user?
2. Does the `Context` implement `Validatable`?
3. Are all I/O contracts discovered JIT and appropriately named (Client, Repository, or Messenger)?
4. Is the orchestration a complete narrative in `doExecute`?
5. Did I provide a `TestShell` with stateful adapters instead of mocks?
6. Is there a strict physical separation (e.g., separate Maven modules) between the Core logic and the Shell infrastructure?
