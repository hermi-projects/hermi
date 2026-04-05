# Hermi Framework Primer: Intent-Driven Architecture (IDA)

The Hermi Framework is designed to protect **Business Intent** from **Delivery Details**. It follows a strict "Engineering-First" approach, ensuring that your core logic can survive platform migrations (e.g., from Spring to Quarkus) or delivery shifts (e.g., from REST to AI MCP Tools).

## Key Principles

### 1. Intent vs. Delivery
- **Intent**: *What* the business wants to do (e.g., "Find a User", "Notify a Customer").
- **Delivery**: *How* it's done technically (e.g., "LDAP query", "AWS SES email").
- **Rule**: Intent is constant; Delivery is interchangeable.

### 2. The Two-Phase Lifecycle
- **Phase 1: Discovery**: We define the boundary using pure Java. We "discover" dependencies (Clients, Repositories, Messengers) as abstract contracts *while* writing the logic.
- **Phase 2: Realization**: We implement those contracts as production adapters using specific technologies (Spring, JDBC, Kafka).

### 3. The Discovery Loop
We never ask for all requirements upfront. We poll for:
1. **Action/Resource**: The "What" and "On Whom".
2. **Context**: Input fields.
3. **Result**: Output fields.
4. **Intents**: The external collaborators needed.

### 4. Physical vs. Logical Separation
In Hermi, separation of concerns is enforced **physically** via Maven/Gradle modules, not just logically via packages.
- **The Core Module**: Contains pure Java logic. It has ZERO dependencies on infrastructure frameworks (Spring, Hibernate, etc.). This ensures the business intent is protected from "framework leakage."
- **The Shell Module**: Depends on the Core module to implement its contracts. This is where technology choices (Spring Boot, Kafka, JDBC) are materialized.
- **Rule**: If the Core module imports a framework class, the **Boundary Integrity** is broken.

## Core Components

| Component | Description |
| :--- | :--- |
| **UseCase** | The orchestrator of business logic. Pure Java. |
| **Client** | An intent to call an external service for data. |
| **Repository** | An intent to persist or retrieve state. |
| **Messenger** | An intent to notify the world of a fact. |
| **Shell** | The outermost layer (REST Controller, Kafka Consumer) that triggers the Use Case. |

## Why "No Mocks"?
In Phase 1, we verify logic using **Test Shells** (stateful in-memory adapters) instead of mocks. This proves the logic works against real-world state transitions, providing "Engineering-First" verification before the database even exists.
