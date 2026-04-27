package org.hermi.shell;

/**
 * [AI ARCHITECTURAL CONTRACT] --------------------------------------------------
 *
 * @apiNote
 *     <p>ROLE: Secure Cryptographic Adapter for Phase 2 transit.
 *     <p>DESIGN INTENT: Decouples encryption/decryption complexity from protocol execution. Ensures
 *     {@link SecureClient} remains focused on transport while cryptographic decisions are isolated
 *     and independently replaceable.
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. Do not define instance
 *           variables to store request-specific data.
 *       <li>2. NO LOGGING PLAINTEXT: NEVER log plaintext input or decrypted output (PII/Sensitive
 *           data).
 *       <li>3. NO HARDCODED KEYS: Always inject encryption keys via {@code @Value} or secrets
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
 *     public String encrypt(VaultReq input) {
 *         // Use AES-GCM via JCE — NEVER log 'input'
 *         return AesGcmUtil.encrypt(base64Key, serialize(input));
 *     }
 *
 *     @Override
 *     public VaultRes decrypt(String encryptedOutput) {
 *         // Use AES-GCM via JCE — NEVER log decrypted result
 *         return deserialize(AesGcmUtil.decrypt(base64Key, encryptedOutput));
 *     }
 * }
 * }</pre>
 */

/**
 * Cryptographic Contract for secure payload transit.
 *
 * <p>This interface decouples encryption complexity from protocol transport. It is intended for use
 * when specific vendors require non-standard encryption schemes, allowing the system to handle
 * secure payloads without leaking cryptographic logic into the business or network layers.
 *
 * @param <I> Input type (vendor request payload, pre-encryption)
 * @param <O> Output type (vendor response payload, post-decryption)
 */
public interface Cryptor<I, O> {

  /**
   * Encrypts the raw vendor input payload before transmission.
   *
   * @param input the raw input payload
   * @return the encrypted string representation to be transmitted to the external system
   */
  String encrypt(I input);

  /**
   * Decrypts the raw encrypted response from the external system back into the vendor output
   * payload.
   *
   * @param encryptedOutput the encrypted string representation received from the external system
   * @return the decrypted output data
   */
  O decrypt(String encryptedOutput);
}
