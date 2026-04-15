📘 Hermi Annotation Module

The Hermi Annotation module provides a set of lightweight, framework‑agnostic annotations used to describe the semantic meaning, sensitivity, and data type of fields within domain models.

These annotations contain no business logic.They serve purely as metadata and are interpreted by other modules such as:

hermi-validation → format checking

hermi‑logging → masking sensitive data

future modules → API documentation, serialization rules, etc.

This module is the foundational layer of the Hermi ecosystem.

🎯 Purpose of This Module

Define shared annotations used across multiple modules

Avoid duplication of annotation definitions

Keep annotations independent from validation or logging logic

Provide a stable metadata layer for the entire ecosystem

The annotation module does not:

Perform validation

Perform masking

Contain any runtime logic

All behavior is implemented in upper‑level modules.

📦 Included Annotations

1. @SSN

Marks a field as a Social Security Number.

Used by:

validation → validate SSN format

logging → apply SSN masking

Example:
```java
@SSN
private String ssn;
```
2. @Email

Marks a field as an email address.

Used by:

validation → email format validation

logging → email masking

Example:
```java
@Email
private String email;
```

3. @Phone

Marks a field as a phone number.

Used by:

validation → phone number validation

logging → phone number masking

Example:

```java
@Phone
private String phone;
```

🧠 Design Principles

✔ 1. Annotations as metadata

Annotations describe what the field represents, not how it should be processed.

✔ 2. Zero dependencies

This module does not depend on validation, logging, or any other module.

✔ 3. Reusable across the ecosystem

Multiple modules can interpret the same annotation differently.

✔ 4. Avoid annotation duplication

Users annotate fields once, and all modules benefit.

🧩 Example: Used with Validation Module

```java
public class UserInput {

    @Email
    private String email;

    @SSN
    private String ssn;
}
```

The validation module will automatically validate the formats.

🧩 Example: Used with Hermi Logging Module

```java
public class UserInfo {

    @Phone
    private String phone;
    
    @SSN
    private String ssn;
}
```

The logging module will automatically mask sensitive fields.

🎯 Summary

The Hermi Annotation module defines the shared annotation layer for the entire Hermi ecosystem.It provides:

Unified annotation definitions

Clean module boundaries

Excellent extensibility

A great developer experience

Validation, logging, and future modules can all interpret these annotations to provide powerful, consistent behavior.
