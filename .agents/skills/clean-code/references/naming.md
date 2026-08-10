# Meaningful Names

> **Core Rule:** Good names must answer all the big questions: why it exists, what it does, and how it is used. If a name requires a comment to explain it, the name has failed.

## 1. Intention-Revealing Names
- **Rule:** Variables, functions, and classes must explicitly reveal their intent without mental mapping.
- **Constraint:** If a variable or function requires a comment to clarify its purpose, its name is invalid.
- **Example (Bad):** `int d;`
- **Example (Good):** `int elapsedTimeInDays;`

## 2. Avoid Misinformation
- **Rule:** Do not leave false clues or inaccurate type hints that obscure code meaning.
- **Constraint:** Do not name a container `accountList` unless its data structure is literally a `List`; use `accounts` instead. Avoid using names that vary only slightly or contain subtle typos.

## 3. Make Meaningful Distinctions
- **Rule:** Eliminate redundant noise words that exist solely to satisfy compiler syntax.
- **Constraint:** Do not use meaningless suffixes like `ProductInfo`, `ProductData`, or `ProductObject` when `Product` is sufficient (`Info` and `Data` are indistinct clutter). Do not create pseudo-synonymous methods returning the exact same type.

## 4. Use Searchable Names
- **Rule:** Single-letter names and magic numbers are prohibited in broad scopes.
- **Constraint:** The scope rule applies—single-letter variables (like `i`, `j`, `k`) are permitted *only* as short-lived loop counters. Replace magic numbers with uppercase named constants (e.g., `SECONDS_PER_DAY = 86400`).

## 5. Avoid Encodings & Prefixes
- **Rule:** Do not encode types, scopes, or framework details into names.
- **Constraint:** Hungarian notation (e.g., `strName`) is forbidden. Do not prefix interface names with `I` (e.g., avoid `IUserDao`); name the implementation class directly instead.

## 6. Grammar & Naming Conventions
- **Rule:** Adhere strictly to standard grammatical parts of speech based on code structure.
- **Classes / Objects:** Must use **nouns or noun-phrases** (e.g., `Customer`, `AddressParser`). Avoid generic suffixes like `Manager` or `Processor`.
- **Methods / Functions:** Must use **verbs or verb-phrases** (e.g., `postPayment`, `save`).
- **Booleans / Predicates:** Must read like a question and be prefixed with `is`, `has`, or `can` (e.g., `isAccountOpen`, `hasActiveSession`).

## 8. Java Naming Conventions

| Identifier Type | Naming Rule & Case Style | Typical Example | Notes |
| :--- | :--- | :--- | :--- |
| **Packages** | All lowercase, reverse domain notation | `com.example.order.service` | Avoid uppercase or underscores |
| **Classes & Interfaces** | PascalCase (UpperCamelCase), Noun/Noun phrase | `Customer`, `OrderProcessor`, `Runnable` | Must be nouns or noun phrases |
| **Methods** | camelCase, Verb/Verb phrase | `calculateTotal()`, `findUserById()` | Must start with a verb |
| **Variables (Instance/Local)** | camelCase, Descriptive noun | `userName`, `retryCount`, `orderId` | Avoid single letters or vague terms |
| **Constants** | UPPER_SNAKE_CASE (All caps, underscores) | `MAX_RETRY_LIMIT`, `DEFAULT_TIMEOUT_MS` | Must be static final |
| **Type Parameters (Generics)** | Single uppercase letter (typically) | `T` (Type), `E` (Element), `K` (Key), `V` (Value) | Use descriptive PascalCase if multi-char (`TService`) |
| **Enums** | PascalCase for type, UPPER_SNAKE_CASE for values | `public enum DayOfWeek { MONDAY, TUESDAY }` | Enum values are constants |
| **Annotations** | PascalCase, Noun or Adjective | `@Override`, `@Transactional`, `@NonNull` | Follow standard metadata appearance |
