package org.hermi.commons;

import jakarta.validation.ConstraintViolation;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hermi.commons.audit.LogAuditor;
import org.hermi.commons.conversion.Converter;
import org.hermi.commons.conversion.Convertible;
import org.hermi.constraint.validation.InputValidationException;
import org.hermi.constraint.validation.Validatable;
import org.hermi.constraint.validation.Validator;

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
 *       <li>3. ONLY implement {@code doExecute}. The lifecycle methods ({@code execute}, {@code
 *           validateContext}, {@code validateResult}) are sealed by design.
 *       <li>4. The {@code execute(C)} method is {@code final}. Do not attempt to bypass validation
 *           or auditing.
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
 *   <li>Convenience methods for conversion-based execution.
 * </ul>
 *
 * @param <C> the type of the execution context
 * @param <R> the type of the execution result
 */
public abstract class Executor<C, R> {

  private final LogAuditor<C, R> logAuditor;

  /** Creates an Executor with a {@link LogAuditor} for debug logging. */
  protected Executor() {
    this.logAuditor = new LogAuditor<>(getClass());
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
   * @throws InputValidationException if the context or result is invalid
   * @throws NullPointerException if the context or result is null
   */
  public final R execute(C context) {
    UUID logId = logAuditor.recordContext(context);
    try {
      validateContext(context);
      R result = doExecute(context);
      logAuditor.recordResult(logId, result);
      validateResult(result);
      return result;
    } catch (Exception e) {
      logAuditor.recordError(logId, context, e);
      throw e;
    }
  }

  /**
   * Executes the logic by first converting the source context.
   *
   * @param convertibleContext the source context that can be converted to {@code C}
   * @return the execution result
   * @throws NullPointerException if convertibleContext is null
   */
  public final R execute(Convertible<C> convertibleContext) {
    Objects.requireNonNull(
        convertibleContext,
        String.format("%s: convertible context cannot be null", getClass().getSimpleName()));
    return execute(convertibleContext.convert());
  }

  /**
   * Executes the logic by converting a source instance using the provided converter.
   *
   * @param <S> the type of the source instance
   * @param source the source instance
   * @param converter the converter to transform {@code S} to {@code C}
   * @return the execution result
   * @throws NullPointerException if source or converter is null
   */
  public final <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(
        source, String.format("%s: source cannot be null", getClass().getSimpleName()));
    Objects.requireNonNull(
        converter, String.format("%s: converter cannot be null", getClass().getSimpleName()));
    return execute(converter.convert(source));
  }

  /**
   * Validates the input context.
   *
   * @param context the context to validate
   * @throws NullPointerException if context is null
   * @throws InputValidationException if context is {@link Validatable} and fails validation
   */
  private void validateContext(C context) {
    Objects.requireNonNull(
        context, String.format("%s: Context cannot be null", getClass().getSimpleName()));
    if (!(context instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<C>> violations = Validator.validate(context);
    if (!violations.isEmpty()) {
      String typeName = context.getClass().getSimpleName();
      throw new InputValidationException(
          String.format("%s: Context, %s, is not valid", getClass().getSimpleName(), typeName),
          violations);
    }
  }

  /**
   * Validates the execution result.
   *
   * @param result the result to validate
   * @throws NullPointerException if result is null
   * @throws InputValidationException if result is {@link Validatable} and fails validation
   */
  private void validateResult(R result) {
    Objects.requireNonNull(
        result, String.format("%s: Result cannot be null", getClass().getSimpleName()));
    if (!(result instanceof Validatable)) {
      return;
    }
    Set<ConstraintViolation<R>> violations = Validator.validate(result);
    if (!violations.isEmpty()) {
      String typeName = result.getClass().getSimpleName();
      throw new InputValidationException(
          String.format("%s: Result, %s, is not valid", getClass().getSimpleName(), typeName),
          violations);
    }
  }
}
