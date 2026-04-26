package org.hermi.usecase.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.usecase.standard.UseCase;
import org.hermi.validation.Validatable;

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

  @SuppressWarnings("unchecked")
  public DispatcherUseCase(Handler<C, R>... handlers) {
    this(List.of(handlers));
  }

  public DispatcherUseCase(List<Handler<C, R>> handlers) {
    this.handlers = new ArrayList<>();
    this.handlers.addAll(handlers);
  }

  public void register(Handler<C, R> handler) {
    this.handlers.add(handler);
  }

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
