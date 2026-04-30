package org.hermi.shell.secure;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Secure Cryptographic Adapter.
 *     <p>DESIGN INTENT: Decouple encryption/decryption complexity from protocol execution.
 *     <p>PURPOSE: Ensure {@link SecureClient} remains focused on transport while cryptographic
 *     decisions are isolated.
 *     <p>Phase: 2
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. Do not define instance
 *           variables to store request-specific data.
 *       <li>2. NO LOGGING PLAINTEXT: NEVER log plaintext input or decrypted output (PII/Sensitive
 *           data).
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
 * Interface for cryptographic operations required for secure payload transit.
 *
 * @param <I> input type (raw vendor request)
 * @param <O> output type (decrypted vendor response)
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
