package org.hermi.usecase;

/**
 * <b>AI INSTRUCTION:</b> WHAT: The caller-facing contract of the Use Case layer — a declared intent
 * ({@code Intent<C, R>}) with a single verb {@link #fulfill(Object)}. WHY: Callers depend on WHAT
 * they want to happen, never on the execution machinery (auditing, validation) in {@link
 * org.hermi.commons.Executor}. WHO: Implemented by the semantic base classes ({@link
 * org.hermi.usecase.standard.UseCase}, {@link org.hermi.usecase.standard.Client}, {@link
 * org.hermi.usecase.standard.Repository}, {@link org.hermi.usecase.standard.Messenger}, {@link
 * org.hermi.usecase.dispatcher.Handler}); depended on by callers such as Shell-layer Controllers
 * and {@link org.hermi.usecase.dispatcher.DispatcherUseCase}. WHEN: Whenever one component calls
 * another — the caller expresses an intent, the callee fulfills it. WHERE: Use Case layer only —
 * the Shell layer keeps its own protocol verbs ({@code exchange}, {@code publish}) and does NOT
 * implement this interface. HOW: Never implement {@code Intent} directly — extend a semantic base
 * class, which inherits the lifecycle from {@link org.hermi.commons.Executor} and exposes {@code
 * fulfill}.
 *
 * <p>DO NOT add: - machinery methods to this interface (it is the what-contract; the how lives in
 * {@link org.hermi.commons.Executor}) - Shell-layer implementations of this interface
 */

/** Intent Contract: what callers ask for — the how lives in the Executor machinery. */

/**
 * The caller-facing contract of the Use Case layer: a declared intent, fulfilled on demand.
 *
 * <p>From the caller's perspective every Use Case layer component is an intent made executable —
 * {@code useCase.fulfill(context)} asks for a business outcome, {@code client.fulfill(request)}
 * asks for an external system result. The validation and auditing lifecycle is deliberately not
 * part of this interface.
 *
 * @param <C> the type of the intent context
 * @param <R> the type of the intent result
 */
public interface Intent<C, R> {
  /**
   * Fulfills the intent declared by the caller.
   *
   * @param context the intent context
   * @return the intent result
   */
  R fulfill(C context);
}
