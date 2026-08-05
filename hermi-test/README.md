# Hermi Test

`hermi-test` is a lightweight, reflection-based testing utility library for Java and JUnit 5. It automates boundary-value and structural edge-case generation for domain models (like `User` or `Customer`), eliminating repetitive test data setup and ensuring comprehensive guard-clause testing with minimal boilerplate.

---

## Features

* **JUnit 5 Native Integration:** Implements `ArgumentsProvider` to cleanly feed parameterized tests using `@ArgumentsSource`.
* **Primary Constructor & Field Injection Support:** Introspects domain models to construct objects using their primary constructor and falls back on direct field injection for any unassigned properties.
* **Modular Boundary Registry:** Uses a decoupled `Boundary<T>` interface strategy to define valid and invalid edge-case values for primitive and object types.
* **Combinatorial Explosion Protection:** Parameter-wise (one-factor-at-a-time) test generation ensures exhaustive boundary checks without running out of memory.

---

## Installation

Add the library to your project's test scope via Maven (ensure it is published to your local or remote repository via `mvn clean install`):

```xml
<dependency>
    <groupId>org.hermi</groupId>
    <artifactId>hermi-test</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>

```

---

## Usage Guide

### 1. Define Your Domain Model

Your domain models can use primary constructors or standard fields without requiring a public no-arg constructor:

```java
public class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
        validate();
    }

    public void validate() {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Invalid name");
        if (age < 18) throw new IllegalArgumentException("Must be at least 18");
    }
}

```

### 2. Write Parameterized Boundary Tests

Use the `@ArgumentsSource` annotation with `HermiExecutorArgumentsProvider` on your test method:

```java
import com.yourcompany.testing.provider.HermiExecutorArgumentsProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import static org.junit.jupiter.api.Assertions.*;

public class UserBoundaryTest {

    @ParameterizedTest(name = "Boundary test case injection")
    @ArgumentsSource(HermiExecutorArgumentsProvider.class)
    void testUserBoundaryViolations(User user) {
        // Assert that structural boundary violations correctly trigger validation guards
        assertThrows(IllegalArgumentException.class, () -> {
            // Your domain validation logic execution
        });
    }
}

```

---

## Extending Custom Boundaries

To support custom types (e.g., `Email`, `LocalDate`), implement the `Boundary<T>` interface and register them in the `BoundaryRegistry`:

```java
public class EmailBoundary implements Boundary<String> {
    @Override
    public String validValue() {
        return "test@example.com";
    }

    @Override
    public List<String> invalidValues() {
        return List.of(null, "", "invalid-email", "@missing-user.com");
    }
}

```
