package org.hermi.usecase.standard;

import org.hermi.commons.Executor;
import org.hermi.validation.Validatable;

/**
 * <b>AI INSTRUCTION:</b> When defining a Client contract in the Use Case (Phase 1), use this class.
 * DO NOT add any implementation logic. ONLY define the Context and Result records. The Result
 * record MUST implement Validatable. Context and Result fields MUST use ONLY plain Java types
 * (String, UUID, BigDecimal, LocalDate, primitives). NEVER place Spring, HTTP, or vendor-specific
 * types (HttpHeaders, ApiRequest, RestTemplate) inside Context or Result records.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * public abstract class FindUserClient extends Client<FindUserClient.Context, FindUserClient.Result> {
 *   public static record Context(String ssn) {}
 *   public static record Result(String name) implements Validatable {}
 * }
 * }</pre>
 */

/** Phase 1 IO Contract Definition for external API. */

/**
 * An abstract class representing a client for 3rd party API communication, including REST, gRPC,
 * etc.
 *
 * @param <C> the type of the context
 * @param <R> the type of the result
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
