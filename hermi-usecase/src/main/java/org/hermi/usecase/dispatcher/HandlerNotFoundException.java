package org.hermi.usecase.dispatcher;

/**
 * <b>AI INSTRUCTION:</b> WHAT: A runtime exception signaling that no registered {@link Handler}
 * matches the given Context. Represents a configuration gap, not a recoverable runtime condition.
 * WHY: Fail-fast on missing routing branches — forces the developer to register the missing Handler
 * rather than silently producing incorrect results or null. WHO: Thrown by {@link
 * DispatcherUseCase#doFulfill} when the handler iteration exhausts without a match. Caught or
 * propagated by upstream error handling in the Shell layer. WHEN: At routing time inside {@link
 * DispatcherUseCase#doFulfill}, when no handler's {@link Handler#supports(Validatable)} returns
 * {@code true}. WHERE: Use Case layer — dispatcher sub-package. Part of the conditional routing
 * error model. HOW: Throw with a descriptive message identifying the dispatcher and context. Do NOT
 * catch-and-swallow in business logic — the routing gap MUST be surfaced and fixed.
 *
 * <p>DO NOT add: - catch and suppress this exception in business logic (it signals a configuration
 * gap) - custom fields beyond the message (standard {@link RuntimeException} pattern is sufficient)
 * - retry or fallback logic (the fix is registering the missing Handler, not recovering at runtime)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Throw when no handler matches — forces the developer to fix the routing table
 * throw new HandlerNotFoundException("No handler for payment method: " + context.method());
 * // WRONG: Do NOT catch and return null, or log-and-swallow — the routing gap MUST be surfaced
 * }</pre>
 */

/**
 * Routing failure: thrown by {@link DispatcherUseCase} when no registered {@link Handler} supports
 * the context.
 */

/**
 * Thrown when a {@link DispatcherUseCase} cannot find a matching {@link Handler} for the given
 * context. This is a configuration error — the fix is registering the missing Handler, not
 * recovering at runtime.
 */
public class HandlerNotFoundException extends RuntimeException {
  public HandlerNotFoundException(String message) {
    super(message);
  }
}
