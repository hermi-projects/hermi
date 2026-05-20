---
name: code-naming
description: Strictly enforce Clean Code naming standards on all identifiers. Apply when writing, reviewing, or refactoring code.
compatibility: Works across all platforms supporting the agentskills.io spec (Claude Code, GitHub Copilot, Cursor).
metadata:
  author: hermi-team
  version: "1.3.0"
---

## Mode Detection

Determine operating mode from the user's request:

| Trigger Phrase | Mode | Behavior |
|---|---|---|
| "review", "check", "audit", "scan", "find issues" | **Review Mode** | Read files, report violations with file:line references, suggest fixes. Do NOT edit. |
| "fix", "rename", "refactor", "clean up" | **Refactor Mode** | Apply fixes directly. Report each change with before/after. |
| "write", "generate", "create", "implement" | **Write Mode** | Apply rules to generated code before output. Run self-correction loop internally. |

## Identifier Evaluation Algorithm

For each identifier encountered, execute these checks in order. Stop at the first failure:

```
1. SCOPE_CHECK: Is scope > 5 lines AND name length < 2 chars? → FAIL
2. HUNGARIAN_CHECK: Does name start with a type-encoding prefix? → FAIL
   (Prefixes: str, i, b, l, f, d, m_, s_, arr, ptr, p, v, tbl, sz)
3. ABBREVIATION_CHECK: Contains non-standard abbreviation? → FAIL
   (Allowed: id, uuid, url, uri, http, sdk, api, io, xml, json, html, css, js)
4. NOUN_CHECK (classes only): Is the class name not a noun/noun-phrase? → FAIL
5. VERB_CHECK (methods only): Does the method name lack a verb? → FAIL
6. SEARCHABILITY_CHECK: Would grep for this exact name return > 50 false positives? → FAIL
7. MISLEADING_CHECK: Does the name imply a type or concept that differs from the declared type? → FAIL
8. CONTEXT_CHECK: Is the name a bare noun ({size, count, name, id, type, value, key, status, code, date, path}) in a multi-entity scope? → FAIL
9. COMMENT_SMELL_CHECK: Does understanding this name require reading a comment? → FAIL
```

## Rule Catalog

Each rule follows the pattern: **IF** condition **THEN** action. Apply mechanically.

### R1: Single-Letter & Generic Names

```
IF: identifier is a variable, parameter, or field
AND: ((scope > 5 lines AND length < 2 chars)
     OR name ∈ {d, data, info, list, map, obj, val, tmp, temp, str, num, flag, item, thing, stuff})
THEN: rename to describe WHAT it holds, including context
```

| Before | After |
|---|---|
| `d` | `elapsedTimeInDays` |
| `list` | `activeSubscribers` |
| `data` | `unprocessedOrderBatch` |
| `info` | `customerContactDetails` |

**Exception**: Loop index variables with scope ≤ 5 lines (`i`, `j`, `k`) are permitted. Stream lambda parameters with scope ≤ 3 lines (`x`, `e`) are permitted.

### R2: Abbreviation Expansion

```
IF: identifier contains a substring matching a known abbreviation
AND: substring ∉ allowed-abbreviation-list
THEN: expand to full word
```

Known abbreviation → full word mapping:
| Abbrev | Expand To | Abbrev | Expand To |
|---|---|---|---|
| `idx` | `index` | `exc`/`ex` | `exception` |
| `ctx` | `context` | `gen` | `generate`/`generation` |
| `req` | `request` | `resp` | `response` |
| `impl` | `implementation` | `spec` | `specification` |
| `cfg`/`conf` | `configuration` | `init` | `initialize` |
| `db` | `database` | `repo` | `repository` |
| `svc` | `service` | `mgr` | `manager` |
| `stmt` | `statement` | `tx`/`txn` | `transaction` |
| `pwd`/`pword` | `password` | `usr` | `user` |
| `addr` | `address` | `desc` | `description` |
| `calc` | `calculate` | `exec` | `execute` |
| `max`/`min` | `maximum`/`minimum` | `prev`/`cur`/`next` | `previous`/`current`/`next` |

### R3: Hungarian Notation

```
IF: identifier starts with a lowercase type-encoding prefix
AND: prefix ∈ {str, i, b, l, f, d, m_, s_, arr, ptr, p, v, sz}
THEN: strip the prefix
```

| Before | After |
|---|---|
| `strName` | `name` |
| `iCount` | `count` |
| `bIsActive` | `isActive` |
| `m_member` | `member` |
| `arrItems` | `items` |

### R4: Unit Suffixes

```
IF: identifier represents a measurement value (time, size, distance, rate, etc.)
AND: the unit is NOT stated in the name
THEN: append the unit to the name
```

| Dimension | Suffix Pattern | Examples |
|---|---|---|
| Time | `...In<Unit>` or `...<Unit>` | `timeoutInSeconds`, `retryDelayMs`, `elapsedTimeInDays` |
| Size | `...In<Unit>` or `...<Unit>` | `fileSizeInBytes`, `maxPayloadKb` |
| Rate | `...Per<Unit>` | `requestsPerSecond`, `eventsPerMinute` |
| Money | `...In<Currency>` or `...Cents` | `amountInCents`, `priceInUsd` |
| Percentage | `...Percent` or `...BasisPoints` | `discountPercent`, `rateBasisPoints` |

### R5: Interface Naming

```
IF: type is an interface
AND: name starts with "I" followed by uppercase letter (e.g., IFileSaver)
THEN: remove the "I" prefix
```

```
IF: type is a concrete class implementing an interface
AND: class adds no meaningful distinction beyond the interface name
THEN: prefix with "Default" ONLY in this case
```

| Before | After |
|---|---|
| `IFileSaver` | `FileSaver` |
| `FileSaver` + `DefaultFileSaver` | Acceptable only if no better name exists |

### R6: Base/Abstract Class Prefixes

```
IF: class name starts with "Base" or "Abstract"
THEN: rename to the domain concept itself
```

| Before | After |
|---|---|
| `BaseTruck` | `Truck` |
| `AbstractController` | `Controller` |
| `BaseEntity` | `Entity` |

The class name should reveal its domain role, not its position in an inheritance hierarchy.

### R7: Utils/Helper/Manager Classes

```
IF: class name ends with "Utils", "Utility", "Helper", or "Manager"
THEN: flag for decomposition. Move each method into the class it operates on.
```

| Before | After |
|---|---|
| `CookieUtils.parseCookie(cookie)` | `cookie.parse()` |
| `StringHelper.truncate(str, n)` | `str.truncate(n)` |
| `ConfigManager` | Split into `AppConfig`, `DatabaseConfig`, `CacheConfig` |

### R8: Method Naming

```
IF: method name describes implementation mechanism (HOW)
AND: NOT business intent (WHAT)
THEN: rename to the business intent
```

| HOW (reject) | WHAT (use) |
|---|---|
| `fetchDataFromMySQLAndFilter()` | `getAuthorizedActiveUsers()` |
| `sendKafkaMessage()` | `notifyUserRegistered()` |
| `insertIntoDatabase()` | `saveCustomer()` |
| `callExternalApi()` | `retrieveCreditScore()` |

```
IF: method has side effects (throws, mutates state, writes)
AND: verb does not signal the side effect
THEN: add side-effect signal to verb
```

| Without Signal | With Signal |
|---|---|
| `getContext()` (throws if invalid) | `validateContextOrThrow()` |
| `process()` (mutates field) | `processAndUpdateState()` |

### R9: Class Naming

```
IF: type is a class/interface
AND: name is NOT a noun or noun phrase
THEN: rename to a noun/noun phrase
```

```
IF: class name ∈ {InfoManager, DataProcessor, RequestHandler, ObjectFactory, ServiceProvider, OperationUtils}
THEN: replace with a domain-specific noun reflecting the single responsibility
```

### R10: One Word Per Concept

```
IF: two identifiers express the same action using different verbs
OR: two identifiers express the same concept using different nouns
THEN: unify to a single term across the codebase
```

| Mixed (reject) | Unified (use exactly one per codebase) |
|---|---|
| `fetch` / `retrieve` / `get` | Pick one, use everywhere |
| `remove` / `delete` / `destroy` | Pick one, use everywhere |
| `create` / `build` / `construct` / `make` | Pick one, use everywhere |
| `start` / `begin` / `initiate` | Pick one, use everywhere |

### R11: Magic Numbers → Named Constants

```
IF: numeric literal appears in code (not in a constant declaration)
AND: its purpose is not self-evident from context
THEN: extract to a named constant
```

| Before | After |
|---|---|
| `if (status == 4)` | `private static final int STATUS_ACTIVE = 4;` → `if (status == STATUS_ACTIVE)` |
| `Thread.sleep(3000)` | `private static final long RETRY_DELAY_MS = 3000;` → `Thread.sleep(RETRY_DELAY_MS)` |
| `getGenericTypeName(0)` | `private static final int CONTEXT_TYPE_INDEX = 0;` → `getGenericTypeName(CONTEXT_TYPE_INDEX)` |

**Exception**: `0`, `1`, `-1` in loop initialization, increment, or bounds-checking context.

### R12: Slang & Humor

```
IF: identifier contains slang, jargon, humor, or pop-culture reference
THEN: replace with professional domain terminology
```

| Before | After |
|---|---|
| `whack()` | `terminate()` |
| `grenade()` | `abort()` |
| `yeet()` | `discard()` |
| `nukeRecords()` | `purgeRecords()` |
| `kthxbai()` | `finalize()` |

### R13: Misleading Type Names

```
IF: identifier contains a collection-type suffix
AND: suffix ∈ {List, Map, Set, Array, Dict, Collection, Hash, Vector, Queue, Stack}
AND: the declared type does NOT match the suffix
THEN: rename to match actual type OR change the declared type
```

| Before | Problem | After |
|---|---|---|
| `Set<User> userList` | Suffix says List, type is Set | `Set<User> activeUsers` |
| `List<Config> configMap` | Suffix says Map, type is List | `List<Config> configEntries` |
| `Map<K,V> itemArray` | Suffix says Array, type is Map | `Map<K,V> itemLookup` |
| `String userName` (but holds an ID) | Name says Name, content is ID | `String userId` |

This rule also applies when the name implies a different domain concept than what it holds:

```
IF: identifier name implies content that differs from actual usage
AND: a reader would be misled about the variable's purpose
THEN: rename to match actual content
```

| Before | Problem | After |
|---|---|---|
| `int age` (holds birth year) | `age` implies years-since-birth | `int birthYear` |
| `boolean isEnabled` (holds "is admin") | Name implies feature flag | `boolean isAdmin` |

### R14: Context Disambiguation

```
IF: identifier is a bare noun without qualifying context
AND: noun ∈ {size, count, name, id, type, value, key, status, state, code, number, date, time, path, url, text, message, result, response, request, input, output, source, target, data}
AND: parent scope contains multiple entities that could own this noun
THEN: prefix with the specific entity it belongs to
```

| Before | After |
|---|---|
| `size` | `fileSize` or `bufferSize` or `windowSize` |
| `count` | `activeUserCount` or `retryCount` or `pageCount` |
| `name` | `customerName` or `fileName` or `columnName` |
| `id` | `orderId` or `sessionId` or `transactionId` |
| `status` | `paymentStatus` or `deploymentStatus` or `accountStatus` |
| `type` | `mediaType` or `eventType` or `accountType` |
| `date` | `createdDate` or `expiryDate` or `shipDate` |
| `code` | `errorCode` or `countryCode` or `productCode` |

**Exception**: When the bare noun is the ONLY entity of its kind in scope and the scope is ≤ 20 lines (e.g., a 3-line lambda body operating on a single `File` object — `size` is unambiguous).

---

## Review Mode Output Format

When reporting violations, use this exact structure:

```
FILE: <relative-path>
  L<line>: <identifier> — <rule-id>: <one-line violation description>
  FIX: <before> → <after>
```

Group violations by file. Sort by severity: **FAIL** (must fix) before **WARN** (should fix).

Severity mapping:
- **FAIL**: R1, R3, R5, R6, R7, R11, R13 (single-letter names, Hungarian, I-prefix interfaces, Base/Abstract prefixes, Utils classes, magic numbers, misleading names)
- **WARN**: R2, R4, R8, R9, R10, R12, R14 (abbreviations, missing units, method intent, class nouns, vocabulary consistency, slang, bare-noun context)

---

## Refactor Mode Confirmation

After each fix, output one line:
```
<file>:L<line> <before> → <after>
```

---

## Self-Correction Loop (Write Mode)

Before emitting generated code, for each identifier you introduced:
1. Apply the Identifier Evaluation Algorithm.
2. If any check fails, rename and re-check.
3. Repeat until all identifiers pass.

---

For deep architectural naming patterns or concrete refactoring templates, consult the embedded assets:
*   See [Architectural Vocabulary](references/ARCHETYPES.md) for domain-driven naming types.
*   See [Refactoring Templates](assets/TEMPLATES.md) for side-by-side pattern matching.
