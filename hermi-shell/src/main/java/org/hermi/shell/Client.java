package org.hermi.shell;

import java.util.UUID;
import org.hermi.commons.Executor;

public abstract class Client<Req, Res> extends Executor<Req, Res> {

  protected abstract UUID saveRequest(Req request);

  protected abstract Res doExchange(Req resuest);

  protected abstract void saveResult(UUID requestId, Res response);

  public Res exchange(Req request) {
    UUID requestId = saveRequest(request);
    Res response = this.execute(request);
    saveResult(requestId, response);
    return response;
  }

  @Override
  protected Res doExecute(Req request) {
    return this.doExchange(request);
  }
}
