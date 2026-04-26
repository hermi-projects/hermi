package org.hermi.shell;

/**
 * Generic interface for encrypting and decrypting external interaction payloads.
 *
 * <p>Separating cryptographic operations from protocol execution adheres to the "Same Reason to
 * Change" principle. This ensures that the primary execution logic remains pure and focused on the
 * interaction itself, while the {@code Cryptor} handles the complexities of securing the physical
 * data transit over the wire.
 *
 * @param <I> Input type (Request or Message payload)
 * @param <O> Output type (Response or Result payload)
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
