package org.hermi.shell;

import org.hermi.commons.Executor;

public abstract class Messenger<M, R> extends Executor<M, R> {
  protected abstract void saveMessage(M message);

  protected abstract R doPublish(M message);

  protected abstract void saveResult(M message, R result);

  public R publish(M message) {
    saveMessage(message);
    R result = execute(message);
    saveResult(message, result);
    return result;
  }

  @Override
  protected R doExecute(M message) {
    return doPublish(message);
  }
}
