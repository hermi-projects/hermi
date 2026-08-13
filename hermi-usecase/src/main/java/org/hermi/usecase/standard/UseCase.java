package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.commons.validation.JakartaValidator;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Defines the business logic boundary for one business capability.
 * Orchestrates abstract I/O contracts ({@link Client}, {@link Repository}, {@link Messenger})
 * discovered Just-In-Time within {@code doExecute(Context)}. The base class validates Context and
 * integrates {@link org.hermi.commons.audit.Auditor}. WHY: Blueprint-First Orchestration — business
 * logic reveals its own I/O needs without coupling to infrastructure. Enables Phase 1 verification
 * with local adapters before Phase 2 production wiring. WHO: Called by Shell layer (Phase 2
 * services/controllers). Extended by domain use case contracts (e.g., {@code FindUserUseCase}) and
 * their Default implementations. WHEN: Callers invoke {@code execute(Context)} — it validates, then
 * delegates to {@code doExecute}. NEVER call {@code doExecute} directly. Subclasses override {@code
 * doExecute}, NEVER {@code execute}. WHERE: Use Case layer (Phase 1) — the central unit of business
 * logic. Sits between Shell (infrastructure delivery) and Domain contracts. Knows NOTHING about
 * HTTP, JPA, or Kafka. HOW: Extend with nested {@code Context} (implements {@link Validatable}) and
 * {@code Result} records. Name the abstract contract {@code {Action}{Resource}UseCase}. Name the
 * implementation {@code Default{Action}{Resource}UseCase}. Inject contracts via constructor.
 * Discover I/O contracts JIT within {@code doExecute}.
 *
 * <p>DO NOT add: - try-catch (infrastructure exceptions MUST be caught and wrapped as domain
 * exceptions in doExecute) - log statements (handled by {@link org.hermi.commons.audit.Auditor}) -
 * null checks on context (handled by {@link Validatable} validation in execute()) - framework
 * annotations ({@code @Service}, {@code @Transactional}, {@code @Component}) in Phase 1
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Default prefix, Context/Result as nested records, constructor injection
 * public class DefaultFindUserUseCase extends FindUserUseCase {
 *   private final FindUserClient findUserClient;
 *   public DefaultFindUserUseCase(FindUserClient findUserClient) {
 *     this.findUserClient = findUserClient;
 *   }
 *   @Override
 *   protected Result doExecute(Context context) {
 *     var apiResult = findUserClient.execute(new FindUserClient.Context(context.ssn()));
 *     return new Result(apiResult.name(), apiResult.email());
 *   }
 * }
 * // WRONG: FindUserUseCaseImpl, FindUserService — do NOT use these names
 * // WRONG: Do NOT add @Service, @Transactional, or local try-catch in doExecute
 * }</pre>
 */

/**
 * Blueprint-First Orchestration: validates Context, delegates to {@code doExecute}, integrates
 * {@link org.hermi.commons.audit.Auditor}.
 */

/**
 * Base class for all business use cases within the Hermi framework. Extends {@link
 * org.hermi.commons.Executor} with automatic {@link Validatable} validation and audit integration.
 * Subclasses implement {@code doExecute(Context)} to define the business logic, discovering I/O
 * contracts (Client, Repository, Messenger) Just-In-Time.
 *
 * @param <C> the type of the context, which MUST implement {@link Validatable}
 * @param <R> the type of the result
 */
public abstract class UseCase<C, R> extends Executor<C, R> {
  protected UseCase() {
    setContextValidator(new JakartaValidator());
  }

  /**
   * Implements the business logic for this use case.
   *
   * <p>The base {@code execute(Context)} method validates the context before calling this method.
   * Infrastructure exceptions MUST be caught and wrapped as domain exceptions — never let
   * technology-specific exceptions (SocketTimeoutException, DataAccessException) escape this
   * method.
   *
   * @param context the validated use case input
   * @return the use case output
   */
  @Override
  protected abstract R doExecute(C context);
}
