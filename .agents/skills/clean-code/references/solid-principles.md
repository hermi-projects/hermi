# SOLID Principles

> **Core Rule:** SOLID principles guide the structural organization of classes and modules to ensure they are maintainable, flexible, and robust against change.

## 1. Single Responsibility Principle (SRP)
- **Rule:** A class should have one, and only one, reason to change.
- **Constraint:** Do not mix multiple actor requirements (e.g., UI, business logic, persistence) into a single class.

## 2. Open/Closed Principle (OCP)
- **Rule:** Software entities should be open for extension, but closed for modification.
- **Constraint:** Prefer polymorphism (interfaces/strategy pattern) over massive `if-else` or `switch-case` chains when introducing new variants or behaviors.

## 3. Liskov Substitution Principle (LSP)
- **Rule:** Derived classes must be substitutable for their base classes without breaking the system.
- **Constraint:** Subclasses must not weaken pre-conditions, strengthen post-conditions, or throw unexpected unsupported operation exceptions.

## 4. Interface Segregation Principle (ISP)
- **Rule:** Clients should not be forced to depend upon interfaces that they do not use.
- **Constraint:** Split fat, monolithic interfaces into smaller, role-specific client interfaces.

## 5. Dependency Inversion Principle (DIP)
- **Rule:** Depend upon abstractions, do not depend upon concrete implementations.
- **Constraint:** High-level modules must not depend on low-level modules; both should depend on shared abstractions (e.g., via Dependency Injection).
