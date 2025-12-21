package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.exception.DecryptionException;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decryptor {
  private static final Logger log = LoggerFactory.getLogger(Decryptor.class);
  private static final int GCM_TAG_LENGTH_BITS = 128;

  private final HkdfDeriver hkdfDeriver;

  public Decryptor() {
    CryptoProvider.ensureInitialized();
    this.hkdfDeriver = new HkdfDeriver();
  }

  public byte[] decrypt(EncryptedPayload payload, byte[] secretKey) {
    log.trace(
        "Decrypting payload: ctKem={} bytes, ciphertext={} bytes",
        payload.getCtKem().length(),
        payload.getCiphertext().length());
    try {
      byte[] ctKem = Base64Url.decode(payload.getCtKem());
      byte[] nonce = Base64Url.decode(payload.getNonce());
      byte[] aad = Base64Url.decode(payload.getAad());
      byte[] ciphertext = Base64Url.decode(payload.getCiphertext());

      // Step 1: Decapsulate to get shared secret
      log.trace("Step 1: ML-KEM decapsulation");
      byte[] sharedSecret = decapsulate(ctKem, secretKey);

      // Step 2: Derive AES key using HKDF
      log.trace("Step 2: HKDF key derivation");
      byte[] aesKey = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);

      // Step 3: Decrypt using AES-256-GCM
      log.trace("Step 3: AES-GCM decryption");
      byte[] result = decryptAesGcm(aesKey, nonce, aad, ciphertext);
      log.trace("Decryption successful: {} bytes plaintext", result.length);
      return result;
    } catch (DecryptionException e) {
      log.error("Decryption failed: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Decryption failed: {}", e.getMessage());
      throw new DecryptionException("Decryption failed", e);
    }
  }

  /**
   * Decapsulate the KEM ciphertext using raw ML-KEM-768 secret key.
   *
   * @param ctKem KEM ciphertext
   * @param secretKey raw ML-KEM-768 secret key (2400 bytes)
   * @return shared secret
   */
  public byte[] decapsulate(byte[] ctKem, byte[] secretKey) {
    try {
      // Create ML-KEM private key parameters from raw bytes
      MLKEMPrivateKeyParameters privateKeyParams =
          new MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, secretKey);

      // Use Bouncy Castle's low-level extractor for decapsulation
      MLKEMExtractor extractor = new MLKEMExtractor(privateKeyParams);
      return extractor.extractSecret(ctKem);
    } catch (Exception e) {
      throw new DecryptionException("ML-KEM decapsulation failed", e);
    }
  }

  public byte[] decryptAesGcm(byte[] aesKey, byte[] nonce, byte[] aad, byte[] ciphertext) {
    try {
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
      SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
      cipher.updateAAD(aad);

      return cipher.doFinal(ciphertext);
    } catch (GeneralSecurityException e) {
      throw new DecryptionException("AES-GCM decryption failed", e);
    }
  }
}
