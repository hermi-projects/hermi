package org.hermi.usecase;

import java.util.Objects;
import org.hermi.commons.conversion.Converter;
import org.hermi.commons.conversion.Convertible;
import org.hermi.usecase.util.Executor;

public abstract class UseCase<C, R> extends Executor<C, R> {
  protected abstract R doExecute(C command);

  public R execute(C command) {
    return run(command);
  }

  public R execute(Convertible<C> command) {
    Objects.requireNonNull(command, getSimpleName() + ", convertible command cannot be null");
    return run(command.convert());
  }

  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleName() + ", converter cannot be null");
    return run(converter.convert(source));
  }

  @Override
  protected R doRun(C command) {
    return doExecute(command);
  }
}
