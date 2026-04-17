package org.hermi.shell;

import org.hermi.commons.Executor;

public abstract class Messenger<M, R> extends Executor<M, R> {
  protected abstract void beforePublish(M message);

  protected abstract R doPublish(M message);

  protected abstract void afterPublish(M message, R result);

  public R publish(M message) {
    beforePublish(message);
    R result = execute(message);
    afterPublish(message, result);
    return result;
  }

  @Override
  protected R doExecute(M message) {
    return doPublish(message);
  }
}
