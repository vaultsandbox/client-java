package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.exception.DecryptionException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

public class KeypairGenerator {
  // Raw ML-KEM-768 key sizes
  public static final int PUBLIC_KEY_SIZE = 1184;
  public static final int SECRET_KEY_SIZE = 2400;

  private final SecureRandom random;

  public KeypairGenerator() {
    CryptoProvider.ensureInitialized();
    this.random = new SecureRandom();
  }

  /**
   * Generate a new ML-KEM-768 keypair with raw key format. Raw keys are required by the
   * VaultSandbox API and SMTP server. Uses Bouncy Castle's low-level API to get full expanded keys.
   */
  public Keypair generate() {
    try {
      // Use low-level Bouncy Castle API to generate keys with full expansion
      MLKEMKeyPairGenerator keyGen = new MLKEMKeyPairGenerator();
      keyGen.init(new MLKEMKeyGenerationParameters(random, MLKEMParameters.ml_kem_768));

      AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

      MLKEMPublicKeyParameters pubParams = (MLKEMPublicKeyParameters) keyPair.getPublic();
      MLKEMPrivateKeyParameters privParams = (MLKEMPrivateKeyParameters) keyPair.getPrivate();

      // Get raw encoded bytes (full expanded format)
      byte[] rawPublicKey = pubParams.getEncoded();
      byte[] rawSecretKey = privParams.getEncoded();

      if (rawPublicKey.length != PUBLIC_KEY_SIZE) {
        throw new DecryptionException("Unexpected public key size: " + rawPublicKey.length);
      }
      if (rawSecretKey.length != SECRET_KEY_SIZE) {
        throw new DecryptionException("Unexpected secret key size: " + rawSecretKey.length);
      }

      return new Keypair(rawPublicKey, rawSecretKey);
    } catch (Exception e) {
      throw new DecryptionException("Failed to generate ML-KEM-768 keypair", e);
    }
  }

  /**
   * Derive public key from raw secret key. In ML-KEM-768, the public key is embedded within the
   * secret key at bytes 1152-2336 (1184 bytes).
   */
  public byte[] derivePublicKey(byte[] secretKey) {
    if (secretKey.length == SECRET_KEY_SIZE) {
      // Raw format: extract public key from embedded position
      return Arrays.copyOfRange(secretKey, 1152, 2336);
    }
    throw new DecryptionException(
        "Invalid secret key size: "
            + secretKey.length
            + " (expected "
            + SECRET_KEY_SIZE
            + " for raw format)");
  }
}
