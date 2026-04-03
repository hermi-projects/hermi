package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

/**
 * An abstract class representing a client for 3rd party API communication, including REST, gRPC,
 * etc.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
public abstract class Client<I, O extends Validatable> extends Executor<I, O> {
  /**
   * Calls the external system with the client request and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer (Phase 1)</b>: Defines the contract by extending this class and
   *       specifying the input type {@code I} and output type {@code O}. Only the Output (which
   *       returns to the Use Case) typically implements {@link
   *       org.hermi.usecase.commons.validation.Validatable Validatable}.
   *   <li><b>Shell Layer (Phase 2)</b>: Implements the real-world communication logic using
   *       specific technologies, prefixed with the technology name (e.g., {@code
   *       RestFindUserClient}).
   * </ul>
   *
   * <p>Example Use Case Layer (Phase 1):
   *
   * <pre>{@code
   * public abstract class FindUserClient extends Client<FindUserClient.Input, FindUserClient.Output> {
   *   public static record Input(String ssn) {}
   *   public static record Output(String name, String email) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example Shell Layer (Phase 2):
   *
   * <pre>{@code
   * @Component
   * public class LexisNexisFindUserClient extends FindUserClient
   *     implements ClientAdapter<ApiRequest, ApiResponse, FindUserClient.Input, FindUserClient.Output> {
   *
   *   private final RestTemplate restTemplate;
   *
   *   @Override
   *   protected Output doCall(Input input) {
   *     ApiRequest apiRequest = convertInput(input);
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
   * @param input the client request input
   * @return the client response output
   */
  protected abstract O doCall(I input);

  public O call(I input) {
    return run(input);
  }

  public O call(Convertible<I> convertibleInput) {
    Objects.requireNonNull(
        convertibleInput, getSimpleClassName() + ", convertible input cannot be null");
    return call(convertibleInput.convert());
  }

  public <S> O call(S source, Converter<S, I> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return call(converter.convert(source));
  }

  @Override
  protected O doRun(I input) {
    return doCall(input);
  }
}
