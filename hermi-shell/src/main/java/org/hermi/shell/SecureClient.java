package org.hermi.shell;

import java.util.Objects;

public abstract class SecureClient<Req, Res> extends Client<Req, Res> {

  private final Cryptor<Req, Res> cryptor;

  protected SecureClient(Auditor<Req, Res> auditor, Cryptor<Req, Res> cryptor) {
    super(auditor);
    this.cryptor = Objects.requireNonNull(cryptor, "Cryptor is required for SecureClient");
  }

  protected abstract String doExchange(String ecryptedRequest);

  @Override
  protected Res doExchange(Req request) {
    String encryptedRequest = cryptor.encrypt(request);
    String encryptedResponse = doExchange(encryptedRequest);
    return cryptor.decrypt(encryptedResponse);
  }
}
