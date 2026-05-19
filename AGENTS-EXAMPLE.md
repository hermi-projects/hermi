# Hermi Framework: AI Agent Guidelines

## 1. Project Overview & Architecture
**Framework**: `Hermi`
**Architecture**: Intent-Driven Architecture
**Purpose**: Ensure strict separation between Pure Domain Logic (Phase 1: Use Cases) and Infrastructure/Delivery concerns (Phase 2: Shell).

## 2. ALLOWED SCOPE

When generating new functionality, the primary architectural components **MUST** be a subclass or implementation of the base contracts listed in the grid below. DO NOT invent new global services or managers. 

*(**Exemptions (Classes that do not require a Hermi Base Class)**: 
1. **Framework Entry Points**: (e.g., Spring `@RestController`, `@KafkaListener`) - act purely as protocol boundaries.
2. **Configuration & Wiring**: (e.g., `@Configuration`, `@Bean`) - strictly for dependency injection and framework setup.
3. **Data Carriers**: (e.g., JPA `@Entity`, JSON DTOs, pure Java `record`) - MUST be anemic (contain NO business logic).
4. **Custom Exceptions**: (e.g., `DomainException`) - for business or technical error signaling.
5. **Helper/Utility Classes**: (e.g., parsers, formatters) - MUST be stateless, contain NO I/O, and be strictly scoped/private to a specific Use Case or Adapter.)*

Before writing code for any of these, you MUST explicitly ask the user for the "Required Information" listed in the grid below.

| Layer | Name | Class Full Name | Description | Required Information to Implement | Key Constraints |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Use Case (Phase 1)** | `UseCase` | `org.hermi.usecase.standard.UseCase` | Pure Domain Logic Sovereign (Contains Business Rules & Stateless Workflow) | 1. Core Workflow & Business Rules<br>2. Context Schema<br>3. Result Schema<br>4. Required I/O Contracts<br>5. Domain Exceptions | **Stateless**. Must encapsulate purely **Business Rules**. **NO** infra types. MUST catch infra exceptions and throw Domain Exceptions. |
| **Use Case (Phase 1)** | `DispatcherUseCase` | `org.hermi.usecase.standard.DispatcherUseCase` | Routes domain logic based on event/action types | 1. Routing Logic<br>2. Context Schema<br>3. Result Schema | **Stateless**. **NO** infrastructure types. |
| **Use Case (Phase 1)** | `Client` | `org.hermi.usecase.standard.Client` | Contract for external service capability | 1. Target Capability<br>2. Context Schema<br>3. Result Schema and constrains | **Stateless**. **NO** Tech Bleed (no HttpHeaders). **Pure Java** records only. |
| **Use Case (Phase 1)** | `Messenger` | `org.hermi.usecase.standard.Messenger` | Contract for asynchronous event dispatch | 1. Target Event/Action<br>2. Context Schema<br>3. Result Schema and constrains | **Stateless**. **NO** broker types (no ProducerRecord). **Pure Java** records only. |
| **Use Case (Phase 1)** | `Repository` | `org.hermi.usecase.standard.Repository` | Contract for domain data persistence | 1. Target Action (Save/Find)<br>2. Context Schema<br>3. Result Schema and constrains | **Stateless**. **NO** JPA/JDBC types. **NO** business logic inside. **Pure Java** records only. |
| **Shell (Phase 2)** | `Client` | `org.hermi.shell.Client` | Technical HTTP/RPC protocol executor | 1. Endpoint URL<br>2. HTTP Method<br>3. Auth<br>4. Headers<br>5. Request Schema<br>6. Response Schema<br>7. Error Format | ONLY override `doExchange`. **NO** try-catch telemetry. **NO** data translation. |
| **Shell (Phase 2)** | `Messenger` | `org.hermi.shell.Messenger` | Technical broker/topic messaging executor | 1. Destination (Topic)<br>2. Message Headers<br>3. Message Payload Schema | ONLY override `doPublish`. **NO** try-catch telemetry. |
| **Shell (Phase 2)** | `Mapper` | `org.hermi.shell.Mapper` | Anti-Corruption Translation Gateway | 1. Domain Schema<br>2. Vendor Schema<br>3. Field-level Mapping Rules | PURE TRANSLATION. **NO** I/O calls. **NO** branching logic. **NO** exceptions thrown. |
| **Shell (Phase 2)** | `SecureClient` | `org.hermi.shell.secure.SecureClient` | Protocol executor with payload encryption | 1. Standard Client Info<br>2. Specific PII fields requiring encryption | ONLY override `doExchange`. **NO** telemetry. MUST invoke Cryptor. |
| **Shell (Phase 2)** | `PersistentAuditor` | `org.hermi.shell.audit.PersistentAuditor` | Observability and compliance logger | 1. Audit Log Destination<br>2. Audit Event Schema | MUST NOT block main business threads unnecessarily. |
| **Shell (Phase 2)** | `Cryptor` | `org.hermi.shell.secure.Cryptor` | Field-level encryption/decryption handler | 1. Encryption Algorithm<br>2. Key Management details | **Stateless**. MUST be deterministic. |
| **Shell (Phase 2)** | `LocalClient` | `org.hermi.shell.util.LocalClient` | Local adapter for Phase 1 Use Case testing/validation | 1. Mock state fields to define | **NOT for Production**. State must be local. NO external dependencies. |
| **Shell (Phase 2)** | `InMemoryRepository` | `org.hermi.shell.util.InMemoryRepository` | Local adapter for Phase 1 Use Case testing/validation | 1. Java Collection type (Map/List) to use | **NOT for Production**. State must use standard Collections. NO DB. |
| **Shell (Phase 2)** | `ConsoleMessenger` | `org.hermi.shell.util.ConsoleMessenger` | Local adapter for Phase 1 Use Case testing/validation | 1. Output string format | **NOT for Production**. PURE WIRING to System.out. NO broker. |

## 3. Escalation Protocol
If a task requires an integration pattern that does not cleanly fit into the Whitelist above (e.g., a GraphQL fetcher, a WebSocket broadcaster, a cron-job trigger):
1. Do NOT invent new patterns or implement `Executor` directly.
2. Explicitly ask the human Architect to define a new foundational Base Class contract before you proceed.

## 4. Code Style & Documentation
- **AI-Native Javadoc Headers**: All core components must use a single, unified `/** ... */` Javadoc block, but strictly segmented using specific headers (`@apiNote`, `@implSpec`).
- **Statelessness**: All implementations MUST be strictly stateless. Only final, immutable dependencies (via constructor injection) are allowed.

### Naming Conventions (Crucial)
- **Structure**: Methods MUST follow `verb + noun` or `verb + preposition + noun`. Always use **lowerCamelCase** for methods and variables.
- **Verb Vocabulary**:
  - **Remote/Heavy Data**: Use `fetch...` (e.g., `fetchUserProfile`).
  - **Database/Complex Queries**: Use `find...` or `query...`.
  - **In-Memory/Lightweight Getter**: Use `get...`.
  - **Boolean Returns**: MUST start with `is...`, `has...`, `can...`, or `should...`.
  - **Data Conversion**: Use `to...`, `parse...`, or `as...`.

## 5. The Three Iron Rules (Global Disciplines)
1. **Anti-Mocking Rule**: NO Mocking Frameworks (Mockito, PowerMock) are allowed in Phase 1 (Use Case Core) tests. You MUST use stateful `LocalAdapters` (e.g., `InMemoryRepository`) to prove logic against real state transitions.
2. **Dependency Anti-Bleed Rule**: Phase 1 (Core) `pom.xml` or `build.gradle` MUST NOT contain any Spring, Database, Kafka, or network-related dependencies.
3. **Validation Protocol**: Any data crossing the Use Case boundary (e.g., `UseCase.Context`, `Client.Result`) MUST explicitly `implements Validatable`.

## 6. Architectural Wiring (Production Adapters)
When implementing a Phase 1 Contract (e.g., `FindUserClient`) for Production, you MUST use the strict **Adapter Pattern**:
1. Extend the custom Phase 1 Contract.
2. Inject the Phase 2 Vendor Executor (e.g., `LexisNexisUserClient`) and the Phase 2 `Mapper`.
3. The overridden method must ONLY perform pure wiring: `mapper.toPayload()` -> `vendorClient.execute()` -> `mapper.toResult()`. NO business logic.

## 7. Security & Observability
- **PII/Sensitive Data**: Must be processed through a **Cryptor** component before storage or transmission.
- **Observability**: All client interactions must be logged via an **PersistentAuditor** component.

## 8. Implementation Data Guardrails (Pre-Flight Checks)
Before writing any implementation code for the allowed contracts, you MUST STOP and explicitly ASK the human for the exact data points listed in the **"Required Information to Implement"** column of the `ALLOWED SCOPE` grid (Section 2). 

Do NOT assume or hallucinate any external URLs, broker topics, or domain schemas. Wait for the human to provide the complete checklist before generating code.
