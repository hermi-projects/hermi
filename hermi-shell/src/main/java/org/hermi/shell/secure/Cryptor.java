package org.hermi.shell.secure;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Secure Cryptographic Adapter.
 *     <p>DESIGN INTENT: Decouple seal/unseal complexity from protocol execution.
 *     <p>PURPOSE: Ensure {@link SecureClient} remains focused on transport while cryptographic
 *     decisions are isolated.
 *     <p>Phase: 2
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. Do not define instance
 *           variables to store request-specific data.
 *       <li>2. NO LOGGING PLAINTEXT: NEVER log input or unsealed output (PII/Sensitive data).
 *       <li>3. NO HARDCODED KEYS: Always inject encryption keys via {@code @Value} or secret
 *           managers.
 *       <li>4. DELEGATE CRYPTO: NEVER implement internal cipher algorithms; use JCE providers or
 *           proven libraries (e.g., Bouncy Castle, Google Tink).
 *       <li>5. ALGORITHM SAFETY: Use AES-GCM or better. Forbid DES, 3DES, MD5, SHA-1, or ECB mode.
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER implement custom "roll-your-own" crypto logic.
 *       <li>DO NOT inject a Cryptor into plain {@link Client} or Use Case layer components.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * @Component
 * public class AesGcmCryptor implements Cryptor<VaultReq, VaultRes> {
 *     @Value("${vendor.aes.key}") private String base64Key;
 *
 *     @Override
 *     public String seal(VaultReq payload) {
 *         // Use AES-GCM via JCE — NEVER log 'payload'
 *         return AesGcmUtil.encrypt(base64Key, serialize(payload));
 *     }
 *
 *     @Override
 *     public VaultRes unseal(String encryptedResponse) {
 *         // Use AES-GCM via JCE — NEVER log unsealed result
 *         return deserialize(AesGcmUtil.decrypt(base64Key, encryptedResponse));
 *     }
 * }
 * }</pre>
 */

/**
 * Interface for cryptographic operations required for secure payload transit.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public interface Cryptor<P, R> {

  /**
   * Seals (serializes + encrypts) the raw input payload before transmission.
   *
   * @param payload the raw input payload
   * @return the encrypted string representation to be transmitted to the external system
   */
  String seal(P payload);

  /**
   * Unseals (decrypts + deserializes) the raw encrypted response from the external system back into
   * the result type.
   *
   * @param encryptedResponse the encrypted string representation received from the external system
   * @return the unsealed result data
   */
  R unseal(String encryptedResponse);
}
