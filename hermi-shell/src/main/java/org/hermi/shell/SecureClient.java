package org.hermi.shell;

import java.util.Objects;

/**
 * A specialized extension of {@link Client} that enforces cryptographic security over the external
 * interaction boundary.
 *
 * <p>This class implements the "Fail-Secure" integration pattern by transparently encrypting
 * outgoing vendor requests and decrypting incoming vendor responses through an injected {@link
 * Cryptor}. The protocol implementation is delegated entirely to the subclass via encrypted string
 * payloads.
 *
 * @param <Req> Vendor request type (Pre-encryption Domain Object)
 * @param <Res> Vendor response type (Post-decryption Domain Object)
 */
public abstract class SecureClient<Req, Res> extends Client<Req, Res> {

  private final Cryptor<Req, Res> cryptor;

  /**
   * Constructs a SecureClient with both an auditor and a cryptor.
   *
   * @param auditor the auditor to trace and persist interactions
   * @param cryptor the cryptor to encrypt and decrypt vendor payloads
   */
  protected SecureClient(Auditor<Req, Res> auditor, Cryptor<Req, Res> cryptor) {
    super(auditor);
    this.cryptor = Objects.requireNonNull(cryptor, "Cryptor is required for SecureClient");
  }

  /**
   * Implementation hook for transmitting the physical, securely encrypted payload over the wire.
   *
   * @param encryptedRequest the fully encrypted string payload ready for transit
   * @return the raw, encrypted response string from the vendor
   */
  protected abstract String doExchange(String encryptedRequest);

  @Override
  protected Res doExchange(Req request) {
    String encryptedRequest = cryptor.encrypt(request);
    String encryptedResponse = doExchange(encryptedRequest);
    return cryptor.decrypt(encryptedResponse);
  }
}
