package org.hermi.usecase;

import org.hermi.usecase.util.Executor;

public abstract class Messenger<C, R> extends Executor<C, R> {
  protected abstract R doSend(C command);

  public R send(C command) {
    return run(command);
  }

  @Override
  protected R doRun(C command) {
    return doSend(command);
  }
}
