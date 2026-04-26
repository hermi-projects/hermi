package org.hermi.usecase.dispatcher;

import org.hermi.usecase.standard.UseCase;
import org.hermi.validation.Validatable;

/**
 * <b>AI INSTRUCTION:</b> When subclassing Handler, implement both {@code support} (routing
 * condition) and {@code doExecute} (actual logic). Ensure it extends {@link Handler} and NOT
 * UseCase directly.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * public class CreditCardHandler extends Handler<PaymentContext, PaymentResult> {
 *   public boolean support(PaymentContext ctx) { return "CREDIT".equals(ctx.method()); }
 *   protected PaymentResult doExecute(PaymentContext ctx) { return new PaymentResult("success"); }
 * }
 * }</pre>
 */

/** Handler implementation for a DispatcherUseCase. */

/**
 * A conditional routing block executed by a {@link DispatcherUseCase}.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
 */
public abstract class Handler<C extends Validatable, R> extends UseCase<C, R> {
  /**
   * Evaluates if this handler supports the given context.
   *
   * @param context the input context to evaluate
   * @return {@code true} if this handler can process the context, {@code false} otherwise
   */
  public abstract boolean support(C context);
}
