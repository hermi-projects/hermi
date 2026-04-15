package org.hermi.usecase.dispatcher;

import java.util.ArrayList;
import java.util.List;
import org.hermi.usecase.standard.UseCase;
import org.hermi.validation.Validatable;

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
  protected R doExecute(C context) {
    for (Handler<C, R> handler : handlers) {
      if (handler.support(context)) {
        return handler.execute(context);
      }
    }
    throw new HandlerNotFoundException(
        getSimpleClassName() + ": No handler found for context: " + context);
  }
}
