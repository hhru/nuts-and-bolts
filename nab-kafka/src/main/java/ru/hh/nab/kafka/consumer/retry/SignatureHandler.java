package ru.hh.nab.kafka.consumer.retry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Identifies how message should be signed and verified
 * @param <T> type of the message value
 */
public interface SignatureHandler<T> {

  static <T> SignatureHandler<T> bySecret(String secret) {

    byte[] secretBytes = Objects.requireNonNull(secret).getBytes(StandardCharsets.UTF_8);

    return new SignatureHandler<T> () {

      @Override
      public byte[] signMessage(T message) {
        return secretBytes;
      }

      @Override
      public boolean verifyMessage(T message, byte[] signature) {
        return Arrays.equals(secretBytes, signature);
      }
    };
  }

  /**
   * Signs the given message and returns the signature.
   *
   * @param message the message to be signed
   * @return the generated signature
   */
  byte[] signMessage(T message);

  /**
   * Verifies the signature of the given message.
   *
   * @param message the original message
   * @param signature the signature to verify
   * @return true if the signature is valid, false otherwise
   */
  boolean verifyMessage(T message, byte[] signature);
}
