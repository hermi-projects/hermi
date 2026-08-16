package org.hermi.usecase.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.usecase.standard.UseCase;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Routes a single Context to the first matching {@link Handler} based
 * on {@link Handler#supports(Validatable)}. A specialized {@link UseCase} for conditional branching
 * that replaces if-else chains with a pluggable handler registry. WHY:
 * Strategy/Chain-of-Responsibility pattern — each Handler encapsulates its own routing condition
 * and business logic. New branches are added by registering a new Handler, not by editing
 * doFulfill. WHO: Extended by concrete dispatchers (e.g., {@code DefaultPaymentDispatcher}). Calls
 * registered {@link Handler} instances in registration order — first match wins. WHEN: Handlers are
 * registered at construction time via the constructor. {@link #register(Handler)} allows dynamic
 * addition but MUST be called before {@code fulfill()}. Subclasses MUST NOT override {@code
 * doFulfill} — the routing loop is already implemented. WHERE: Use Case layer — dispatcher
 * sub-package. A sibling pattern to the standard {@link UseCase}, for scenarios where one context
 * maps to one of many handlers. HOW: Extend {@code DispatcherUseCase<C, R>}. Register handlers via
 * constructor. Each {@link Handler} implements {@code supports(C)} (routing condition) and {@code
 * doFulfill(C)} (branch logic). Missing handler throws {@link HandlerNotFoundException}.
 *
 * <p>DO NOT add: - override doFulfill (routing logic is already implemented in the base class) -
 * try-catch around handler execution (exception boundary is in UseCase.fulfill()) - null checks on
 * handler list (constructor ensures non-null) - default/fallback handler logic (missing handler
 * MUST throw HandlerNotFoundException)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Extends DispatcherUseCase, registers handlers via constructor, does NOT override doFulfill
 * public class DefaultPaymentDispatcher extends DispatcherUseCase<PaymentContext, PaymentResult> {
 *   public DefaultPaymentDispatcher(CreditCardHandler credit, ACHHandler ach) {
 *     super(credit, ach);
 *   }
 * }
 * // WRONG: PaymentDispatcherImpl, PaymentRoutingService — do NOT use these names
 * // WRONG: Do NOT override doFulfill or add fallback/default routing logic
 * }</pre>
 */

/**
 * Conditional Routing: dispatches a Context to the first matching {@link Handler} based on {@code
 * supports()}.
 */

/**
 * A specialized Use Case pattern that routes execution to a specific {@link Handler}. Iterates
 * through registered handlers and delegates to the first one whose {@link
 * Handler#supports(Validatable)} method returns {@code true}. Throws {@link
 * HandlerNotFoundException} if no handler matches.
 *
 * @param <C> the type of the context, which MUST implement {@link Validatable}
 * @param <R> the type of the result
 */
public abstract class DispatcherUseCase<C, R> extends UseCase<C, R> {
  private final List<Handler<C, R>> handlers;

  /**
   * Constructs a DispatcherUseCase with a variadic array of handlers.
   *
   * @param handlers the handlers to register
   */
  @SafeVarargs
  protected DispatcherUseCase(Handler<C, R>... handlers) {
    this(List.of(handlers));
  }

  /**
   * Constructs a DispatcherUseCase with a list of handlers.
   *
   * @param handlers the list of handlers
   */
  protected DispatcherUseCase(List<Handler<C, R>> handlers) {
    super();
    this.handlers = new ArrayList<>();
    this.handlers.addAll(handlers);
  }

  /**
   * Routes the context to the first supported handler and executes it.
   *
   * @param context the context to process
   * @return the result from the matching handler
   * @throws HandlerNotFoundException if no registered handler supports the context
   */
  @Override
  protected R doFulfill(C context) {
    for (Handler<C, R> handler : handlers) {
      if (handler.supports(context)) {
        return handler.fulfill(context);
      }
    }
    throw new HandlerNotFoundException(
        getClass().getSimpleName() + ": No handler found for context: " + context);
  }
}
