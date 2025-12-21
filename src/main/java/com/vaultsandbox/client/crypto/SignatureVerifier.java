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

  public SignatureVerifier() {
    CryptoProvider.ensureInitialized();
  }

  public boolean verify(EncryptedPayload payload) {
    try {
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
    } catch (Exception e) {
      throw new SignatureVerificationException("Signature verification failed", e);
    }
  }

  public void verifyOrThrow(EncryptedPayload payload) {
    if (!verify(payload)) {
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
