package org.hermi.shell;

import org.hermi.commons.Executor;

public abstract class Client<Req, Res> extends Executor<Req, Res> {

  protected abstract void saveRequest(Req request);

  protected abstract Res doExchange(Req resuest);

  protected abstract void saveResult(Req request, Res response);

  public Res exchange(Req request) {
    saveRequest(request);
    Res response = this.execute(request);
    saveResult(request, response);
    return response;
  }

  @Override
  protected Res doExecute(Req request) {
    return this.doExchange(request);
  }
}
