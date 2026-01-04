package com.vaultsandbox.client.crypto;

import com.vaultsandbox.client.exception.SignatureVerificationException;
import com.vaultsandbox.client.model.Algorithms;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;

public class SignatureVerifier {
  // ML-DSA-65 (Dilithium3) raw public key size: 1952 bytes
  private static final int ML_DSA_65_PUBLIC_KEY_SIZE = 1952;
  private static final String CONTEXT = "vaultsandbox:email:v1";

  private final PayloadValidator payloadValidator;

  public SignatureVerifier() {
    CryptoProvider.ensureInitialized();
    this.payloadValidator = new PayloadValidator();
  }

  /**
   * Verifies an encrypted payload with full validation per spec §8.1.
   *
   * <p>This method performs all required validations including:
   *
   * <ul>
   *   <li>Version validation (must be 1)
   *   <li>Algorithm validation
   *   <li>Size validation for all binary fields
   *   <li>Server key pinning verification
   *   <li>ML-DSA-65 signature verification
   * </ul>
   *
   * @param payload the encrypted payload to verify
   * @param pinnedServerKey the server's public key from inbox creation (base64url)
   * @return true if signature is valid
   * @throws SignatureVerificationException if verification fails
   */
  public boolean verify(EncryptedPayload payload, String pinnedServerKey) {
    try {
      // Validate payload per spec §8.1 steps 2-5
      payloadValidator.validate(payload, pinnedServerKey);

      // Step 6: Verify signature (spec §8.1)
      byte[] transcript = buildTranscript(payload);
      byte[] signature = Base64Url.decode(payload.getSig());
      byte[] serverSigPk = Base64Url.decode(payload.getServerSigPk());

      // Use raw public key with Bouncy Castle's low-level API
      MLDSAPublicKeyParameters publicKeyParams =
          new MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_65, serverSigPk);

      MLDSASigner verifier = new MLDSASigner();
      verifier.init(false, publicKeyParams);
      verifier.update(transcript, 0, transcript.length);
      return verifier.verifySignature(signature);
    } catch (SignatureVerificationException e) {
      throw e;
    } catch (Exception e) {
      throw new SignatureVerificationException("Signature verification failed", e);
    }
  }

  /**
   * Verifies signature and throws if invalid.
   *
   * @param payload the encrypted payload to verify
   * @param pinnedServerKey the server's public key from inbox creation (base64url)
   * @throws SignatureVerificationException if verification fails
   */
  public void verifyOrThrow(EncryptedPayload payload, String pinnedServerKey) {
    if (!verify(payload, pinnedServerKey)) {
      throw new SignatureVerificationException();
    }
  }

  private byte[] buildTranscript(EncryptedPayload p) {
    // version (1 byte) || algs_ciphersuite || context ||
    // ct_kem || nonce || aad || ciphertext || server_sig_pk
    return concat(
        new byte[] {(byte) p.getV()},
        formatAlgsCiphersuite(p.getAlgs()).getBytes(StandardCharsets.UTF_8),
        CONTEXT.getBytes(StandardCharsets.UTF_8),
        Base64Url.decode(p.getCtKem()),
        Base64Url.decode(p.getNonce()),
        Base64Url.decode(p.getAad()),
        Base64Url.decode(p.getCiphertext()),
        Base64Url.decode(p.getServerSigPk()));
  }

  private String formatAlgsCiphersuite(Algorithms algs) {
    return String.format(
        "%s:%s:%s:%s", algs.getKem(), algs.getSig(), algs.getAead(), algs.getKdf());
  }

  private byte[] concat(byte[]... arrays) {
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
