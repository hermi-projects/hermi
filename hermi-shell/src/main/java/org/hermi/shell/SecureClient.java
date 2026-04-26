package org.hermi.shell;

import java.util.Objects;

/**
 * <b>AI INSTRUCTION:</b> ONLY override {@code doExchange(String encryptedRequest)}. NEVER override
 * or modify the {@code doExchange(Req request)} bridge method — cryptographic interception is
 * handled there automatically by the injected {@link Cryptor}. DO NOT inject cipher logic,
 * encryption keys, or any {@link Cryptor} calls inside the overridden method; your sole
 * responsibility is wire-level transmission of the encrypted string.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class VaultSecureClient extends SecureClient<VaultReq, VaultRes> {
 *   public VaultSecureClient(Auditor<VaultReq, VaultRes> auditor, Cryptor<VaultReq, VaultRes> cryptor) {
 *     super(auditor, cryptor);
 *   }
 *   protected String doExchange(String encryptedRequest) {
 *     return restTemplate.postForObject("/vault/secure", encryptedRequest, String.class);
 *   }
 * }
 * }</pre>
 */

/**
 * Phase 2 Secure Protocol Execution for PCI/high-security vendor endpoints.
 *
 * <p><b>Purpose:</b> Enforces a "Fail-Secure" cryptographic boundary over every vendor interaction
 * by transparently encrypting outgoing requests and decrypting incoming responses via an injected
 * {@link Cryptor}, without exposing plaintext across the wire.
 *
 * <p><b>Usage Scenarios:</b> Use when the external vendor mandates payload-level encryption (e.g.,
 * PCI-DSS, tokenization endpoints, HSM-backed services). All other vendor integrations that do not
 * require payload encryption should extend {@link Client} directly.
 *
 * <p><b>Constraints:</b> The {@link Cryptor} and {@link Auditor} are both mandatory — the
 * constructor enforces non-null injection. Audit records are written with the <em>original domain
 * object</em> (pre-encryption), not the encrypted string.
 *
 * <p><b>Dependencies:</b> Requires a concrete {@link Cryptor} implementation and a concrete {@link
 * Auditor} implementation, both bound to the same {@code <Req, Res>} types.
 *
 * @param <Req> Vendor request type (pre-encryption domain object)
 * @param <Res> Vendor response type (post-decryption domain object)
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
