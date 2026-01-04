package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.exception.DecryptionException;
import com.vaultsandbox.client.model.Algorithms;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.security.MessageDigest;

/**
 * Validates encrypted payloads according to VaultSandbox spec §8.1.
 *
 * <p>Validation steps per spec:
 *
 * <ol>
 *   <li>Parse payload (done by JSON deserializer)
 *   <li>Validate version == 1
 *   <li>Validate algorithm identifiers
 *   <li>Validate decoded binary field sizes
 *   <li>Verify server key matches pinned key
 * </ol>
 */
public class PayloadValidator {

  // Expected algorithm identifiers per spec §3.1
  private static final String EXPECTED_KEM = "ML-KEM-768";
  private static final String EXPECTED_SIG = "ML-DSA-65";
  private static final String EXPECTED_AEAD = "AES-256-GCM";
  private static final String EXPECTED_KDF = "HKDF-SHA-512";

  // Expected sizes per spec §5.3
  private static final int EXPECTED_CT_KEM_SIZE = 1088;
  private static final int EXPECTED_NONCE_SIZE = 12;
  private static final int EXPECTED_SIG_SIZE = 3309;
  private static final int EXPECTED_SERVER_SIG_PK_SIZE = 1952;

  /**
   * Validates an encrypted payload according to spec §8.1 steps 2-5.
   *
   * @param payload the encrypted payload to validate
   * @param pinnedServerKey the server signature public key from inbox creation (base64url encoded)
   * @throws DecryptionException if validation fails
   */
  public void validate(EncryptedPayload payload, String pinnedServerKey) {
    // Step 2: Validate version (spec §8.1)
    if (payload.getV() != 1) {
      throw new DecryptionException("Unsupported protocol version: " + payload.getV());
    }

    // Step 3: Validate algorithms (spec §8.1)
    validateAlgorithms(payload.getAlgs());

    // Step 4: Validate sizes (spec §8.1)
    validateSizes(payload);

    // Step 5: Verify server key matches pinned key (spec §8.1)
    validateServerKey(payload.getServerSigPk(), pinnedServerKey);
  }

  private void validateAlgorithms(Algorithms algs) {
    if (algs == null) {
      throw new DecryptionException("Missing algorithm specification");
    }
    if (!EXPECTED_KEM.equals(algs.getKem())) {
      throw new DecryptionException("Invalid KEM algorithm: " + algs.getKem());
    }
    if (!EXPECTED_SIG.equals(algs.getSig())) {
      throw new DecryptionException("Invalid signature algorithm: " + algs.getSig());
    }
    if (!EXPECTED_AEAD.equals(algs.getAead())) {
      throw new DecryptionException("Invalid AEAD algorithm: " + algs.getAead());
    }
    if (!EXPECTED_KDF.equals(algs.getKdf())) {
      throw new DecryptionException("Invalid KDF algorithm: " + algs.getKdf());
    }
  }

  private void validateSizes(EncryptedPayload payload) {
    // Decode and check ct_kem size
    byte[] ctKem = Base64Url.decode(payload.getCtKem());
    if (ctKem.length != EXPECTED_CT_KEM_SIZE) {
      throw new DecryptionException(
          "Invalid ct_kem size: " + ctKem.length + " (expected " + EXPECTED_CT_KEM_SIZE + ")");
    }

    // Decode and check nonce size
    byte[] nonce = Base64Url.decode(payload.getNonce());
    if (nonce.length != EXPECTED_NONCE_SIZE) {
      throw new DecryptionException(
          "Invalid nonce size: " + nonce.length + " (expected " + EXPECTED_NONCE_SIZE + ")");
    }

    // Decode and check signature size
    byte[] sig = Base64Url.decode(payload.getSig());
    if (sig.length != EXPECTED_SIG_SIZE) {
      throw new DecryptionException(
          "Invalid signature size: " + sig.length + " (expected " + EXPECTED_SIG_SIZE + ")");
    }

    // Decode and check server_sig_pk size
    byte[] serverSigPk = Base64Url.decode(payload.getServerSigPk());
    if (serverSigPk.length != EXPECTED_SERVER_SIG_PK_SIZE) {
      throw new DecryptionException(
          "Invalid server_sig_pk size: "
              + serverSigPk.length
              + " (expected "
              + EXPECTED_SERVER_SIG_PK_SIZE
              + ")");
    }
  }

  /**
   * Validates that the server signature public key in the payload matches the pinned key.
   *
   * <p>Per spec §11.3, this comparison MUST be constant-time.
   */
  private void validateServerKey(String payloadServerKey, String pinnedServerKey) {
    if (pinnedServerKey == null) {
      throw new DecryptionException("No pinned server key available for verification");
    }

    byte[] payloadKey = Base64Url.decode(payloadServerKey);
    byte[] pinnedKey = Base64Url.decode(pinnedServerKey);

    // Per spec §11.3: Use constant-time comparison
    if (!MessageDigest.isEqual(payloadKey, pinnedKey)) {
      throw new DecryptionException("Server key mismatch: payload key does not match pinned key");
    }
  }
}
