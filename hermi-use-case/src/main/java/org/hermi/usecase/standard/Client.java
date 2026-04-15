package org.hermi.usecase.standard;

import org.hermi.usecase.util.Executor;
import org.hermi.validation.Validatable;

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
   *     implements Adapter<ApiRequest, ApiResponse, FindUserClient.Input, FindUserClient.Output> {
   *
   *   private final RestTemplate restTemplate;
   *
   *   @Override
   *   protected Result doExecute(Context context) {
   *     ApiRequest apiRequest = convertInput(context);
   *     ApiResponse apiResponse = process(apiRequest);
   *     return convertOutput(apiResponse);
   *   }
   *
   *   @Override
   *   public ApiRequest convertInput(Input input) {
   *     return new ApiRequest(input.ssn());
   *   }
   *
   *   @Override
   *   public ApiResponse process(ApiRequest input) {
   *     return restTemplate.postForObject("/api/users", input, ApiResponse.class);
   *   }
   *
   *   @Override
   *   public Output convertOutput(ApiResponse output) {
   *     return new Output(output.getName(), output.getEmail());
   *   }
   * }
   * }</pre>
   *
   * @param context the client request context
   * @return the client response result
   */
  protected abstract R doExecute(C context);
}
