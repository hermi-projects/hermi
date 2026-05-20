# Architectural Naming Archetypes

When dealing with specialized components, always map the class suffix to its precise architectural responsibility:

*   **UseCase**: Implements core application business logic (e.g., `CreateOrderUseCase`).
*   **Client**: Manages communication with external 3rd-party APIs or microservices (e.g., `PaymentGatewayClient`).
*   **Repository**: Handles database persistence abstractions (e.g., `CustomerRepository`).
*   **Messenger**: Dispatches events, notifications, or cloud messages (e.g., `EmailNotificationMessenger`).