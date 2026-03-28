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
 * @param <C> the type of the command
 * @param <R> the type of the response
 */
public abstract class Client<C, R extends Validatable> extends Executor<C, R> {
  /**
   * Sends the client request to an external system and returns the response.
   *
   * <ul>
   *   <li><b>Use Case Layer</b>: Defines the contract by extending this class and specifying the
   *       command type {@code C} and result type {@code R}.
   *   <li><b>Shell Layer</b>: Implements the real-world communication logic, typically by adopting
   *       the adapter design pattern by implementing {@link org.hermi.shell.adapter.ClientAdapter
   *       ClientAdapter} to coordinate data transformation and API execution.
   * </ul>
   *
   * <p>Example Use Case Layer:
   *
   * <pre>{@code
   * public abstract class FindUserClient extends Client<FindUserClient.Command, FindUserClient.Result> {
   *   public static record Command(@NotNull @NotBlank String ssn) implements Validatable {}
   *   public static record Result(String name, String email) implements Validatable {}
   * }
   * }</pre>
   *
   * <p>Example Shell Layer (Implementation):
   *
   * <pre>{@code
   * public class DefaultFindUserClient extends FindUserClient
   *     implements ClientAdapter<ApiRequest, ApiResponse, FindUserClient.Command, FindUserClient.Result> {
   *
   *   private final RestTemplate restTemplate;
   *
   *   @Override
   *   protected Result doSend(Command command) {
   *     ApiRequest apiRequest = convertCommand(command);
   *     ApiResponse apiResponse = process(apiRequest);
   *     return convertResult(apiResponse);
   *   }
   *
   *   @Override
   *   public ApiRequest convertCommand(Command command) {
   *     return new ApiRequest(command.ssn());
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
   * @param command the client request command
   * @return the client response result
   */
  protected abstract R doSend(C command);

  public R send(C command) {
    return run(command);
  }

  public R send(Convertible<C> convertibleCommand) {
    Objects.requireNonNull(
        convertibleCommand, getSimpleClassName() + ", convertible command cannot be null");
    return send(convertibleCommand.convert());
  }

  public <S> R send(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return send(converter.convert(source));
  }

  @Override
  protected R doRun(C command) {
    return doSend(command);
  }
}
