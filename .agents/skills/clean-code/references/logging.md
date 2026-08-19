# Logging

> **Core Rule:** Logs must serve as structured, context-rich audit trails that answer four fundamental questions: who performed the action, when it happened, what data was involved, and why the operation failed. They must be precise, secure, and performant. and never contain sensitive data.

## 1. Log Level Discipline

* **Rule:** Use appropriate log levels strictly based on the severity and urgency of the event to ensure effective monitoring and alerting.
* **Constraints:**
* **ERROR:** Reserved exclusively for blocking, unrecoverable system failures that require **immediate human intervention** or trigger high-priority alerts.
* **WARN:** For non-breaking, unexpected states where the system gracefully recovers, retries, or falls back. Requires attention, but is not an emergency.
* **INFO:** Reserved for high-level business milestones and workflow state transitions (e.g., user registration complete, payment processed).
* **DEBUG / TRACE:** Strictly for local development and deep troubleshooting. **Must be disabled in production environments** to avoid log pollution and performance drag.

## 2. Rich Context & Metadata

* **Rule:** Logs must carry comprehensive contextual identifiers to trace execution flows accurately across distributed systems.
* **Constraints:**
* **Correlation IDs:** Always bind logs to a distributed Trace ID or Request ID.
* **Business Keys:** Inject domain-specific primary keys (e.g., `userId`, `orderId`, `transactionId`) into log messages rather than printing generic operational text.

## 3. Security & PII Shielding

* **Rule:** Protect sensitive user data and credentials from leaking into log files.
* **Constraints:**
* **Forbidden:** Never log raw passwords, secret tokens, API keys, credit card numbers, or unmasked sensitive Personal Identifiable Information (PII).
* **Masking Mandate:** Any necessary sensitive fields (such as phone numbers or emails) must be programmatically masked before logging (e.g., `138****1234`).

## 4. Performance & Parameterization

* **Rule:** Write efficient logging code that avoids unnecessary CPU, memory, and I/O overhead.
* **Constraints:**
* **Parameterization:** Always use parameterized placeholders (e.g., `logger.info("User {} logged in", userId)`) instead of heavy string concatenation (`"User " + userId + " logged in"`).
* **Avoid Heavy Computations:** Do not invoke expensive serialization or method computations inside log statements unless the target log level is actively enabled.

## 5. Actionable & Readable Messages

* **Rule:** Log messages must be self-explanatory and guide the reader straight to the root cause.
* **Constraints:**
* **Forbidden:** Vague, empty, or uninformative log statements (e.g., `logger.error("Error occurred");` or `logger.info("Success");`) are strictly forbidden.
* **Error Inclusion:** When catching exceptions, always pass the root exception object as the final parameter to preserve full stack traces rather than just printing `ex.getMessage()`.
