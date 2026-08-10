# Law of Demeter

> **Core Rule:** A module should not know about the internal details of the objects it manipulates. Talk only to your immediate friends and not to strangers.

## 1. The Principle of Least Knowledge
- **Rule:** Restrict method interactions to direct associations to reduce coupling.
- **Constraint:** An object method should only invoke methods of:
  1. The object itself.
  2. Objects passed as parameters.
  3. Objects created within the method.
  4. Direct instance variables/components.

## 2. Avoiding Train Wrecks
- **Rule:** Prohibit long method chaining that exposes internal structures across boundaries.
- **Constraint:** Chained calls like `app.getConfig().getDatabase().getConnection().execute()` are **forbidden**. Break chains or delegate behavior to the immediate object (e.g., `app.executeDatabaseQuery()`).
