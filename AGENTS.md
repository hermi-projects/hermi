# AGENTS.md — Hermi Framework Governance

This repository houses the core engine, standard abstractions, and lifecycle machinery of **Hermi: Intent-Driven Architecture**. All contributors (humans and AI agents) must strictly adhere to `Clean Code` standards. The canonical narrative lives in [README.md](README.md); documentation rules live in [docs/AGENTS.md](docs/AGENTS.md).

## Repository layout

```text
hermi/
├── hermi-constraint     # Data masking (@Mask, @SSN) and boundary validation machinery
├── hermi-commons        # Execution lifecycle engine (Executor), Auditor, and Validator machinery
├── hermi-logging        # Structured execution logging driven by @HermiLogging (AspectJ)
├── hermi-logging-test   # Runnable demo app and scenario tests exercising hermi-logging
├── hermi-usecase        # Intent contract, UseCase/Client/Repository/Messenger, DispatcherUseCase routing
├── hermi-shell          # Infrastructure adapter contracts: Client, Consumer, Controller, Mapper, Messenger, SecureClient
├── hermi-starter        # Aggregate of hermi-usecase, hermi-shell, and hermi-logging
└── hermi-test           # Boundary-driven test support: BoundaryRegistry, Jupiter argument provider, InstanceGenerator
```

## Commands

```sh
mvn clean compile          # Compile all Maven modules
mvn test                   # Run unit tests across all modules
mvn verify                 # Run full verification (tests + integration checks)
mvn clean install          # Run full verification and install artifacts to local repository
mvn spotless:apply         # Format code
```

## Definition of Done 
Before declaring any implementation or refactoring task complete, agents must:

1. **Self-Audit**: Actively verify all modified code against the `clean-code` skill. If violations or code smells are found, refactor them before delivery.
2. **Format**: Ensure code is formatted according to the project's style guidelines.
3. **Verify & Install**: Run `mvn clean install` to run all tests and install verified artifacts to the local Maven repository.

## Editing these instructions

`CLAUDE.md` may symlink to this `AGENTS.md` at root; edit the real file directly. Keep each rule self-contained while linking high-level docs in `docs/`. Condense when clarity survives.
