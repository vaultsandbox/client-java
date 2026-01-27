package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.model.EncryptedPayload;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

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
 * boolean valid = CryptoUtils.verifySignature(encryptedPayload, pinnedServerKey);
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
   * Verifies the ML-DSA-65 signature on an encrypted payload with full validation.
   *
   * <p>This performs all validations per VaultSandbox spec §8.1:
   *
   * <ul>
   *   <li>Version validation (must be 1)
   *   <li>Algorithm validation
   *   <li>Size validation for all binary fields
   *   <li>Server key pinning verification
   *   <li>ML-DSA-65 signature verification
   * </ul>
   *
   * @param payload the encrypted payload with signature
   * @param pinnedServerKey the server's public key from inbox creation (base64url)
   * @return true if signature is valid
   * @throws com.vaultsandbox.client.exception.SignatureVerificationException if verification fails
   */
  public static boolean verifySignature(EncryptedPayload payload, String pinnedServerKey) {
    return SIGNATURE_VERIFIER.verify(payload, pinnedServerKey);
  }

  /**
   * Safely verifies signature without throwing exceptions.
   *
   * @param payload the encrypted payload with signature
   * @param pinnedServerKey the server's public key from inbox creation (base64url)
   * @return true if signature is valid, false otherwise
   */
  public static boolean verifySignatureSafe(EncryptedPayload payload, String pinnedServerKey) {
    try {
      return SIGNATURE_VERIFIER.verify(payload, pinnedServerKey);
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

  /**
   * Computes the emails hash for a list of email IDs per VaultSandbox spec.
   *
   * <p>Hash = base64url(sha256(sortedIds.join(",")))
   *
   * <p>The hash is computed on-demand, never stored.
   *
   * @param emailIds list of email IDs
   * @return base64url encoded SHA-256 hash
   */
  public static String computeEmailsHash(List<String> emailIds) {
    String joined = emailIds.stream().sorted().collect(Collectors.joining(","));

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
      return Base64Url.encode(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  /**
   * Concatenates multiple byte arrays into a single byte array.
   *
   * @param arrays the byte arrays to concatenate
   * @return a new byte array containing all input arrays concatenated in order
   */
  public static byte[] concat(byte[]... arrays) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      for (byte[] array : arrays) {
        bos.write(array);
      }
      return bos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to concatenate byte arrays", e);
    }
  }
}
