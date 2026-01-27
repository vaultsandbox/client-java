package com.vaultsandbox.client.crypto;

import java.io.Closeable;
import java.util.Arrays;

/**
 * Holds a cryptographic keypair (public and secret keys).
 *
 * <p>This class implements {@link Closeable} to allow secure zeroing of secret key material. Call
 * {@link #close()} when done with the keypair to clear sensitive data from memory.
 */
public class Keypair implements Closeable {
  private final byte[] publicKey;
  private final byte[] secretKey;
  private volatile boolean cleared = false;

  public Keypair(byte[] publicKey, byte[] secretKey) {
    this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
    this.secretKey = Arrays.copyOf(secretKey, secretKey.length);
  }

  public byte[] getPublicKey() {
    return Arrays.copyOf(publicKey, publicKey.length);
  }

  public byte[] getSecretKey() {
    checkNotCleared();
    return Arrays.copyOf(secretKey, secretKey.length);
  }

  public String getPublicKeyBase64Url() {
    return Base64Url.encode(publicKey);
  }

  public String getSecretKeyBase64Url() {
    checkNotCleared();
    return Base64Url.encode(secretKey);
  }

  /**
   * Returns whether this keypair has been cleared.
   *
   * @return true if the secret key has been zeroed out
   */
  public boolean isCleared() {
    return cleared;
  }

  private void checkNotCleared() {
    if (cleared) {
      throw new IllegalStateException("Keypair has been cleared");
    }
  }

  /**
   * Clears the secret key material from memory.
   *
   * <p>After calling this method, attempts to access the secret key will throw {@link
   * IllegalStateException}. This method is idempotent.
   */
  @Override
  public void close() {
    if (!cleared) {
      Arrays.fill(secretKey, (byte) 0);
      cleared = true;
    }
  }
}
