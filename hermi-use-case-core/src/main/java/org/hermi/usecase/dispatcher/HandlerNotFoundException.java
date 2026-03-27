package org.hermi.usecase.dispatcher;

public class HandlerNotFoundException extends RuntimeException {
  public HandlerNotFoundException(String message) {
    super(message);
  }
}
