package org.hermi.usecase;

import org.hermi.commons.execution.Executor;
import org.hermi.commons.validation.Validatable;

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
