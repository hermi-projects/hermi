**This project uses Hermi** — an Intent-Driven Architecture (IDA) framework. All new features
MUST follow the two-phase structure below. Do not invent alternative patterns.

Business logic lives in **Phase 1** (pure Java, no frameworks). Infrastructure lives in
**Phase 2** (Spring, Kafka, etc.). Every component is an `Executor` with a single `execute()`
entry point.

Use the decision table to find the right class for your scenario. Open the class to
read its `[AI ARCHITECTURAL CONTRACT]` Javadoc for full generation rules.

## Decision Table

| I need to… | Phase | Class | Package |
|------------|-------|-------|---------|-------|
| Define a business use case | 1 | `UseCase` | `org.hermi.usecase.standard` |
| Route to one of many handlers | 1 | `Handler` | `org.hermi.usecase.dispatcher` |
| Call an external API (contract) | 1 | `Client` | `org.hermi.usecase.standard` |
| Persist data (contract) | 1 | `Repository` | `org.hermi.usecase.standard` |
| Send outbound notification (contract) | 1 | `Messenger` | `org.hermi.usecase.standard` |
| Implement a REST/gRPC client | 2 | `Client` | `org.hermi.shell` |
| Implement a message producer (Kafka, JMS) | 2 | `Messenger` | `org.hermi.shell` |
| Consume inbound events (Kafka, JMS, SQS) | 2 | `Consumer` | `org.hermi.shell` |
| Encrypt payloads for vendor calls | 2 | `SecureClient` | `org.hermi.shell.secure` |
| Wire a use case into a generic shell | 2 | `Controller` | `org.hermi.shell` |
| Translate domain ↔ vendor schemas | 2 | `Mapper` | `org.hermi.shell` |
| Audit execution lifecycle to a DB | 2 | `PersistentAuditor` | `org.hermi.commons.audit` |
| Skip auditing entirely (default) | — | `NoopAuditor` | `org.hermi.commons.audit` |
| Log execution to SLF4J | — | `LogAuditor` | `org.hermi.commons.audit` |

## Iron Rules

These apply across all classes. They are not obvious from any single Javadoc.

1. **Phase 1 knows nothing of Phase 2.** Use Case modules import `hermi-usecase` only — no Spring, no Kafka, no JDBC.
2. **Contracts use pure Java records.** `Client.Context`, `Repository.Context`, `Messenger.Context` MUST use only `String`, `UUID`, `BigDecimal`, etc. No technology types.
3. **Data crossing boundaries MUST be `Validatable`.** `UseCase.Context` and `shell.Client.Result` must implement `Validatable`.
4. **Never subclass `Executor` directly.** Always go through a named base class: `UseCase`, `Client`, `Repository`, `Messenger`, `Handler`, `Consumer`, `Controller`, or `SecureClient`.
5. **Never mock in Phase 1 tests.** Use stateful local adapters (e.g., `InMemorySaveUserRepository`).
6. **Naming is structural.** `{Action}{Resource}UseCase`, `Default{Action}{Resource}UseCase`, `{Tech}{ContractName}`.
7. **Hermi Workflow** Must understand `The Discovery Lifecycle` on [Hermi](README.md) .

## Project Structure

New Hermi projects follow this layout. Phase 1 modules contain only pure Java — no framework
dependencies. Phase 2 modules wire the contracts to specific technologies (Spring, Kafka, etc.).

```
your-project/
├── use-cases/{project}-{action}-{resource}-use-case/    # Phase 1: Pure Java (no frameworks)
│   ├── pom.xml                                 #   depends on hermi-usecase only
│   ├── src/main/java/{org}/{resource}/{action}/usecase/
│   │   ├── {Action}{Resource}UseCase.java      #   Contract: Context + Result
│   │   ├── Default{Action}{Resource}UseCase.java # Implementation: orchestration
│   │   ├── {Action}{Resource}Client.java       #   JIT-discovered API contract
│   │   ├── {Action}{Resource}Repository.java   #   JIT-discovered persistence contract
│   │   └── Notify{Fact}Messenger.java          #   JIT-discovered notification contract
│   └── src/test/java/{org}/{resource}/{action}/shell/
│       ├── {Action}{Resource}Main.java         #   Main Shell (Phase 1 verification)
│       ├── Local{Action}{Resource}Client.java  #   Stateful local adapter
│       └── InMemory{Action}{Resource}Repo.java #   Stateful local adapter
│
└── {project}-{framework}-{type}-shell/         # Phase 2: Infrastructure
    ├── pom.xml                                 #   depends on hermi-shell, Spring, etc.
    └── src/main/java/{org}/{resource}/{action}/shell/
        ├── {Action}{Resource}Controller.java   #   REST entry point
        ├── {Action}{Resource}Consumer.java     #   Kafka/JMS entry point
        ├── {Action}{Resource}Service.java      #   Transactional service (optional)
        ├── client/
        │   ├── {Tech}{Action}{Resource}Client.java  # Production adapter
        │   ├── {Vendor}{Resource}Client.java   #   Vendor protocol executor
        │   └── {Vendor}{Resource}Mapper.java   #   Domain ↔ vendor translation
        ├── repository/
        │   ├── {Tech}{Action}{Resource}Repo.java    # Production adapter
        │   └── {Vendor}{Resource}Mapper.java   #   Domain ↔ entity translation
        └── messenger/
            ├── {Tech}Notify{Fact}Messenger.java      # Production adapter
            ├── {Vendor}{Resource}Messenger.java #   Vendor protocol executor
            └── {Vendor}{Resource}Mapper.java   #   Domain ↔ payload translation
```
