package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.usecase.Intent;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Defines a technology-neutral contract for calling an external
 * system. Specifies input ({@code Context}) and output ({@code Result}) using plain Java types
 * only. WHY: Decouple domain intent from integration complexity. The use case expresses WHAT it
 * needs (e.g., "find user by SSN") without knowing HOW (REST vs gRPC vs GraphQL). WHO: Defined by
 * Use Case layer as an abstract contract. Called by {@code doFulfill} in a use case. Implemented by
 * Shell layer with technology-specific adapters (e.g., {@code RestFindUserClient}). WHEN: Phase 1:
 * define the contract skeleton (Context/Result records only). Phase 2: implement real communication
 * logic with the chosen technology. WHERE: Use Case layer — contract definition only. The
 * implementation lives in Shell layer, prefixed with the technology name (e.g., {@code Rest},
 * {@code Grpc}, {@code Soap}). HOW: Extend with nested static {@code Context} and {@code Result}
 * records. Records use ONLY plain Java types ({@code String}, {@code UUID}, {@code BigDecimal}).
 * Name the contract {@code {Action}{Resource}Client}. Phase 1 {@code doFulfill} returns {@code
 * null}; Phase 2 adapters implement real logic.
 *
 * <p>DO NOT add: - implementation logic in Phase 1 (contract definition only) - technology-specific
 * types ({@code RestTemplate}, {@code HttpHeaders}, gRPC stubs) in records - try-catch, logging, or
 * null checks (handled by {@link org.hermi.commons.Executor} base class) - business logic (Client
 * is a gateway, not a domain service)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: Extends Client with nested static Context/Result records, pure Java types only
 * public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
 *   public static record Context(String ssn) {}
 *   public static record Result(String name, String email) {}
 * }
 * // WRONG: FindUserClientImpl, FindUserApiClient — do NOT use these names
 * // WRONG: Do NOT add RestTemplate, HttpHeaders, or any HTTP types in records
 * }</pre>
 */

/** External System Gateway: technology-neutral contract for outbound API calls. */

/**
 * Base class for all external service client contracts in the Hermi framework. Extends {@link
 * org.hermi.commons.Executor} to provide a technology-neutral interface for external system
 * interactions (REST, gRPC, GraphQL).
 *
 * @param <C> the type of the context (use plain Java types only)
 * @param <R> the type of the result (use plain Java types only)
 */
public abstract class Client<C, R> extends Executor<C, R> implements Intent<C, R> {

  /**
   * Fulfills the caller's intent to reach the external system.
   *
   * @param context the client request context
   * @return the client response result
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
   * Fulfills the external system call.
   *
   * <p>Phase 1 (Use Case Layer): Define the contract by extending this class with Context/Result
   * records. Phase 2 (Shell Layer): Implement the real communication logic using specific
   * technologies (e.g., {@code RestFindUserClient}, {@code GrpcFindUserClient}).
   *
   * @param context the client request context
   * @return the client response result
   */
  protected abstract R doFulfill(C context);
}
