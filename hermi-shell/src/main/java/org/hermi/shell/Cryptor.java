package org.hermi.shell;

public interface Cryptor<I, O> {
  String encrypt(I input);

  O decrypt(String encryptedOutput);
}
