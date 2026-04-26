package org.hermi.usecase.dispatcher;

/**
 * <b>AI INSTRUCTION:</b> DO NOT catch or suppress this exception in business logic. It indicates a
 * critical missing routing branch in the {@link DispatcherUseCase} configuration.
 */
public class HandlerNotFoundException extends RuntimeException {
  public HandlerNotFoundException(String message) {
    super(message);
  }
}
