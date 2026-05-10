package org.hermi.usecase.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.constraint.validation.Validatable;
import org.hermi.usecase.standard.UseCase;

/**
 * <b>AI INSTRUCTION:</b> When subclassing DispatcherUseCase, do NOT override {@code doExecute}.
 * Register {@link Handler} implementations via the constructor. This pattern is for routing a
 * single Context to the correct Handler based on condition.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * public class DefaultPaymentDispatcher extends DispatcherUseCase<PaymentContext, PaymentResult> {
 *   public DefaultPaymentDispatcher(List<Handler<PaymentContext, PaymentResult>> handlers) {
 *     super(handlers);
 *   }
 * }
 * }</pre>
 */

/** Pattern definition for Conditional Routing. */

/**
 * A specialized Use Case pattern that routes execution to a specific {@link Handler}.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
 */
public abstract class DispatcherUseCase<C extends Validatable, R> extends UseCase<C, R> {
  private final List<Handler<C, R>> handlers;

  /**
   * Constructs a DispatcherUseCase with a variadic array of handlers.
   *
   * @param handlers the handlers to register
   */
  @SuppressWarnings("unchecked")
  public DispatcherUseCase(Handler<C, R>... handlers) {
    this(List.of(handlers));
  }

  /**
   * Constructs a DispatcherUseCase with a list of handlers.
   *
   * @param handlers the list of handlers
   */
  public DispatcherUseCase(List<Handler<C, R>> handlers) {
    this.handlers = new ArrayList<>();
    this.handlers.addAll(handlers);
  }

  /**
   * <b>AI INSTRUCTION:</b> Call ONLY during initialization (constructor or {@code @PostConstruct}).
   * NEVER call this method inside {@code doExecute} — the handler list is NOT thread-safe for
   * concurrent modifications during execution.
   */
  /**
   * Dynamically registers a new handler to this dispatcher.
   *
   * @param handler the handler to add
   */
  public void register(Handler<C, R> handler) {
    this.handlers.add(handler);
  }

  /**
   * Routes the context to the first supported handler and executes it.
   *
   * @param context the context to process
   * @return the result from the processing handler
   * @throws HandlerNotFoundException if no registered handler supports the context
   */
  @Override
  protected R doExecute(C context) {
    for (Handler<C, R> handler : handlers) {
      if (handler.support(context)) {
        return handler.execute(context);
      }
    }
    throw new HandlerNotFoundException(
        getSimpleClassName() + ": No handler found for context: " + context);
  }
}
