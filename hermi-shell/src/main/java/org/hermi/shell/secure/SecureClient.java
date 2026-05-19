package org.hermi.shell.secure;

import java.util.Objects;
import org.hermi.shell.Client;
import org.hermi.shell.audit.PersistentAuditor;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: High-Security Cryptographic Interceptor (Protocol Layer).
 *     <p>DESIGN INTENT: Transparently enforce payload-level encryption for high-compliance vendor
 *     endpoints.
 *     <p>PURPOSE: Enforce a "Fail-Secure" boundary ensuring plaintext never touches the wire.
 *     <p>Phase: 2 (Refinement / Refactoring)
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless.
 *       <li>2. NO WIRE-PLAINTEXT: ONLY override {@code doExchange(String encryptedRequest)}.
 *       <li>3. NO KEY LEAKAGE: DO NOT inject cipher logic, encryption keys, or {@link Cryptor}
 *           calls inside the overridden method; transmission is your sole focus.
 *       <li>4. NAMING PROPHECY: Follow the {@code {Vendor}{Action}{Resource}SecureClient} pattern
 *           (e.g., {@code VaultFindCertSecureClient}).
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER override or modify the {@code final doExchange(P payload)} bridge method.
 *       <li>NEVER attempt to log the {@code encryptedRequest} unless it is for pre-approved
 *           technical debugging.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * @Component
 * public class VaultSecureClient extends SecureClient<VaultReq, VaultRes> {
 *   public VaultSecureClient(Auditor<VaultReq, VaultRes> auditor, Cryptor<VaultReq, VaultRes> cryptor) {
 *     super(auditor, cryptor);
 *   }
 *   @Override
 *   protected String doExchange(String encryptedRequest) {
 *     return restTemplate.postForObject("/vault/secure", encryptedRequest, String.class);
 *   }
 * }
 * }</pre>
 */

/**
 * High-security protocol execution base class for PCI/high-compliance vendor endpoints.
 *
 * @param <P> payload type sent to the external system (pre-encryption)
 * @param <R> result type received from the external system (post-decryption)
 */
public abstract class SecureClient<P, R> extends Client<P, R> {

  private final Cryptor<P, R> cryptor;

  /**
   * Constructs a SecureClient with both an auditor and a cryptor.
   *
   * @param auditor the auditor to trace and persist interactions
   * @param cryptor the cryptor to seal and unseal payloads
   */
  protected SecureClient(PersistentAuditor<P, R> auditor, Cryptor<P, R> cryptor) {
    super(auditor);
    this.cryptor = Objects.requireNonNull(cryptor, "Cryptor is required for SecureClient");
  }

  protected SecureClient(Cryptor<P, R> cryptor) {
    super();
    this.cryptor = Objects.requireNonNull(cryptor, "Cryptor is required for SecureClient");
  }

  /**
   * Implementation hook for transmitting the physical, securely encrypted payload over the wire.
   *
   * @param encryptedRequest the fully encrypted string payload ready for transit
   * @return the raw, encrypted response string from the external system
   */
  protected abstract String doExchange(String encryptedRequest);

  @Override
  protected final R doExchange(P payload) {
    String encryptedRequest = cryptor.seal(payload);
    String encryptedResponse = doExchange(encryptedRequest);
    return cryptor.unseal(encryptedResponse);
  }
}
