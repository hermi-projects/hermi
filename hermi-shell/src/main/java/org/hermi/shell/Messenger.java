package org.hermi.shell;

import java.util.UUID;
import org.hermi.commons.Executor;

public abstract class Messenger<M, R> extends Executor<M, R> {
  protected abstract UUID saveMessage(M message);

  protected abstract R doPublish(M message);

  protected abstract void saveResult(UUID messageId, R result);

  public R publish(M message) {
    UUID messageId = saveMessage(message);
    R result = execute(message);
    saveResult(messageId, result);
    return result;
  }

  @Override
  protected R doExecute(M message) {
    return doPublish(message);
  }
}
