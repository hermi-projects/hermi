package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.constraint.validation.Validatable;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Domain Persistence Interface.
 *     <p>DESIGN INTENT: Decouple business logic from state storage details (SQL, NoSQL, File).
 *     <p>PURPOSE: Provide a technology-neutral interface for data access and persistence.
 *     <p>Phase: 1 (Contract Discovery)
 *     <p>Priority: 4 (Critical Core)
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific
 *           instance variables. Only final, immutable dependencies (via constructor injection) are
 *           allowed.
 *       <li>2. NO INFRASTRUCTURE TYPES: NEVER use JPA, Hibernate, or JDBC types in contract
 *           records.
 *       <li>3. PURE JAVA TYPES: Records MUST use ONLY plain Java types (String, UUID, BigDecimal,
 *           etc.).
 *       <li>4. NAMING PROPHECY: Follow the {@code {Action}{Resource}Repository} pattern (e.g.,
 *           {@code SaveUserRepository}).
 *       <li>5. VALIDATION: The {@code Result} record MUST implement {@link Validatable}.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER add business logic to the Repository; it is only a gateway to the persistence
 *           store.
 *       <li>DO NOT add implementation logic in Phase 1; only define the I/O records.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
 *   public static record Context(String name, String email) {}
 *   public static record Result(String id) implements Validatable {}
 * }
 * }</pre>
 */

/**
 * Base class for all data persistence contracts in the Hermi framework.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result, which MUST implement {@link Validatable}
 */
public abstract class Repository<C, R extends Validatable> extends Executor<C, R> {

  /**
   * Saves the repository request to an external data source and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input and output types. Only the output (which returns to the Use Case)
   *       typically implements {@link Validatable Validatable}.
   *   <li><b>Shell Layer (Phase 2)</b>: Implements the real-world data access logic using specific
   *       technologies, prefixed with the technology name (e.g., {@code JpaSaveUserRepository}).
   * </ul>
   *
   * <p>Example SaveUserRepository Contract in Use Case (Phase 1):
   *
   * <pre>{@code
   * public abstract class SaveUserRepository extends Repository<SaveUserRepository.Context, SaveUserRepository.Result> {
   *   public static record Context(String name, String email) {}
   *   public static record Result(String id) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example SaveUserRepository Implementation in Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class JpaSaveUserRepository extends SaveUserRepository {
   *
   *   private final UserJpaRepository jpaRepository;
   *   private final Mapper<Context, Result, UserEntity, UserEntity> mapper;
   *
   *   public JpaSaveUserRepository(UserJpaRepository jpaRepository,
   *       Mapper<Context, Result, UserEntity, UserEntity> mapper) {
   *     this.jpaRepository = jpaRepository;
   *     this.mapper = mapper;
   *   }
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     UserEntity entity = mapper.toPayload(context);
   *     UserEntity savedEntity = jpaRepository.save(entity);
   *     return mapper.toResult(savedEntity);
   *   }
   * }
   * }</pre>
   *
   * @param context the repository request context
   * @return the repository response result
   */
  protected abstract R doExecute(C context);
}
