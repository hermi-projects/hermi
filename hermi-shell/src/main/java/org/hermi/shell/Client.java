package org.hermi.shell;

import org.hermi.commons.Executor;

public abstract class Client<Req, Res> extends Executor<Req, Res> {

  protected abstract void beforeExchange(Req request);

  protected abstract Res doExchange(Req resuest);

  protected abstract void afterExchange(Req request, Res response);

  public Res exchange(Req request) {
    beforeExchange(request);
    Res response = this.execute(request);
    afterExchange(request, response);
    return response;
  }

  @Override
  protected Res doExecute(Req request) {
    return this.doExchange(request);
  }
}
