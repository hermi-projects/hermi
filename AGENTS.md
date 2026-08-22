# AGENTS.md — Hermi Framework Governance

This repository houses the core engine, standard abstractions, and lifecycle machinery of **Hermi: Intent-Driven Architecture**. All contributors (humans and AI agents) must strictly adhere to these engineering and architectural standards. The canonical narrative lives in [README.md](README.md); documentation rules live in [docs/AGENTS.md](docs/AGENTS.md).

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
mvn spotless:apply         # Format code
```

## Conventions

### 1. Framework Architecture Invariants

* **The Sovereign `fulfill()` Contract**: `Executor` enforces the lifecycle — audit start → validate context → `doExecute` → validate result → audit success — and `execute` is `protected final`. `UseCase.fulfill` is `final`; `doFulfill` is the only extension point. Never call `doFulfill` directly, never subclass `Executor` directly.
* **Fail Loudly on Misconfiguration**: Core machinery has no silent fallbacks — `Executor` rejects null auditors and validators with `Objects.requireNonNull`, and `DispatcherUseCase` throws `HandlerNotFoundException` rather than defaulting to a fallback handler.
* **Module Dependency Discipline**: Each module's dependencies are fixed by its purpose — `hermi-shell` depends only on `hermi-commons`; delivery-framework dependencies (Spring Boot) appear only in the runnable demo module `hermi-logging-test`. Do not add delivery frameworks to modules that do not already carry them.
* **Test Support via Boundary Machinery**: Test input generation in `hermi-test` is driven by `Boundary` (valid/invalid values) registered in `BoundaryRegistry` and fed by `HermiArgumentsProvider`. New type support extends `Boundary`, not ad hoc per-test input factories.

### 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

* State your assumptions explicitly. If uncertain, ask.
* If multiple interpretations exist, present them - don't pick silently.
* If a simpler approach exists, say so. Push back when warranted.
* If something is unclear, stop. Name what's confusing. Ask.

### 3. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

* No features beyond what was asked.
* No abstractions for single-use code.
* No "flexibility" or "configurability" that wasn't requested.
* No error handling for impossible scenarios.
* If you write 200 lines and it could be 50, rewrite it.
* Use Guard Clauses to implement Early Returns and keep the happy path unindented.
*(Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.)*

### 4. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

* Don't "improve" adjacent code, comments, or formatting, and don't refactor things that aren't broken.
* Match existing style, even if you'd do it differently.
* Remove imports/variables/functions that YOUR changes made unused. The test: Every changed line should trace directly to the request.

### 5. Goal-Driven Execution

**Define success criteria. Loop until verified.**

* "Add validation" → "Write tests for invalid inputs, then make them pass"
* "Fix the bug" → "Write a test that reproduces it, then make it pass"
* For multi-step tasks, state a brief plan with verification checkpoints.

### 6. Code Quality Standards

* **Readability** — Name functions and variables for intent, not implementation.
* **Simplicity** — One method, one job. If a method exceeds 20 lines, it's doing too much.
* **Maintainability** — Open for extension, closed for modification.
* **Robustness** — Handle null, failure, and edge cases at system boundaries. Never swallow exceptions.
* **Efficiency** — Choose data structures by their algorithmic guarantees. The hot path must not pay for unused abstractions.

## Documentation

* **Document Current State, Never Change History**: Never use "previously", "now", "was changed", or PR references in code comments or durable prose. Git commits and decision records own history.
* **Do Not Comment on Facts Obvious from Code**: Eliminate comment slop. JavaDoc is reserved for non-obvious contracts, threading/concurrency invariants, and type constraints.
* **AI-Native Javadoc**: Every Java component carries the Double-Block structure (AI Contract + Standard Javadoc) mandated by the hermi-doc skill — see [AI-NATIVE-JAVADOC-PROMPT.md](AI-NATIVE-JAVADOC-PROMPT.md).
* **Non-Trivial Changes Require a Decision Record**: Structural changes to the execution lifecycle, validation handling, or auditing format MUST ship with a decision record; [docs/AGENTS.md](docs/AGENTS.md) defines the canonical location.
* **One Home per Fact**: Avoid duplicating rules or explanations across documents; link to the authoritative source via relative paths.
* **Markdown Formatting**: Use soft-wrap (one physical line per paragraph) to maintain clean git diffs. Files end with exactly one trailing newline.

## Editing these instructions

`CLAUDE.md` may symlink to this `AGENTS.md` at root; edit the real file directly. Keep each rule self-contained while linking high-level docs in `docs/`. Condense when clarity survives.
