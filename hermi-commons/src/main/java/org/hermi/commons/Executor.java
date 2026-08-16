package org.hermi.commons;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.audit.Auditor;
import org.hermi.commons.audit.LogAuditor;
import org.hermi.commons.audit.NoopAuditor;
import org.hermi.commons.validation.NoopValidator;
import org.hermi.commons.validation.Validator;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Lifecycle Orchestration Engine — the execution spine of the Hermi framework.
 *     <p>DESIGN INTENT: Enforce a non-negotiable lifecycle (audit → validate → execute → validate →
 *     audit) around every unit of work, so that subclass authors only provide business logic via
 *     {@code doExecute}.
 *     <p>PURPOSE: Guarantee observability and data integrity as framework invariants, not opt-in
 *     concerns.
 *     <p>Phase: 2 (Hardened)
 *     <p>Priority: 5 (Critical Core)
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific
 *           instance variables. Only final, immutable dependencies (via constructor injection) are
 *           allowed.
 *       <li>2. NEVER subclass Executor directly. Use the semantic base classes: {@code UseCase},
 *           {@code Client}, {@code Repository}, or {@code Messenger}.
 *       <li>3. ONLY implement the semantic hook of your layer ({@code doFulfill} in the Use Case
 *           layer, {@code doExchange}/{@code doPublish} in the Shell layer). {@code doExecute} is
 *           sealed by the semantic base classes.
 *       <li>4. The {@code execute(C)} method is {@code protected final} — callers use the public
 *           verb of the semantic base class ({@code fulfill} in the Use Case layer, {@code
 *           exchange}/{@code publish} in the Shell layer). Do not attempt to bypass validation or
 *           auditing.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER subclass Executor directly in application code — always go through UseCase,
 *           Client, Repository, or Messenger.
 *       <li>DO NOT catch and swallow exceptions inside {@code doExecute} unless rethrowing a
 *           domain-specific exception.
 *       <li>DO NOT return {@code null} from {@code doExecute}; the framework enforces non-null
 *           results.
 *     </ul>
 */

/**
 * Abstract base class for components that execute logic based on a context and return a result.
 *
 * <p>The Executor handles:
 *
 * <ul>
 *   <li>Pre-execution validation of the input context.
 *   <li>Auditing of the execution lifecycle (logged via {@link LogAuditor} by default).
 *   <li>Post-execution validation of the returned result.
 * </ul>
 *
 * @param <C> the type of the execution context
 * @param <R> the type of the execution result
 */
public abstract class Executor<C, R> {

  private Auditor<C, R> auditor = new NoopAuditor<>();
  private Validator contextValidator = new NoopValidator();
  private Validator resultValidator = new NoopValidator();

  /**
   * Sets the auditor for this executor.
   *
   * <p>Call this during initialization to replace the default {@link NoopAuditor} with a custom
   * implementation (e.g. {@link org.hermi.commons.audit.LogAuditor} or {@link
   * org.hermi.commons.audit.PersistentAuditor}).
   *
   * @param auditor the auditor to use (must not be null)
   * @throws NullPointerException if auditor is null
   */
  public void setAuditor(Auditor<C, R> auditor) {
    this.auditor = Objects.requireNonNull(auditor, "Auditor cannot be null");
  }

  protected void setContextValidator(Validator contextValidator) {
    this.contextValidator =
        Objects.requireNonNull(contextValidator, "Context validator cannot be null");
  }

  protected void setResultValidator(Validator resultValidator) {
    this.resultValidator =
        Objects.requireNonNull(resultValidator, "Result validator cannot be null");
  }

  /**
   * Implements the core execution logic.
   *
   * @param context the validated execution context
   * @return the execution result
   */
  protected abstract R doExecute(C context);

  /**
   * Executes the logic with the given context.
   *
   * <p>The lifecycle is: audit start → validate context → {@link #doExecute} → validate result →
   * audit success. Any exception is audited before being propagated.
   *
   * @param context the execution context
   * @return the execution result
   */
  protected final R execute(C context) {
    UUID trackingId = auditor.recordContext(context);
    try {
      contextValidator.validate(context);
      R result = doExecute(context);
      auditor.recordResult(trackingId, result);
      resultValidator.validate(result);
      return result;
    } catch (Exception e) {
      auditor.recordError(trackingId, context, e);
      throw e;
    }
  }
}
