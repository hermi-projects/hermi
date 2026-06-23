---
name: code-smells
description: >
  Detect, report, and fix code smells across all programming languages. The 4 primary triggers
  are: 'refactor' (action), 'code review' (task), 'hard to maintain' (symptom), 'clean up'
  (casual). Also trigger when the user pastes code and asks for feedback without a specific
  question, or asks 'is this good code?'. Covers all major smell categories with general rules
  for every language and language-specific deep rules.
metadata:
  version: "1.0.0"
---

# Code Smells

This skill detects and fixes code smells — patterns in code that indicate deeper design
problems. Smells are not bugs; they are signals that the code may be hard to read, test,
or change in the future.

## Language-Specific Rules

This skill has two layers:
1. **General rules** (this file) — apply to all languages.
2. **Language-specific rules** — read the relevant reference file when the language is known.

| Language | Reference File |
|---|---|
| Java | `references/java.md` |


**Always read the language-specific reference file** before reporting or fixing smells if
the target language is known. The language-specific file adds extra rules and overrides
thresholds where appropriate.

---

## Mode Detection

Determine the operating mode from the user's request:

| Trigger Phrase | Mode | Behavior |
|---|---|---|
| "review", "check", "audit", "scan", "find", "what smells" | **Detect Mode** | Read code, report violations with severity + location. Do NOT edit. |
| "fix", "refactor", "clean up", "remove smell", "improve" | **Fix Mode** | Apply fixes directly. Report each change with before/after. |
| *(no clear verb, or just code pasted)* | **Detect Mode** | Default to detection. Offer to fix at the end. |

---

## Smell Catalog — General Rules

These rules apply to all languages. Thresholds can be overridden by language-specific files.

### Category 1: Bloaters
Code that has grown so large it is hard to work with.

#### S01 — Long Method
```
IF: a method/function body exceeds 20 lines (excluding blank lines and comments)
THEN: flag as WARNING

IF: a method/function body exceeds 30 lines
THEN: flag as CRITICAL

WHY: Long methods accumulate over time — it's always easier to add than to extract.
     They are hard to name, hard to test, and hide duplicate code.
     Classes with short methods live longest.

SIGNAL: If you feel the need to add a comment inside a method body, that block of
        code is a candidate for extraction — even a single line deserves its own
        method if it requires explanation.

FIX (apply in order):
  1. **Extract Method** — pull the block into a new well-named method.
     After extraction, apply the **newspaper metaphor**:
     a. Place the **caller above the callee** — the high-level method reads top-to-bottom as a sequence of named steps, each one implemented below.
     b. Ensure the original method body reads as a **table of contents** — every line is a method call whose name tells you what it does. No inline logic.
     c. If the extracted methods don't form a natural top-to-bottom flow, you didn't extract at the right abstraction level — refactor again.
  2. If local variables block extraction:
     - Replace Temp with Query (compute it in-place instead of storing)
     - Introduce Parameter Object (group related params into an object)
     - Preserve Whole Object (pass the object instead of its fields)
  3. If none of the above work, use Replace Method with Method Object
     (move the entire method to its own class).
  4. For conditionals inside the method: use Decompose Conditional.
  5. For loops inside the method: Extract Method on the loop body.

PAYOFF:
  - Classes with short methods live longest. The longer a method is, the harder
    it becomes to understand and maintain.
  - Long methods are the perfect hiding place for unwanted duplicate code.
  - Performance note: extracting more methods has negligible performance impact
    in almost all cases — and cleaner code makes real optimizations easier to spot.
```

#### S02 — Large Class
```
IF: a class has too many fields, methods, or lines of code
THEN: flag as WARNING

WHY: Classes start small and grow bloated over time. Developers find it mentally
     easier to add a feature to an existing class than to create a new one.
     Large classes become impossible to remember in full and breed duplication.

SIGNAL: If a class is handling unrelated concerns, it is ready to be split.

FIX (choose based on what is being extracted):
  1. Extract Class — if part of the behavior can be spun off into a separate component.
  2. Extract Subclass — if part of the behavior is used only in rare cases or can be
     implemented in different ways.
  3. Extract Interface — if you need to define a contract of operations the client can use.
  4. Duplicate Observed Data — if the large class owns the UI/graphical interface:
     move data and behavior to a separate domain object, keeping the two in sync.

PAYOFF:
  - Developers no longer need to remember a large number of attributes for one class.
  - Splitting large classes avoids duplication of code and functionality.
```

#### S03 — Long Parameter List
```
IF: a method/function has more than 2 parameters
THEN: flag as WARNING

IF: more than 4 parameters
THEN: flag as CRITICAL

WHY: Long parameter lists grow from two sources:
     1. Multiple algorithms merged into one method, controlled by flags/switches.
     2. Efforts to decouple classes — data is passed in rather than looked up,
        but this creates a longer and longer list of "context" parameters.
     Such lists are hard to understand, easy to call in the wrong order, and
     signal that the method is doing too much or knows too much.

SIGNAL: If a method has boolean flags or "mode" parameters controlling its behavior,
        the method likely contains two or more merged algorithms — split it.

FIX (choose based on where the parameters come from):
  1. Replace Parameter with Method Call — if a parameter is just the result of
     calling another method, let the method call it directly instead.
  2. Preserve Whole Object — if several parameters all come from the same object,
     pass the object itself rather than its individual fields.
  3. Introduce Parameter Object — if parameters come from different sources but
     logically belong together, group them into a single named object.

PAYOFF:
  - More readable, shorter code.
  - Refactoring may reveal previously unnoticed duplicate code.

IGNORE IF: removing a parameter would create an unwanted dependency between classes.
```

#### S04 — Data Clumps
```
IF: the same group of 3+ variables/fields/parameters appear together in multiple places
THEN: flag as WARNING

WHY: Data clumps are usually caused by poor program structure or copy-paste programming.
     These grouped values belong together — they are a class waiting to be born.

SIGNAL: Delete one value from the group mentally. If the remaining values no longer
        make sense without it, the group is a data clump and should become an object.

FIX (choose based on where the clump appears):
  1. Extract Class — if the clumped fields are repeated across class definitions,
     move them into their own class.
  2. Introduce Parameter Object — if the same clump is passed repeatedly as method
     parameters, replace them with a single named object.
  3. Preserve Whole Object — if some of the clump's data is passed to other methods,
     pass the entire object rather than individual fields.
  4. Look at logic that uses these fields — it may belong inside the new data class.

PAYOFF:
  - Improves understanding and organization: related operations are now in one place.
  - Reduces code size.

IGNORE IF: passing the whole object instead of individual values would create an
           undesirable dependency between two classes.
```

#### S05 — Primitive Obsession
```
IF: a class uses primitive fields (e.g., string, int, double) to represent domain concepts like currency, ranges, phone numbers, or addresses
OR: a class/method uses primitive constants (e.g., USER_ADMIN = 1) to code type or category information
OR: a raw array/list is used to store diverse fields using index constants
THEN: flag as WARNING

WHY: Primitives lack behavior, validation, and domain meaning. Storing range, currency, or formatted strings as raw primitives spreads parsing, validation, and duplicate logic across the codebase, and prevents compiler-level type checking.

SIGNAL: Search for methods performing validation (e.g., length, regex, range checks) on the same primitive parameter or field in multiple places.

FIX (choose based on context):
  1. Replace Data Value with Object — group related primitive fields or behavior into a small Value Object.
  2. Replace Type Code with Class/Subclasses/State/Strategy — extract type code constants into an enum or class hierarchy.
  3. Replace Array with Object — convert raw arrays/lists used as data objects into proper classes with named fields.
  4. Introduce Parameter Object — combine multiple primitive parameters into a single parameter object.

PAYOFF:
  - Improves understanding and organization: value validation and behavior are contained in one place.
  - Increases flexibility: objects are easier to extend than raw primitives.
  - Enables type safety: prevents passing incorrect types (e.g., matching wrong IDs).
```
---

### Category 2: OOP Abusers
Incorrect or incomplete use of object-oriented design.

#### S06 — Switch Statements
```
IF: a method contains a complex switch statement or sequence of if-else statements branching on a type, mode, or category
THEN: flag as WARNING

IF: the same switch/if-else branching pattern appears in more than one class or method
THEN: flag as CRITICAL

WHY: Having type-based branching scattered across the codebase means adding a new type requires finding and modifying every single switch. Polymorphism localizes this behavior, so each type owns its own logic.

SIGNAL: Search for duplicate switch or if-else blocks that inspect the same type, enum, or property in different files or methods.

FIX (choose based on context):
  1. Replace Conditional with Polymorphism — if the switch is based on a type code, extract behavior into subclass implementations or a State/Strategy object.
  2. Extract/Move Method — if the switch needs to be isolated in its own helper method or moved to the class that owns the data being branched on.
  3. Replace Parameter with Explicit Methods — if the switch is simple and merely calls the same method with different parameters.
  4. Introduce Null Object — if one of the conditional branches handles a null or default case.

PAYOFF:
  - Improves code organization by consolidating branching behavior.
  - Simplifies adding new types since changes are localized to a single class rather than scattered switches.

IGNORE IF: the switch performs simple actions/mappings, or is used within a Factory pattern to select and instantiate a class.
```

#### S07 — Refused Bequest
```
IF: a subclass overrides a parent method only to throw an exception, return a dummy value, or do nothing
OR: a subclass uses only a small fraction of the methods and properties inherited from its parent class
THEN: flag as WARNING

WHY: Using inheritance purely for code reuse between completely different concepts creates an incorrect taxonomy. It forces the subclass to carry unneeded interfaces, which confuses readers and leads to runtime exception-throwing placeholders.

SIGNAL: Inspect overridden methods in a subclass to see if they throw `UnsupportedOperationException` or return default/empty values.

FIX (choose based on context):
  1. Replace Inheritance with Delegation — if the subclass has nothing conceptually in common with the superclass; hold an instance of the superclass as a field instead.
  2. Extract Superclass — if both classes share some behavior but subclass does not need all of it; extract commonalities into a new shared superclass and have both inherit from it.

PAYOFF:
  - Improves code clarity: clarifies class relationships (e.g., no dog class inheriting from a chair class).
  - Enhances maintenance: subclasses are not forced to implement or override irrelevant interfaces.
```

#### S08 — Temporary Field
```
IF: a class has fields that are only set and used under specific circumstances (such as during a complex algorithm execution) and remain null or empty the rest of the time
THEN: flag as WARNING

WHY: Class fields should represent the core state of an object throughout its entire lifecycle. Fields used only as temporary holders for complex calculations clutter the class, make the object's true state confusing to reason about, and lead to defensive null checks.

SIGNAL: Search for class fields that are accessed or mutated by only one method or a small subset of methods, and are typically initialized to null or empty.

FIX (choose based on context):
  1. Extract Class (or Replace Method with Method Object) — move the temporary fields and all methods operating on them into a new dedicated class.
  2. Introduce Null Object — replace the conditional null/existence checks on the temporary field with a null object implementation.

PAYOFF:
  - Improves code clarity: fields represent only the long-lived, genuine state of the object.
  - Better organization: segregates complex algorithms and their state from the main class.
```
#### S09 — Alternative Classes with Different Interfaces
```
IF: two or more classes perform functionally identical or highly similar tasks but have different method names or signatures
THEN: flag as WARNING

WHY: Having functionally equivalent classes with different interfaces increases duplication, bulks up the codebase, and confuses developers who won't know which class is the correct one to use.

SIGNAL: Search for classes that share identical behavior or logic blocks but use different naming conventions (e.g., `fetchData()` vs `retrieve()`, or `Client` vs `Customer`).

FIX (choose based on context):
  1. Rename Methods — rename methods to make them identical across all alternative classes.
  2. Align Signatures (Move Method / Add Parameter) — adjust signatures and logic until the implementations match.
  3. Extract Superclass — extract common behavior into a shared superclass or interface if they only partially overlap.
  4. Delete Redundant Class — delete the duplicate class once callers are redirected to the unified implementation.

PAYOFF:
  - Eliminates redundant code duplication.
  - Improves readability: developers no longer need to guess between two equivalent classes.

IGNORE IF: the classes are part of external/third-party libraries that cannot be modified.
```
---

### Category 3: Change Preventers
Code that makes a single logical change require edits in many places.

#### S10 — Divergent Change
```
IF: a single class has multiple unrelated reasons to change because it aggregates disjoint responsibilities (e.g., modifying product logic requires changing unrelated methods for finding, displaying, and ordering products)
THEN: flag as CRITICAL

WHY: Divergent change occurs when a class violates the Single Responsibility Principle (SRP). Keeping multiple unrelated concerns in the same class makes it highly complex, harder to test, and prone to regressions when modifying unrelated features.

SIGNAL: Identify methods within the class that use completely disjoint subsets of class fields, or observe if the class is modified in almost every commit for unrelated features.

FIX (choose based on context):
  1. Extract Class — split the behavior of the class into separate, highly cohesive classes.
  2. Extract Superclass / Extract Subclass — if different classes share some common behavior, organize them using a shared inheritance structure.

PAYOFF:
  - Improves code organization and localizes changes.
  - Reduces duplicate code.
  - Simplifies long-term maintenance and support.
```

#### S11 — Shotgun Surgery
```
IF: a single logical change or modification requires making many small edits across multiple classes (e.g., 4+ different classes/files)
THEN: flag as CRITICAL

WHY: Shotgun surgery means a single responsibility is fragmented across too many classes. This lack of cohesion makes the system fragile: any change is tedious to implement, and it is easy to forget an edit, leading to silent bugs.

SIGNAL: Search for past commits or pull requests where a single feature implementation touched a large number of disjoint files.

FIX (choose based on context):
  1. Move Method / Move Field — consolidate the scattered behavior and data fields into a single, cohesive class (create a new one if no suitable class exists).
  2. Inline Class — if moving the code leaves the original classes empty or redundant, merge them entirely into the receiving class.

PAYOFF:
  - Better code organization and localization.
  - Less code duplication.
  - Easier maintenance: a logical change now only requires editing one place.
```
#### S12 — Parallel Inheritance Hierarchies
```
IF: creating a subclass of one class forces you to create a matching subclass of another class
THEN: flag as WARNING

WHY: Parallel inheritance hierarchies increase maintenance overhead. For every new subclass added to one branch of the system, a corresponding subclass must be added to the parallel branch, resulting in tight coupling and duplication.

SIGNAL: Inspect class names and hierarchy structures for matching suffixes or prefixes (e.g., `Laptop` & `LaptopController`, `Desktop` & `DesktopController`).

FIX (choose based on context):
  1. Link Hierarchies — make instances of one hierarchy refer directly to instances of the other hierarchy.
  2. Move Method / Move Field — move all behavior and fields from the redundant hierarchy into the primary hierarchy, then delete the empty classes.

PAYOFF:
  - Eliminates class proliferation and duplication of subclass structures.
  - Improves code organization.

IGNORE IF: de-duplicating the hierarchies would produce even more complex or uglier architectural coupling.
```
---

### Category 4: Dispensables
Code that is unnecessary and should be removed.

#### S13 — Duplicate Code
```
IF: the same or near-identical block of code (typically 5+ lines) appears in 2+ methods, classes, or files
AND: the duplicate blocks share the same business concept or reason to change
OR: identical code blocks are executed across all branches of a conditional statement
THEN: flag as CRITICAL

WHY: Duplicate code forces developers to maintain logic in multiple places. Bug fixes or feature changes made in one spot must be manually copied to the others, leading to out-of-sync behavior, hidden bugs, and increased code bulk.

SIGNAL: Search for repeated sequences of statements, or use copy-paste detection tools to identify high-similarity blocks across files.

FIX (choose based on context):
  1. Extract Method — if the duplicate code is inside the same class, move it to a single helper method.
  2. Pull Up Field / Method / Constructor Body — if the duplication exists in subclasses of a common parent, move the shared fields/methods/constructor logic up to the superclass.
  3. Form Template Method — if subclasses have similar algorithms with slightly different steps, extract a template method in the superclass.
  4. Extract Superclass / Extract Class — if the duplication is in unrelated classes, introduce a shared superclass or extract the behavior into a separate collaborator class.
  5. Substitute Algorithm — if two methods accomplish the same goal with different algorithms, replace the complex version with the simpler, unified algorithm.
  6. Consolidate Conditional (Expression / Fragments) — merge multiple conditions that do the same thing, or move duplicate statements outside branch trees.

PAYOFF:
  - Shorter and simpler code structure.
  - Easier maintenance and cheaper support (bugs only need to be fixed in one location).

IGNORE IF: the duplicate fragments represent different business concepts and have different reasons to change (coincidental duplication), or merging them reduces readability.
```

#### S14 — Dead Code
```
IF: a variable, parameter, field, method, class, import, or conditional branch is never referenced, executed, or used
THEN: flag as WARNING

WHY: Obsolete code adds noise and increases cognitive load, wasting developers' time as they try to understand or maintain code that has no effect. Unreachable branches or unused parameters also mask design flaws and integration bugs.

SIGNAL: Use IDE reference-finding tools or static analysis checkers to verify if a declaration has zero active incoming references, or if a branch condition is statically impossible to reach.

FIX (choose based on context):
  1. Delete Code / Files — remove the unused local variable, private field, method, import, or file.
  2. Remove Parameter — remove the unused method parameter and update all invocation signatures.
  3. Inline Class / Collapse Hierarchy — if the unused class is part of a hierarchy or acts as a redundant middleman, merge its behavior into its subclass/superclass or caller.

PAYOFF:
  - Shrunk code size and clutter.
  - Simpler maintenance: developers focus only on active code paths.
```

#### S15 — Speculative Generality
```
IF: code abstractions (e.g., abstract classes, interfaces), methods, parameters, or fields exist solely to support anticipated future features with no active production callers
THEN: flag as INFO

WHY: Speculative generality violates the YAGNI ("You Aren't Gonna Need It") principle. Building abstractions or hooks for future features that might never be implemented unnecessarily complicates the codebase, hides design intent, and increases maintenance costs.

SIGNAL: Look for interfaces or abstract base classes with exactly one subclass/implementation, or parameters/methods that have zero production references.

FIX (choose based on context):
  1. Collapse Hierarchy — if an abstract class or interface has only one concrete subclass, merge them.
  2. Inline Class / Inline Method — eliminate speculative classes or methods by merging them directly into their caller.
  3. Remove Parameter — remove unused parameters from the method signature.
  4. Delete Unused Fields — safely delete fields that are defined but never read.

PAYOFF:
  - Slimmer, cleaner codebase with less indirection.
  - Easier maintenance and support.

IGNORE IF: the code is part of a public library/framework API designed for external clients, or the fields/methods are strictly used by unit tests to access or verify internal state.
```

#### S16 — Lazy Class
```
IF: a class has very little responsibility, delegates all of its logic to collaborators, or has become ridiculously small due to refactoring
THEN: flag as INFO

WHY: Every class in a codebase costs time and effort to understand, test, and maintain. If a class does not do enough work to justify its existence, keeping it adds useless indirection and bloats the program structure.

SIGNAL: Scan for classes that have very few lines of code, only delegate methods, or whose subclass implements almost no custom behavior.

FIX (choose based on context):
  1. Inline Class — move all fields and methods from the lazy class directly into its primary client/caller class, then delete it.
  2. Collapse Hierarchy — if the lazy class is a subclass or superclass with minimal distinct logic, merge it into the other class in the hierarchy.

PAYOFF:
  - Reduced codebase size and fewer files to manage.
  - Simplified program navigation and reduced indirection.

IGNORE IF: the class is created as a temporary placeholder to delineate architectural intentions for upcoming feature work.
```
#### S17 — Comments
```
IF: a method or code block is filled with inline explanatory comments detailing what the code does or how it does it
THEN: flag as WARNING

WHY: Comments are often written with good intentions but serve as a "deodorant" masking poorly named, overly complex, or unorganized code. Code should be self-documenting; if it requires comments to explain what it does, the structure needs refactoring.

SIGNAL: Scan code for inline comments that explain statements or block logic rather than explaining the high-level API contract or the "why" behind an implementation decision.

FIX (choose based on context):
  1. Extract Method — extract a commented block of code into its own well-named helper method (using the comment content to guide the name).
  2. Extract Variable — break down complex commented expressions into smaller, named intermediate variables.
  3. Rename Method — rename the method to be descriptive if comments are still needed to explain what it does.
  4. Introduce Assertion — replace comments detailing state assumptions with run-time assert statements.

PAYOFF:
  - Code becomes cleaner, more intuitive, and self-documenting.
  - Reduces the risk of "comment rot" where comments and implementation drift apart over time.

IGNORE IF: comments explain the "why" (rationales, business logic constraints) or document highly complex algorithms that cannot be simplified further.
```

#### S18 — Duplicate Documentation
```
IF: the same comment or documentation appears in multiple locations (methods, classes, files)
THEN: flag as WARNING

WHY: When documentation is copied and pasted, it inevitably becomes outdated in some locations while staying current in others. This creates a maintenance nightmare where developers rely on wrong or outdated explanations, leading to confusion and bugs.

SIGNAL: Use text search to find identical or near-identical comments in different parts of the codebase. If the same explanation appears in 3+ locations, it's duplicate documentation.

FIX (choose based on context):
  1. Move documentation to a shared location — create a single source of truth (e.g., a method doc, class-level comment, or dedicated documentation file)
  2. Reference from other locations — instead of copying, use a short reference like `// See ProcessOrder for detailed explanation`
  3. Extract to a shared helper — if the documentation explains a reusable pattern, extract it into a method with the explanation in its Javadoc

PAYOFF:
  - Documentation stays consistent — no conflicting explanations
  - Updates are localized — change in one place, automatically reflected everywhere
  - Reduces maintenance burden — less text to keep in sync

IGNORE IF: the documentation explains version-specific behavior (e.g., performance characteristics under different conditions) and is intentionally duplicated for reference
```

#### S19 — Data Class
```
IF: a class consists entirely of fields (public or private with boilerplate getters/setters) and contains zero methods that operate on its own data
THEN: flag as WARNING

WHY: Data classes are containers, not objects. They expose their internals and force all behavior into client code, which leads to scattered logic, duplicated operations, and missed encapsulation opportunities. The true power of objects is combining data with behavior.

SIGNAL: Scan the class's method list. If every method is either a getter, a setter, or a framework-required override (equals/hashCode/toString), and you can find operations on this class's fields scattered across 3+ client files, the class is a data class.

FIX (choose based on context):
  1. Encapsulate Field — if fields are public, make them private with getters/setters
  2. Move Method + Extract Method — review client code and pull operations that manipulate this class's data into the class itself
  3. Remove Setting Method + Hide Method — after behavior is moved in, narrow or remove overly broad accessors

PAYOFF:
  - Operations on the same data are gathered in one place, eliminating scattered duplication
  - Client code becomes thinner and more expressive — callers say "what" not "how"

IGNORE IF: The class is a DTO, API contract, or framework-required POJO that intentionally carries data across a boundary (serialization boundary, network layer, ORM entity)
```
---

### Category 5: Couplers
Excessive coupling between classes.

#### S20 — Feature Envy
```
IF: a method accesses data or calls methods from another class more than from its own class
THEN: flag as WARNING

WHY: If things change at the same time, they should be in the same place. A method that is more interested in another class's data means the method belongs there — keeping it elsewhere scatters related logic, creates duplication, and makes the data class an anemic container.

SIGNAL: Pick a method and count references to `this` fields vs references to another class's fields/methods. If the other class wins, it's Feature Envy.

FIX (choose based on context):
  1. Move Method — if the entire method clearly belongs on the other class, move it there directly
  2. Extract Method — if only part of the method accesses the other class's data, extract that part and move it
  3. Split via Extract Method — if the method uses data from several classes, split it so each piece lives in the class whose data it uses most

PAYOFF:
  - Less code duplication: data handling code is centralized next to the data
  - Better organization: methods that operate on data live with the data

IGNORE IF: the behavior is intentionally separated from the data to allow dynamic strategy/behavior swapping (e.g., Strategy, Visitor, or similar patterns)
```

#### S21 — Message Chains
```
IF: code calls a chain of 3+ getters/methods: a.getB().getC().getD()
THEN: flag as WARNING

WHY: Message chains couple the caller to every object in the navigation path — any change in the intermediate types or relationships requires modifying the caller. This creates fragile code that breaks when the class structure changes.

SIGNAL: Search for dot-chains that access intermediate objects solely to reach a deeper object. If removing any intermediate link breaks the chain, it's a message chain.

FIX (choose based on context):
  1. Hide Delegate — add a method on the first object that directly returns what the caller needs, hiding the navigation
  2. Extract Method + Move Method — if the end result is a meaningful operation, extract it as a method on the first object in the chain

PAYOFF:
  - Reduces dependencies between classes in the chain
  - Reduces bloated, fragile code

IGNORE IF: hiding the delegation would introduce Middle Man — overly aggressive hiding can make it hard to see where functionality lives
```

#### S22 — Middle Man
```
IF: more than 50% of a class's public methods do nothing but delegate to another class
THEN: flag as INFO

WHY: A class that only delegates adds indirection without value — every reader must chase the calls to understand what actually happens. This is often the leftover shell after real behavior has been moved elsewhere, or the result of overzealous Message Chain elimination.

SIGNAL: Count public methods that simply forward the call to another class. If the majority do nothing but delegate, the class is a middle man.

FIX (choose based on context):
  1. Remove Middle Man — inline the delegation into the caller, or merge the middle man into the class it delegates to
  2. Replace with Delegation — if the middle man exists for a valid architectural reason, keep it but document the intent

PAYOFF:
  - Less bulky code and fewer indirection layers
  - Readers can see directly what the code does without chasing delegates

IGNORE IF: the middle man serves a deliberate purpose — avoiding interclass dependencies (Proxy), adding cross-cutting behavior (Decorator), or as part of a deliberate design pattern
```

#### S23 — Inappropriate Intimacy
```
IF: two classes access each other's private/internal fields or methods directly
OR: two classes have bidirectional dependencies
THEN: flag as CRITICAL

WHY: Classes should know as little about each other as possible. Intimate classes are tightly coupled — changes to one ripple to the other, they cannot be tested or reused independently, and the relationship makes the design brittle.

SIGNAL: Look for classes that import each other (bidirectional), or inspect usages of getters/setters across class boundaries — if class A repeatedly reaches into class B's internals, and vice versa, it's inappropriate intimacy.

FIX (choose based on context):
  1. Move Method / Move Field — move the parts that one class uses from the other class into the class that needs them
  2. Extract Class + Hide Delegate — make the shared data an official collaborator with a clean interface
  3. Change Bidirectional Association to Unidirectional — if the classes are mutually interdependent, break one direction
  4. Replace Delegation with Inheritance — if the intimacy is between a subclass and superclass, consider whether inheritance should replace the back-and-forth

PAYOFF:
  - Improves code organization and reduces coupling
  - Simplifies testing, maintenance, and reuse
```
#### S24 — Incomplete Library Class
```
IF: a third-party library or framework class lacks a feature or method you need, and you cannot modify the library source
THEN: flag as WARNING

WHY: Libraries are read-only — when they don't meet your needs, the missing functionality tends to get scattered across client code as ad-hoc helper methods or utility classes, creating duplication and maintenance burden.

SIGNAL: Search for static utility methods or helper classes that exist solely to compensate for a missing feature in a library class. If you find 2+ such helpers for the same library class, it's an incomplete library class.

FIX (choose based on scope):
  1. Introduce Foreign Method — if you only need one or two additional methods, add them as static methods in a client-side utility
  2. Introduce Local Extension — if you need significant changes, create a subclass or wrapper that extends the library class with the missing behavior

PAYOFF:
  - Reduces code duplication: missing functionality is centralized in one extension rather than scattered across callers
  - You can still benefit from existing library functionality without forking it

IGNORE IF: the extension would be so extensive that writing a standalone replacement is simpler, or the library is expected to add the feature soon
```

---

### Category 6: Readability
How code reads at the body and expression level — naming, literals, conditionals, and formatting.

#### S25 — Vertical Separation
```
IF: related methods (caller-callee pairs) are not kept close together in the file, or variable declarations are far from their first usage
THEN: flag as WARNING

WHY: Code that reads top-to-bottom follows a natural hierarchy of abstraction. When related concepts are separated by long blocks of unrelated logic, readers cannot follow the thread — they must scroll and search constantly. This adds cognitive overhead and obscures whether code is well-structured.

SIGNAL: Open a file and scroll — if you need to jump back and forth to trace a call chain or find where a variable was declared, vertical separation is violated.

FIX (choose based on context):
  1. Group caller above callee — place the calling function directly above the function(s) it calls
  2. Declare variables near first use — move variable declarations from the top of the method to just before they're first used
  3. Extract and regroup — if a method has multiple unrelated sections that prevent grouping, extract each section into its own well-named method, then group those methods by dependency order

PAYOFF:
  - Files read like newspaper articles — high-level story first, details below
  - Readers can follow the thread without jumping around
  - Encourages better method factoring — extraction becomes natural
```

#### S26 — Contradictory Naming
```
IF: a class, method, or variable is named in a way that misrepresents its behavior, scope, or responsibilities
THEN: flag as WARNING

WHY: Name mismatches are insidious — they silently mislead developers during maintenance. A method named `getUser()` that actually modifies state, a class named `Utils` that contains business logic, or a flag named `enabled` that actually means `disabled` create cognitive dissonance that leads to bugs.

SIGNAL: Scan code for common mismatch patterns: methods that mutate state but sound like queries (e.g., `get*` that modify), nouns or adjectives that misrepresent what the code does (`*Manager` that doesn't manage, `*Utils` that has opinionated logic), or boolean/status flags that invert meaning.

FIX (choose based on context):
  1. Rename the element — change the name to match the behavior (e.g., `getUser()` -> `updateUser()`, or `enabled` -> `disabled`)
  2. Extract the contradictory logic — if a method does more than its name implies, extract the unexpected behavior into a new method with the correct name, then rename the original or keep it as a thin wrapper
  3. Use documentation — if renaming is impractical (e.g., public API), add a detailed Javadoc/comment explaining the mismatch

PAYOFF:
  - Code becomes self-documenting — names accurately reflect behavior
  - Maintenance becomes easier — no cognitive traps or hidden meanings
  - Refactoring is safer — changes are easier to reason about

IGNORE IF: the mismatch is within a private method and all call sites are within the same class, or the name follows a well-known idiom where the deviation is documented (rare)
```

#### S27 — Obscure Code
```
IF: code relies on deep nesting, complex conditional logic, or convoluted control flow that makes it difficult to understand without tracing
THEN: flag as WARNING

WHY: "Obscure code is like a poorly lit room — you can't tell what's inside until you turn on the lights. When code is hard to follow, developers avoid touching it, leading to stagnation. The goal is to write code that reads like a story, not a puzzle."

SIGNAL: Scan for:
  - Methods with 3+ levels of nested `if`/`else`/`while`/`for` blocks
  - Functions with 30+ lines of code that perform multiple operations
  - Complex boolean expressions with multiple `&&`, `||`, and `!` operators
  - Nested ternary operators that span multiple lines

FIX (choose based on context):
  1. Extract conditions into well-named methods — `if (isValid(user) && !isSuspended(user) && hasPermission(user))` becomes `if (isEligible(user))`
  2. Use guard clauses — handle edge cases first and return early, reducing nesting depth
  3. Break down complex functions — split long functions into smaller, single-responsibility methods
  4. Simplify boolean expressions — use De Morgan's laws or extraction to clarify complex conditions

PAYOFF:
  - Code becomes easier to understand — each method tells a clear story
  - Testing becomes easier — small methods have fewer branches
  - Refactoring becomes safer — isolated logic is easier to change

IGNORE IF: the logic is a standard algorithm that is widely known and concise, or the complexity is isolated within a private helper method
```

#### S28 — Logging Intrusion
```
IF: business logic methods contain logging statements interleaved with domain operations at a ratio of 2+ log lines per business operation, or log statements are placed inside loops that execute on every iteration
THEN: flag as WARNING

WHY: Logging is infrastructure, not domain logic. When logging statements are scattered throughout business methods, they act as "code comments" that distract from the real narrative — readers must mentally skip log lines to follow the flow. This violates vertical separation and makes business logic harder to read, test, and reuse.

SIGNAL: Scan method bodies for alternating lines of `logger.*` calls and domain operations. If removing all log lines produces a cleaner, more readable method, the logging is intrusive.

FIX (choose based on context):
  1. Wrap at the boundary — log only at the method entry/exit, not mid-operation. Use an AOP aspect or a wrapper method that logs before/after delegating to the pure business method
  2. Extract to the orchestration layer — move logging to the controller/service caller, keeping domain methods log-free
  3. Use the newspaper metaphor — pull logging to the "footnote" level: high-level business logic at the top, infrastructure/logging extracted to separate methods or aspects below
  4. Use AOP — for cross-cutting logging (performance, tracing, audit), annotate the method with a custom `@Logged` annotation and let an aspect handle it

PAYOFF:
  - Business logic reads cleanly — no infrastructure noise
  - Domain methods are testable without mocking loggers
  - Logging behavior is centralized and consistent, not ad-hoc

IGNORE IF: the log statements document critical audit/regulatory events and must appear inline for compliance traceability
```

#### S29 — Magic Literals
```
IF: a numeric, string, or boolean literal value appears directly in code and its meaning is not self-evident from context
THEN: flag as CRITICAL

WHY: Raw literals force the reader to guess what the value means. If the same value appears in multiple places, changing it requires hunting down every occurrence. Named constants turn magic numbers into self-documenting intent.

SIGNAL: Scan for any literal that is not 0, 1, or a standard mathematical constant — if you would need a comment to explain what the number means, it needs a named constant.

FIX (choose based on context):
  1. Replace with a named constant — `private static final int MAX_RETRIES = 7` instead of `if (retries > 7)`
  2. Replace with an enum — if the literal represents a category or type, use a named enum value
  3. Extract to a configuration file — if the value changes per environment (timeouts, limits), move it to config

PAYOFF:
  - Code becomes self-documenting — names explain intent
  - Single source of truth — change one constant instead of every occurrence
  - Prevents bugs from inconsistent values

IGNORE IF: the literal is self-explanatory (e.g., basic math `n * 2`, unit conversion `minutes / 60`, or standard API usage)
```

#### S30 — Encapsulate Conditionals
```
IF: a method contains compound boolean expressions (two or more conditions joined by && or ||) inside a control flow statement (if, while, for), or a single boolean expression whose intent is not immediately obvious from the condition itself
THEN: flag as WARNING

WHY: Raw boolean conditions encode what is being checked but not why. The reader must mentally parse each sub-expression and infer the combined intent. Extracting the condition into a well-named method makes the calling code read like prose and lets the condition be reused, tested, and changed independently.

SIGNAL: Scan if/while statements — if you need a comment to explain what the condition means, or if the expression spans more than one line, extract it.

FIX (choose based on context):
  1. Extract the condition to a private method — name the method for the intent: `if (isEligibleForPromotion())` instead of `if (employee.getTenure() > 2 && !employee.isOnProbation() && employee.getRating() >= 4)`
  2. Extract into a local variable — if the condition is only used once and extracting a method would be overkill, name the expression: `boolean isExpiredButNotRecurring = timer.hasExpired() && !timer.isRecurrent()`
  3. Combine with Decompose Conditional — break complex conditionals by extracting both the condition and each branch into their own methods

PAYOFF:
  - Calling code reads at a single level of abstraction — conditions become intent-revealing method calls
  - Extracted conditions are independently testable
  - Reduces duplication when the same compound condition appears in multiple places

IGNORE IF: the expression is trivially simple (e.g., `if (x == null)` or `if (count > 0)`) and extracting would add noise
```

#### S31 — Avoid Negative Conditionals
```
IF: a control flow statement (if, while, ternary) or boolean assignment contains a negated expression (`!isNotX`, `!shouldNotY`) where a positive alternative exists, or a method/field is named with a negative prefix (`isNotDisabled`, `shouldNotSkip`)
THEN: flag as WARNING

WHY: Negation forces a mental double flip — the reader computes "not not-full = is-full" before understanding the check. Over time this compounds into subtle reading errors, especially when negations are negated again or combined with compound conditions. Positive names and expressions let the brain parse intent directly.

SIGNAL: Check every boolean expression in if/while/ternary — if it starts with `!`, ask whether the concept it negates has a positive name. If yes, invert the condition and rename.

FIX (choose based on context):
  1. Rename the predicate — replace `isNotDisabled()` with `isEnabled()`, `shouldNotSkip()` with `shouldProcess()`, `isNotFull()` with `isFull()`
  2. Invert the condition — change `if (!isValid())` to `if (isInvalid())` only if an `isInvalid()` method already exists and reads naturally; otherwise keep the negation and rename the variable instead
  3. Wrap at the call site — if you cannot change the source (e.g., third-party API that returns `isNotExpired()`), wrap it: `private boolean isExpired() { return !api.isNotExpired(); }`
  4. Restructure with early return — replace `if (!isEligible()) { complex logic }` with `if (isEligible()) { complex logic }` by inverting the body or returning early

PAYOFF:
  - Code reads at a natural pace — no mental double flips
  - Reduces operator errors when negations stack in compound conditions
  - Lowers cognitive load during code review

IGNORE IF: no positive alternative exists and the negation is idiomatic (e.g., `if (list.isEmpty())` is the positive form; `if (a != b)` has no meaningful positive alternative)
```

#### S32 — Non-Compliant Naming
```
IF: the meaning of a name cannot be inferred from the name alone and requires reading a comment, the surrounding code, or knowing implicit context to understand what the identifier represents
THEN: flag as WARNING

WHY: Names are the primary communication tool in code. If a name does not reveal intent, every future reader must spend mental effort to decode it. This violates Clean Code's core chapter (Chapter 2: Meaningful Names) — a name should answer "what" and "why", not require the reader to piece it together.

SIGNAL: For each name encountered, ask: "If I delete all comments, is this name still meaningful?" If the answer is no, the name does not reveal intent.

FIX:
  1. Rename to reveal intent — replace `int d; // elapsed time in days` with `int elapsedTimeInDays`
  2. If the name describes mechanism rather than purpose, rename to the business intent — `fetchFromDb(id)` → `getCustomer(id)`
  3. If the name is too generic (`data`, `info`, `val`), rename to describe what it specifically holds — `data` → `unprocessedOrderBatch`

PAYOFF:
  - Code becomes self-documenting — comments become optional rather than required
  - Readers can understand intent without tracing control flow

IGNORE IF: the name follows a well-known domain or team convention that makes the intent obvious to everyone in that context (e.g., `ctx` in a Spring handler method)
```

#### S33 — Output Arguments
```
IF: a method mutates a parameter (collection, StringBuilder, array, or mutable object) to produce its result rather than returning a value
THEN: flag as CRITICAL

WHY: Readers naturally assume arguments are inputs. An argument that is mutated as a side effect hides the function's true behavior at the call site — you cannot tell `appendFooter(s)` from `s.appendFooter()` without reading the body. This violates the principle: a function should either DO something or RETURN something, not both.

SIGNAL: At the call site, scan for variables passed into a method that are non-void. After the call, check if that variable's state has changed — if so, the argument is an output argument.

FIX (choose based on context):
  1. Make the method on the object itself — if the mutated object `s` owns the method, call `s.method()` instead of passing `s` in
  2. Return the result — if the method computes something, return the new value rather than modifying a parameter
  3. Use a Builder — if the method accumulates results over several calls, extract a builder object that accumulates and returns the final product

PAYOFF:
  - Call sites become self-documenting — you can see what a function does without reading its body
  - Functions that return values are easier to test and compose

IGNORE IF: the pattern is explicitly documented as a fluent interface, or the language idiom expects it (e.g., `Collections.sort()` mutates in place by convention)
```

#### S34 — Flag / Selector Arguments
```
IF: a method takes a boolean, string, enum, or int parameter whose primary purpose is to select between two or more distinct code paths or behaviors within the method
THEN: flag as CRITICAL

WHY: Boolean flags (`book(true)`) carry no semantic meaning — the reader cannot tell what "true" means without digging into the body. String/enum selectors (`format("PDF")`, `type(1)`) are slightly more readable but still hide the same problem: the method does N things instead of one. This violates Single Responsibility and requires testing all branches on every call site.

SIGNAL: Search for methods called with a literal `true`/`false`, a string constant that selects a mode, or an enum/int constant that controls behavior. If you cannot infer what the argument means from the method name alone, it's a flag or selector argument.

FIX (choose based on context):
  1. Split into separate methods — replace `book(true)` with `bookRegular()` and `bookPremium()`, or `format("PDF")` with `formatAsPdf()` and `formatAsHtml()`
  2. Extract the selector to the caller — the caller decides which method to call, not the callee
  3. Replace with enum + polymorphism — if splitting produces too many methods, use a Strategy or Visitor pattern where each variant is its own class
  4. Extract the flagged logic — if the flag controls a small inline branch, extract that branch into a separate method called independently

PAYOFF:
  - Call sites become self-documenting — the method name says what happens
  - Each code path is independently testable and changeable
  - No hidden branches waiting to be missed

IGNORE IF: the method is a Factory, Builder, or Strategy resolver whose purpose IS to select an implementation; or a private helper where all call sites are visible in the same class
```

#### S35 — Incorrect Behavior at Boundaries
```
IF: a method does not validate its inputs at the boundary (null checks, range checks, invariants), OR it returns null unexpectedly, OR it swallows exceptions instead of failing fast
THEN: flag as CRITICAL

WHY: Code that fails to guard its boundaries is unpredictable — callers cannot trust the contract. Invalid input may corrupt internal state, produce silent garbage, or crash with an unhelpful stack trace miles away from the actual cause. Every function boundary should be a hard shell that catches and reports violations immediately.

SIGNAL: Look for methods that accept parameters but perform no validation before using them, methods that return null in non-optional signatures, or empty catch blocks that swallow exceptions silently.

FIX (choose based on context):
  1. Validate at the boundary — add precondition checks (null checks, range checks, invariant assertions) at the method entry before any logic executes
  2. Replace null returns with Optional or a Null Object pattern — make the return type honest about what it can be
  3. Fail fast — replace empty catch blocks with specific exception handling, or rethrow wrapped exceptions so the caller knows what went wrong
  4. Use assertions — document invariants that must hold after the method executes, and enforce them with assert or a validation framework

PAYOFF:
  - Bugs are caught at the source, not downstream where diagnosis is harder
  - Callers can trust the contract — no silent failures or mysterious null pointers
  - Failures are loud and immediate, making them easier to fix

IGNORE IF: the boundary is framework-generated code (e.g., serialization constructors, JPA entities) or performance-critical hot paths where every check is measured and accounted for
```

#### S36 — Inappropriate Static
```
IF: a public or protected method is declared static but uses only its parameters (no `this` state), and there is any scenario where polymorphic behavior or testability would be beneficial
THEN: flag as WARNING

WHY: Static methods are compile-time bound — they cannot be overridden, mocked, or swapped. Making a method static just because "it doesn't use `this`" bakes in inflexibility. The moment you need different behavior in a subclass or test, you must refactor the callers too. Prefer instance methods by default; static is a commitment.

SIGNAL: Look for public static methods that are not pure utilities (e.g., not `Math.max`, `Collections.sort`). If you cannot confidently say "no one will ever need to override this", the static is inappropriate.

FIX (choose based on context):
  1. Convert to instance method — remove `static`, use the method normally
  2. Extract to a strategy — if the static method encapsulates an algorithm, extract it into a standalone strategy class that can be injected and overridden
  3. Inject as a dependency — if tests need to replace the behavior, make it an instance method on a class that callers receive via dependency injection

PAYOFF:
  - Methods stay open for extension — subclasses and tests can override behavior
  - Dependency injection becomes possible — no hidden compile-time coupling
  - Testing is simpler — mock or subclass instead of working around static calls

IGNORE IF: the method is a pure utility function (no reasonable need for polymorphism), a private helper within the same class, a factory method, or a performance-critical hot path where virtual dispatch is a measured concern
```

## Severity Guide

| Severity | Meaning | Example |
|---|---|---|
| **CRITICAL** | Significant design violation. Address soon. High risk of bugs, regressions, or test failures. | Duplicate code, Inappropriate Intimacy, Shotgun Surgery, Output Arguments |
| **WARNING** | Design concern. Should be improved, but not urgent. | Feature Envy, Temporary Field, Long Parameter List, Vertical Separation, Contradictory Naming, Obscure Code, Duplicate Documentation |
| **INFO** | Minor or speculative. Worth noting, low urgency. | Lazy Class, Speculative Generality |

---

## Detect Mode Output Format

Report smells using this exact structure:

```
## Code Smell Report

### CRITICAL
- [S13] Duplicate Code — `processOrder()` in OrderService and `handleOrder()` in CartService
  FILE: src/OrderService.java:L45–L67
  FIX: Extract shared logic to `OrderProcessor.execute(order)`

### WARNING
- [S03] Long Parameter List — `createUser(name, email, phone, role, deptId, isAdmin)` has 6 params
  FILE: src/UserFactory.java:L12
  FIX: Introduce `UserCreationRequest` parameter object

### INFO
- [S15] Speculative Generality — `AbstractReportFormatter` has one subclass and no planned extensions
  FILE: src/reports/AbstractReportFormatter.java
  FIX: Inline into `PdfReportFormatter` until a second format is needed

---
Total: 1 CRITICAL, 1 WARNING, 1 INFO
```

At the end of a Detect Mode report, always offer:
> "Would you like me to fix any of these? Say 'fix all', 'fix CRITICAL', or name a specific smell."

---

## Fix Mode Output Format

For each fix applied, output one block:

```
FIXED [S13] Duplicate Code
FILE: src/OrderService.java:L45 + src/CartService.java:L88
BEFORE: (duplicated block in both files)
AFTER: Extracted to `src/shared/OrderProcessor.java#execute(Order)`
```

---

## Self-Check Before Reporting

Before emitting a report, verify:
1. Have you loaded the language-specific reference file if the language is known?
2. Did you apply language-specific thresholds (they override general ones)?
3. Did you check ALL 7 smell categories, not just the obvious ones?
4. Are your file:line references accurate?
5. Did you group by severity (CRITICAL first)?
