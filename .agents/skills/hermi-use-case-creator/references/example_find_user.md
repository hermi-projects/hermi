# Implementation Example: Find User (IDA Reference)

This file provides the canonical code implementation pattern for the Hermi Discovery Lifecycle. Use these templates to ensure structural and semantic consistency.

## Phase 1: Discovery & Verification (The Core / Pure Java)

> **ARCHITECT ANNOUNCEMENT**: "Intent confirmed. I am now entering **Phase 1: Discovery & Verification**. I will implement the pure Java boundary and orchestrate the domain logic without any infrastructure noise."

### 1. Establish the Boundary (FindUserUseCase.java)
```java
package org.hermi.user.find.usecase;

import validation.commons.Validatable;
import org.hermi.usecase.standard.UseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public abstract class FindUserUseCase extends UseCase<FindUserUseCase.Context, FindUserUseCase.Result> {
    public record Context(@NotNull @NotBlank String ssn) implements Validatable {}
    public record Result(String name, String email) {}
}
```

### 2. Discover Intents (JIT Contracts)
```java
public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
    public record Context(String ssn) {}
    public record Result(String name, String email) implements Validatable {}
}

public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
    public record Context(String name, String email) {}
    public record Result(String id) implements Validatable {}
}

public abstract class NotifyUserFoundMessenger extends Messenger<NotifyUserFoundMessenger.Context, NotifyUserFoundMessenger.Result> {
    public record Context(String email, String message) {}
    public record Result(String messageId) implements Validatable {}
}
```

### 3. Holistic Orchestration (Implementation)
```java
public class DefaultFindUserUseCase extends FindUserUseCase {
    private final FindUserClient client;
    private final SaveUserRepository repo;
    private final NotifyUserFoundMessenger messenger;

    public DefaultFindUserUseCase(FindUserClient client, SaveUserRepository repo, NotifyUserFoundMessenger messenger) {
        this.client = client;
        this.repo = repo;
        this.messenger = messenger;
    }

    @Override
    protected Result doExecute(Context context) {
        // 1. Fetch user via client intent
        var apiResult = client.execute(new FindUserClient.Context(context.ssn()));
        
        // 2. Save user via repository intent
        repo.execute(new SaveUserRepository.Context(apiResult.name(), apiResult.email()));
        
        // 3. Notify via messenger intent
        messenger.execute(new NotifyUserFoundMessenger.Context(apiResult.email(), "Found user"));
        
        return new Result(apiResult.name(), apiResult.email());
    }
}
```

### 4. Phase 1 Gate (Verification with Test Shell)
```java
class FindUserTestShell {
    @Test
    void shouldFindAndNotifyUser() {
        var client = new LocalFindUserClient(); // Stateful Test Adapter
        var repo = new InMemorySaveUserRepository();
        var messenger = new ConsoleNotifyUserFoundMessenger();
        
        var useCase = new DefaultFindUserUseCase(client, repo, messenger);
        var result = useCase.execute(new FindUserUseCase.Context("123-456"));
        
        assertNotNull(result);
        assertEquals(1, repo.getSavedCount());
    }
}
```

## Phase 2: Realization & Delivery (The Shell / Infrastructure)

> **ARCHITECT ANNOUNCEMENT**: "Core logic verified. I am now entering **Phase 2: Realization & Delivery**. I will materialize the I/O Intents into production adapters and wire the final Delivery Shell (API/Entry Point)."

### 5. Materialize Intents (Production Adapters)
```java
@Component
public class LexisNexisFindUserClient extends FindUserClient 
    implements ClientAdapter<ApiReq, ApiRes, Context, Result> {
    // Implementation using RestTemplate/WebClient
}
```

### 6. Final Delivery (Entry Point Shell)
```java
@RestController
@RequestMapping("/users")
public class FindUserApiShell {
    private final FindUserService service; // Service wires production adapters

    @GetMapping
    public FindUserUseCase.Result findUser(@RequestBody FindUserUseCase.Context context) {
        return service.findUser(context);
    }
}
```
