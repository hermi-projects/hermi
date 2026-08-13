package org.hermi.usecase.dispatcher;

import org.hermi.commons.Executor;

/**
 * <b>AI INSTRUCTION:</b> WHAT: A single conditional routing branch — implements {@link
 * #supports(Validatable)} (the routing condition) and {@code doExecute} (the branch logic).
 * Executed by {@link DispatcherUseCase} when the condition matches. WHY: Encapsulate one branch of
 * conditional logic as an independently testable unit. Adding a new branch means writing a new
 * Handler — zero changes to the dispatcher. WHO: Called by {@link DispatcherUseCase} during
 * routing. Extended by concrete handlers (e.g., {@code CreditCardHandler}, {@code ACHHandler}).
 * WHEN: {@link #supports(Validatable)} is called BEFORE {@code doExecute} by the dispatcher. {@code
 * supports()} MUST be idempotent and side-effect-free — state mutation causes non-deterministic
 * routing. Handlers are registered in the dispatcher at construction time. WHERE: Use Case layer —
 * dispatcher sub-package. Extends {@link UseCase} with {@link NoopAuditor} (auditing is handled by
 * the parent DispatcherUseCase) and {@code shouldValidate() = false} (validation is deferred to the
 * dispatcher). HOW: Extend {@link Handler}, NOT {@link UseCase} directly. Implement both {@code
 * supports(C)} (pure condition, no side effects) and {@code doExecute(C)} (branch logic). Do NOT
 * add validation, auditing, or logging — the dispatcher handles all cross-cutting concerns.
 *
 * <p>DO NOT add: - extend UseCase directly (MUST extend {@link Handler} instead) - validation logic
 * ({@code shouldValidate()} returns {@code false} by design) - Auditor or logging concerns (uses
 * {@link NoopAuditor}; auditing is handled by DispatcherUseCase) - try-catch in doExecute
 * (exception boundary is in UseCase.execute())
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Extends Handler, implements supports() as idempotent condition, doExecute for logic
 * public class CreditCardHandler extends Handler<PaymentContext, PaymentResult> {
 *   public boolean supports(PaymentContext ctx) { return "CREDIT".equals(ctx.method()); }
 *   protected PaymentResult doExecute(PaymentContext ctx) { return new PaymentResult("charged"); }
 * }
 * // WRONG: CreditCardHandlerUseCase, CreditCardProcessor — do NOT use these names
 * // WRONG: Do NOT extend UseCase directly or override shouldValidate() to return true
 * }</pre>
 */

/**
 * Conditional Routing Block: a single {@code supports()} + {@code doExecute} pair executed by a
 * {@link DispatcherUseCase}.
 */

/**
 * A conditional routing block executed by a {@link DispatcherUseCase}. Each Handler encapsulates a
 * routing condition ({@link #supports(Validatable)}) and the corresponding business logic ({@code
 * doExecute}). Validation and auditing are disabled — the owning DispatcherUseCase handles both.
 *
 * @param <C> the type of the context, which MUST implement {@link Validatable}
 * @param <R> the type of the result
 */
public abstract class Handler<C, R> extends Executor<C, R> {
  /**
   * Evaluates if this handler supports the given context.
   *
   * <p>MUST be idempotent and side-effect-free. The dispatcher calls this method on each registered
   * handler in order; the first match wins.
   *
   * @param context the input context to evaluate
   * @return {@code true} if this handler can process the context, {@code false} otherwise
   */
  public abstract boolean supports(C context);
}
