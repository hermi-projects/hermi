package org.hermi.dispatcher;

public class HandlerNotFoundException extends RuntimeException {
  public HandlerNotFoundException(String message) {
    super(message);
  }
}
