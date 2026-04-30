package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.validation.Validatable;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Secure External Transporter.
 *     <p>DESIGN INTENT: Decouple domain intent from external integration complexities (REST, gRPC).
 *     <p>PURPOSE: Provide a technology-neutral interface for external system interactions.
 *     <p>Phase: 1 (Contract Discovery)
 *     <p>Priority: 4 (Critical Core)
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific
 *           instance variables. Only final, immutable dependencies (via constructor injection) are
 *           allowed.
 *       <li>2. NO TECH BLEED: NEVER use technology-specific types (e.g., RestTemplate, HttpHeaders)
 *           in contract records.
 *       <li>3. PURE JAVA TYPES: Records MUST use ONLY plain Java types (String, UUID, BigDecimal,
 *           etc.).
 *       <li>4. NAMING PROPHECY: Follow the {@code {Action}{Resource}Client} pattern (e.g., {@code
 *           FindUserClient}).
 *       <li>5. VALIDATION: The {@code Result} record MUST implement {@link Validatable}.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>DO NOT add implementation logic to classes extending this contract in Phase 1.
 *       <li>NEVER leak infrastructure-specific metadata into the domain context.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
 *   public static record Context(String ssn) {}
 *   public static record Result(String name) implements Validatable {}
 * }
 * }</pre>
 */

/**
 * Base class for all external service client contracts in the Hermi framework.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result, which MUST implement {@link Validatable}
 */
public abstract class Client<C, R extends Validatable> extends Executor<C, R> {
  /**
   * Executes the external system calling with the client request and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input type {@code I} and output type {@code O}. Only the Output (which
   *       returns to the Use Case) typically implements {@link Validatable Validatable}.
   *   <li><b>Shell Layer (Phase 2)</b>: Implements the real-world communication logic using
   *       specific technologies, prefixed with the technology name (e.g., {@code
   *       RestFindUserClient}).
   * </ul>
   *
   * <p>Example Use Case Layer (Phase 1):
   *
   * <pre>{@code
   * public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
   *   public static record Context(String ssn) {}
   *   public static record Result(String name, String email) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class LexisNexisFindUserClient extends FindUserClient
   *     implements Adapter<FindUserClient.Context, FindUserClient.Result, ApiRequest, ApiResponse> {
   *
   *   private final RestTemplate restTemplate;
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     ApiRequest apiRequest = convertContext(context);
   *     ApiResponse apiResponse = process(apiRequest);
   *     return convertResult(apiResponse);
   *   }
   *
   *   @Override
   *   public ApiRequest convertContext(Context context) {
   *     return new ApiRequest(context.ssn());
   *   }
   *
   *   @Override
   *   public ApiResponse process(ApiRequest input) {
   *     return restTemplate.postForObject("/api/users", input, ApiResponse.class);
   *   }
   *
   *   @Override
   *   public Result convertResult(ApiResponse output) {
   *     return new Result(output.getName(), output.getEmail());
   *   }
   * }
   * }</pre>
   *
   * @param context the client request context
   * @return the client response result
   */
  protected abstract R doExecute(C context);
}
