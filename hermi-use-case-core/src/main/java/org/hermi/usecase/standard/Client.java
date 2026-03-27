package org.hermi.usecase.standard;

import org.hermi.usecase.commons.execution.Executor;
import org.hermi.usecase.commons.validation.Validatable;

public abstract class Client<C, R extends Validatable> extends Executor<C, R> {
  protected abstract R doSend(C command);

  public R send(C command) {
    return run(command);
  }

  @Override
  protected R doRun(C command) {
    return doSend(command);
  }
}
