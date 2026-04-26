package org.hermi.shell;

/**
 * <b>AI INSTRUCTION:</b> When implementing this interface, follow these security rules: NEVER log
 * the plaintext input or the decrypted output — both contain sensitive data. NEVER hardcode
 * encryption keys — always inject them via {@code @Value} or a secrets manager (e.g., Vault). NEVER
 * implement your own cipher algorithm — delegate to a proven JCE provider or a dedicated library
 * (e.g., Bouncy Castle, Google Tink). DO NOT use weak or deprecated algorithms (DES, 3DES, MD5,
 * SHA-1, ECB mode).
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class AesGcmCryptor implements Cryptor<VaultReq, VaultRes> {
 *   @Value("${vendor.aes.key}") private String base64Key;
 *
 *   public String encrypt(VaultReq input) {
 *     // Use AES-GCM via JCE — do NOT log input
 *     return AesGcmUtil.encrypt(base64Key, serialize(input));
 *   }
 *   public VaultRes decrypt(String encryptedOutput) {
 *     // Use AES-GCM via JCE — do NOT log encryptedOutput
 *     return deserialize(AesGcmUtil.decrypt(base64Key, encryptedOutput));
 *   }
 * }
 * }</pre>
 */

/**
 * Phase 2 Cryptographic Contract for secure payload transit.
 *
 * <p><b>Purpose:</b> Decouples encryption/decryption complexity from protocol execution, ensuring
 * the {@link SecureClient} remains focused on wire-level transport while all cryptographic
 * decisions are isolated and independently replaceable.
 *
 * <p><b>Usage Scenarios:</b> Implement this interface when a specific external vendor requires a
 * proprietary or non-standard encryption scheme. For standard TLS-only vendors, use {@link Client}
 * directly and do not implement a Cryptor.
 *
 * <p><b>Constraints:</b> Implementations MUST operate statelessly (no mutable fields that hold
 * decrypted data between calls). Must be thread-safe as a Spring singleton.
 *
 * <p><b>Dependencies:</b> Pair exclusively with {@link SecureClient}. Never inject a Cryptor into a
 * plain {@link Client} or any Use Case layer component.
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
