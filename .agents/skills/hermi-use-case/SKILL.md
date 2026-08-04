---
name: hermi-use-case
description: Generates, designs, and refactors Use Cases for the Hermi Intent-Driven Architecture framework. Use when creating pure Java business intent classes, designing use case boundaries with input and output records.
compatibility: Requires Java 17+, pure Java development mindset, and adherence to the Hermi architecture style.
---

# Hermi Use Case

## What is a Use Case?
In Hermi's Intent-Driven Architecture, the Use Case is the **Sovereign of the domain**, representing a specific business intent. It defines *What* the system does while pushing all technical details (*How* it does it) to the outer Shell. It consists of an **Abstract Class** (defining input/output boundaries) and a **Concrete Implementation** (orchestrating pure Java logic).

## Why Use It?
* **Boundary Control & Security:** Acts as a protocol enforcer via `Validatable` input records to ensure safe data processing.
* **Infrastructure Independence:** Enables pure Java logic execution without external frameworks.
* **Clear Orchestration:** Centralizes I/O contract coordination (Clients, Repositories, Messengers) into testable units.

## When to Use It?
* When creating pure Java business core classes.
* When designing use case boundaries using input and output records.

## Where Does It Live?
* **Phase 1:** Developed as a standalone abstract/concrete class inside a pure Java module with zero framework dependencies, testable directly via memory or a Main Shell.

## How-To: Step-by-Step Instructions

1. **Identify the Business Intent**: Determine the `{Action}` (e.g., `Find`, `Create`, `Update`, `Delete`) and `{Resource}` (e.g., `User`, `Order`, `Payment`).
2. **Define Boundaries (Input & Output)**:
   * Create an abstract class named `{Action}{Resource}UseCase` extending `org.hermi.usecase.standard.UseCase<Input, Output>` (the input record can be named `Context`, `Param`, or `{Action}Param`, and the output record can be named `Result`, `Response`, or `{Resource}Info`).
   * Define a static nested input record implementing `org.hermi.constraint.validation.Validatable` with validation annotations (`@NotNull`, `@NotBlank`).
   * Define a static nested output record containing fields for the execution outcome.
3. **Implement Core Logic**:
   * Create an implementation class named `Default{Action}{Resource}UseCase`.
   * Inject required I/O contracts via constructor injection.
   * Override `doExecute(Input input)` to perform pure Java business choreography.
4. **Ensure Purity**: Verify the module contains **zero** framework or infrastructure dependencies.

## Code Example

```java
// 1. Define Use Case boundaries (Input and Output record names can be customized to fit domain semantics)
public abstract class RegisterUserUseCase extends UseCase<RegisterUserUseCase.Context, RegisterUserUseCase.Result> {
    public static record Context(
        @NotNull @NotBlank String email,
        @NotNull @NotBlank String password
    ) implements Validatable {}
    
    public static record Result(String userId, String status) {}
}

// 2. Implement core business logic
public class DefaultRegisterUserUseCase extends RegisterUserUseCase {
    private final SaveUserRepository saveUserRepository;
    private final EmailMessenger emailMessenger;

    public DefaultRegisterUserUseCase(SaveUserRepository saveUserRepository, EmailMessenger emailMessenger) {
        this.saveUserRepository = saveUserRepository;
        this.emailMessenger = emailMessenger;
    }

    @Override
    protected Result doExecute(Context context) {
        // Core business choreography
        var userId = saveUserRepository.execute(new SaveUserRepository.Context(context.email(), context.password()));
        emailMessenger.execute(new EmailMessenger.Context(context.email(), "Welcome!"));
        
        return new Result(userId, "SUCCESS");
    }
}
```

## Common Edge Cases & Rules

* **Missing Validation**: Always ensure the input record implements `Validatable` and fields have appropriate validation annotations.
* **Flexible Boundary Naming**: While `Context` and `Result` are standard, input and output records can be named differently (e.g., `Param`, `Request`, `Response`, `Info`) if they better match domain semantics.
* **Framework Leakage**: Never inject Spring annotations (`@Service`, `@Autowired`) or JPA annotations into the Use Case module. The business core must remain 100% pure Java.
* **Naming Deviations**: Always adhere strictly to `{Action}{Resource}UseCase` for the abstract class and `Default{Action}{Resource}UseCase` for the implementation.