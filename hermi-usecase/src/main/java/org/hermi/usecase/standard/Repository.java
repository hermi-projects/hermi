package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.usecase.Intent;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Defines a technology-neutral contract for data persistence.
 * Specifies input ({@code Context}) and output ({@code Result}) using plain Java types only. WHY:
 * Decouple business logic from storage technology. The use case saves data without knowing whether
 * the backend is SQL, NoSQL, or a flat file. WHO: Defined by Use Case layer as an abstract
 * contract. Called by {@code doFulfill} in a use case. Implemented by Shell layer with
 * technology-specific adapters (e.g., {@code JpaSaveUserRepository}). WHEN: Phase 1: define the
 * contract skeleton (Context/Result records only). Phase 2: implement real data access with the
 * chosen persistence technology. WHERE: Use Case layer — contract definition only. The
 * implementation lives in Shell layer, prefixed with the technology name (e.g., {@code Jpa}, {@code
 * Jdbc}, {@code Mongo}). HOW: Extend with nested static {@code Context} and {@code Result} records.
 * Records use ONLY plain Java types ({@code String}, {@code UUID}, {@code BigDecimal}). Name the
 * contract {@code {Action}{Resource}Repository}. Phase 1 {@code doFulfill} returns {@code null};
 * Phase 2 adapters implement real logic.
 *
 * <p>DO NOT add: - business logic (Repository is a gateway to the persistence store, not a domain
 * service) - JPA, Hibernate, or JDBC types in Context/Result records - implementation logic in
 * Phase 1 (contract definition only) - try-catch, logging, or null checks (handled by {@link
 * org.hermi.commons.Executor} base class)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Extends Repository with nested static Context/Result records, pure Java types only
 * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
 *   public static record Context(String name, String email) {}
 *   public static record Result(String id) {}
 * }
 * // WRONG: SaveUserRepositoryImpl, UserDao — do NOT use these names
 * // WRONG: Do NOT add @Entity, @Table, JPA annotations, or JDBC types in records
 * }</pre>
 */

/** Domain Persistence Gateway: technology-neutral contract for data access. */

/**
 * Base class for all data persistence contracts in the Hermi framework. Extends {@link
 * org.hermi.commons.Executor} to provide a technology-neutral interface for data access and
 * persistence (SQL, NoSQL, file storage).
 *
 * @param <C> the type of the context (use plain Java types only)
 * @param <R> the type of the result (use plain Java types only)
 */
public abstract class Repository<C, R> extends Executor<C, R> implements Intent<C, R> {

  /**
   * Fulfills the caller's intent to persist or retrieve data.
   *
   * @param context the repository request context
   * @return the repository response result
   */
  @Override
  public final R fulfill(C context) {
    return execute(context);
  }

  /**
   * Seals the Executor hook: delegates to {@link #doFulfill}.
   *
   * @param context the validated context
   * @return the fulfillment result
   */
  @Override
  protected final R doExecute(C context) {
    return doFulfill(context);
  }

  /**
   * Fulfills the persistence intent.
   *
   * <p>Phase 1 (Use Case Layer): Define the contract by extending this class with Context/Result
   * records. Phase 2 (Shell Layer): Implement the real data access logic using specific
   * technologies (e.g., {@code JpaSaveUserRepository}, {@code JdbcSaveUserRepository}).
   *
   * @param context the repository request context
   * @return the repository response result
   */
  protected abstract R doFulfill(C context);
}
