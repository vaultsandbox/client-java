package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.model.EncryptedPayload;

/**
 * Cryptographic utilities for advanced users.
 *
 * <p>Most users do not need to use these methods directly. They are provided for advanced use cases
 * such as custom decryption workflows or signature verification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Keypair keypair = CryptoUtils.generateKeypair();
 * byte[] decrypted = CryptoUtils.decrypt(encryptedPayload, keypair.getSecretKey());
 * boolean valid = CryptoUtils.verifySignature(encryptedPayload);
 * }</pre>
 */
public final class CryptoUtils {

  private static final KeypairGenerator KEYPAIR_GENERATOR = new KeypairGenerator();
  private static final Decryptor DECRYPTOR = new Decryptor();
  private static final SignatureVerifier SIGNATURE_VERIFIER = new SignatureVerifier();

  private CryptoUtils() {} // Prevent instantiation

  /**
   * Generates a new ML-KEM-768 (Kyber768) keypair.
   *
   * @return a new keypair with public and secret keys
   */
  public static Keypair generateKeypair() {
    return KEYPAIR_GENERATOR.generate();
  }

  /**
   * Decrypts an encrypted payload using the provided secret key.
   *
   * @param payload the encrypted payload
   * @param secretKey the ML-KEM-768 secret key
   * @return the decrypted bytes
   * @throws com.vaultsandbox.client.exception.DecryptionException if decryption fails
   */
  public static byte[] decrypt(EncryptedPayload payload, byte[] secretKey) {
    return DECRYPTOR.decrypt(payload, secretKey);
  }

  /**
   * Verifies the ML-DSA-65 signature on an encrypted payload.
   *
   * @param payload the encrypted payload with signature
   * @return true if signature is valid
   * @throws com.vaultsandbox.client.exception.SignatureVerificationException if verification fails
   */
  public static boolean verifySignature(EncryptedPayload payload) {
    return SIGNATURE_VERIFIER.verify(payload);
  }

  /**
   * Safely verifies signature without throwing exceptions.
   *
   * @param payload the encrypted payload with signature
   * @return true if signature is valid, false otherwise
   */
  public static boolean verifySignatureSafe(EncryptedPayload payload) {
    try {
      return SIGNATURE_VERIFIER.verify(payload);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Encodes bytes to base64url (no padding).
   *
   * @param data the bytes to encode
   * @return base64url encoded string
   */
  public static String toBase64Url(byte[] data) {
    return Base64Url.encode(data);
  }

  /**
   * Decodes base64url string to bytes.
   *
   * @param base64url the base64url string
   * @return decoded bytes
   */
  public static byte[] fromBase64Url(String base64url) {
    return Base64Url.decode(base64url);
  }
}
