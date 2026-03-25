package org.hermi.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.commons.validation.Validatable;
import org.hermi.usecase.UseCase;

public abstract class DispatcherUseCase<C extends Validatable, R> extends UseCase<C, R> {
  private final List<Handler<C, R>> handlers;

  @SuppressWarnings("unchecked")
  public DispatcherUseCase(Handler<C, R>... handlers) {
    this(List.of(handlers));
  }

  public DispatcherUseCase(List<Handler<C, R>> handlers) {
    this.handlers = new ArrayList<>();
    this.handlers.addAll(handlers);
  }

  public void register(Handler<C, R> handler) {
    this.handlers.add(handler);
  }

  @Override
  public R doExecute(C input) {
    for (Handler<C, R> handler : handlers) {
      if (handler.support(input)) {
        return handler.execute(input);
      }
    }
    throw new HandlerNotFoundException(
        getSimpleClassName() + ": No handler found for input: " + input);
  }
}
