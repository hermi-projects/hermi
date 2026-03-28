package org.hermi.usecase.standard;

import java.util.Objects;
import org.hermi.usecase.commons.conversion.Converter;
import org.hermi.usecase.commons.conversion.Convertible;
import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

public abstract class Messenger<C, R extends Validatable> extends Executor<C, R> {

  protected abstract R doSend(C command);

  public R send(C command) {
    return run(command);
  }

  public R execute(Convertible<C> command) {
    Objects.requireNonNull(command, getSimpleClassName() + ", convertible command cannot be null");
    return send(command.convert());
  }

  public <S> R execute(S source, Converter<S, C> converter) {
    Objects.requireNonNull(source, getSimpleClassName() + ", source cannot be null");
    Objects.requireNonNull(converter, getSimpleClassName() + ", converter cannot be null");
    return send(converter.convert(source));
  }

  @Override
  protected R doRun(C command) {
    return doSend(command);
  }
}
