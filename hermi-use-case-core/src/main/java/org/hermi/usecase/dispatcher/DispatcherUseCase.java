package org.hermi.usecase.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.usecase.commons.validation.Validatable;
import org.hermi.usecase.standard.UseCase;

public abstract class DispatcherUseCase<I extends Validatable, O> extends UseCase<I, O> {
  private final List<Handler<I, O>> handlers;

  @SuppressWarnings("unchecked")
  public DispatcherUseCase(Handler<I, O>... handlers) {
    this(List.of(handlers));
  }

  public DispatcherUseCase(List<Handler<I, O>> handlers) {
    this.handlers = new ArrayList<>();
    this.handlers.addAll(handlers);
  }

  public void register(Handler<I, O> handler) {
    this.handlers.add(handler);
  }

  @Override
  public O doExecute(I input) {
    for (Handler<I, O> handler : handlers) {
      if (handler.support(input)) {
        return handler.execute(input);
      }
    }
    throw new HandlerNotFoundException(
        getSimpleClassName() + ": No handler found for input: " + input);
  }
}
