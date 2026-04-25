package org.hermi.shell;

import java.util.UUID;

public abstract class CipherClient<Req, Res> extends Client<Req, Res> {

  protected abstract String encrypt(Req request);

  protected abstract String doExchange(String ecryptedRequest);

  protected abstract Res decrypt(String encryptedResponse);

  @Override
  protected Res doExchange(Req resuest) {
    UUID requestId = saveRequest(resuest);
    String encryptedRequest = encrypt(resuest);
    String encryptedResponse = doExchange(encryptedRequest);
    Res response = decrypt(encryptedResponse);
    saveResult(requestId, response);
    return response;
  }
}
