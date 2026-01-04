package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.exception.SignatureVerificationException;
import com.vaultsandbox.client.model.Algorithms;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignatureVerifierTest {

  private SignatureVerifier signatureVerifier;
  private AsymmetricCipherKeyPair signingKeyPair;
  private byte[] rawPublicKey;
  private byte[] rawPrivateKey;
  private String pinnedServerKey;

  @BeforeEach
  void setUp() throws Exception {
    CryptoProvider.ensureInitialized();
    signatureVerifier = new SignatureVerifier();

    // Generate ML-DSA-65 keypair using low-level API for raw keys
    MLDSAKeyPairGenerator keyGen = new MLDSAKeyPairGenerator();
    keyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    signingKeyPair = keyGen.generateKeyPair();

    MLDSAPublicKeyParameters pubParams = (MLDSAPublicKeyParameters) signingKeyPair.getPublic();
    MLDSAPrivateKeyParameters privParams = (MLDSAPrivateKeyParameters) signingKeyPair.getPrivate();

    rawPublicKey = pubParams.getEncoded();
    rawPrivateKey = privParams.getEncoded();
    pinnedServerKey = Base64Url.encode(rawPublicKey);
  }

  @Test
  void testVerifyValidSignature() throws Exception {
    EncryptedPayload payload = createValidPayload();

    assertTrue(signatureVerifier.verify(payload, pinnedServerKey));
  }

  @Test
  void testVerifyOrThrowWithValidSignature() throws Exception {
    EncryptedPayload payload = createValidPayload();

    assertDoesNotThrow(() -> signatureVerifier.verifyOrThrow(payload, pinnedServerKey));
  }

  @Test
  void testVerifyInvalidSignature() throws Exception {
    EncryptedPayload payload = createPayloadWithInvalidSignature();

    assertFalse(signatureVerifier.verify(payload, pinnedServerKey));
  }

  @Test
  void testVerifyOrThrowWithInvalidSignature() throws Exception {
    EncryptedPayload payload = createPayloadWithInvalidSignature();

    assertThrows(
        SignatureVerificationException.class,
        () -> signatureVerifier.verifyOrThrow(payload, pinnedServerKey));
  }

  @Test
  void testVerifyWithTamperedCiphertext() throws Exception {
    EncryptedPayload payload = createValidPayload();

    // Tamper with ciphertext
    EncryptedPayload tamperedPayload = createTamperedPayload(payload, "ciphertext");

    assertFalse(signatureVerifier.verify(tamperedPayload, pinnedServerKey));
  }

  @Test
  void testVerifyWithTamperedNonce() throws Exception {
    EncryptedPayload payload = createValidPayload();

    // Tamper with nonce
    EncryptedPayload tamperedPayload = createTamperedPayload(payload, "nonce");

    assertFalse(signatureVerifier.verify(tamperedPayload, pinnedServerKey));
  }

  @Test
  void testVerifyWithServerKeyMismatch() throws Exception {
    // Create valid payload but verify with different pinned key
    EncryptedPayload payload = createValidPayload();

    // Generate a different keypair for pinned key
    MLDSAKeyPairGenerator differentKeyGen = new MLDSAKeyPairGenerator();
    differentKeyGen.init(
        new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair differentKeyPair = differentKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters differentPubParams =
        (MLDSAPublicKeyParameters) differentKeyPair.getPublic();
    String differentPinnedKey = Base64Url.encode(differentPubParams.getEncoded());

    // Should fail because pinned key doesn't match payload's server_sig_pk
    assertThrows(Exception.class, () -> signatureVerifier.verify(payload, differentPinnedKey));
  }

  @Test
  void testVerifyWithMalformedPublicKey() {
    // Create payload with malformed public key bytes
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(new byte[1088]);
    payload.nonce = Base64Url.encode(new byte[12]);
    payload.aad = Base64Url.encode("test".getBytes());
    payload.ciphertext = Base64Url.encode(new byte[32]);
    payload.sig = Base64Url.encode(new byte[3309]);
    payload.serverSigPk = Base64Url.encode(new byte[100]); // Invalid key size

    // Should throw because server_sig_pk size is invalid
    assertThrows(Exception.class, () -> signatureVerifier.verify(payload, pinnedServerKey));
  }

  @Test
  void testVerifyRejectsInvalidVersion() throws Exception {
    TestEncryptedPayload payload = (TestEncryptedPayload) createValidPayload();
    payload.v = 2; // Invalid version

    assertThrows(Exception.class, () -> signatureVerifier.verify(payload, pinnedServerKey));
  }

  @Test
  void testVerifyRejectsInvalidAlgorithm() throws Exception {
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("INVALID-KEM", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(new byte[1088]);
    payload.nonce = Base64Url.encode(new byte[12]);
    payload.aad = Base64Url.encode("test".getBytes());
    payload.ciphertext = Base64Url.encode(new byte[32]);
    payload.sig = Base64Url.encode(new byte[3309]);
    payload.serverSigPk = pinnedServerKey;

    assertThrows(Exception.class, () -> signatureVerifier.verify(payload, pinnedServerKey));
  }

  private EncryptedPayload createValidPayload() throws Exception {
    byte[] ctKem = new byte[1088];
    byte[] nonce = new byte[12];
    byte[] aad = "test-aad".getBytes(StandardCharsets.UTF_8);
    byte[] ciphertext = "encrypted-content".getBytes(StandardCharsets.UTF_8);

    // Create the payload
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(ctKem);
    payload.nonce = Base64Url.encode(nonce);
    payload.aad = Base64Url.encode(aad);
    payload.ciphertext = Base64Url.encode(ciphertext);
    payload.serverSigPk = Base64Url.encode(rawPublicKey);

    // Build transcript and sign
    byte[] transcript = buildTranscript(payload);
    byte[] signature = sign(transcript);
    payload.sig = Base64Url.encode(signature);

    return payload;
  }

  private EncryptedPayload createPayloadWithInvalidSignature() throws Exception {
    byte[] ctKem = new byte[1088];
    byte[] nonce = new byte[12];
    byte[] aad = "test-aad".getBytes(StandardCharsets.UTF_8);
    byte[] ciphertext = "encrypted-content".getBytes(StandardCharsets.UTF_8);

    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(ctKem);
    payload.nonce = Base64Url.encode(nonce);
    payload.aad = Base64Url.encode(aad);
    payload.ciphertext = Base64Url.encode(ciphertext);
    payload.serverSigPk = Base64Url.encode(rawPublicKey);

    // Create invalid signature
    byte[] invalidSig = new byte[3309]; // ML-DSA-65 signature size
    payload.sig = Base64Url.encode(invalidSig);

    return payload;
  }

  private EncryptedPayload createPayloadWithWrongKey() throws Exception {
    byte[] ctKem = new byte[1088];
    byte[] nonce = new byte[12];
    byte[] aad = "test-aad".getBytes(StandardCharsets.UTF_8);
    byte[] ciphertext = "encrypted-content".getBytes(StandardCharsets.UTF_8);

    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(ctKem);
    payload.nonce = Base64Url.encode(nonce);
    payload.aad = Base64Url.encode(aad);
    payload.ciphertext = Base64Url.encode(ciphertext);

    // Generate a different keypair using low-level API
    MLDSAKeyPairGenerator differentKeyGen = new MLDSAKeyPairGenerator();
    differentKeyGen.init(
        new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair differentKeyPair = differentKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters differentPubParams =
        (MLDSAPublicKeyParameters) differentKeyPair.getPublic();
    payload.serverSigPk = Base64Url.encode(differentPubParams.getEncoded());

    // Sign with original key
    byte[] transcript = buildTranscript(payload);
    byte[] signature = sign(transcript);
    payload.sig = Base64Url.encode(signature);

    return payload;
  }

  private EncryptedPayload createTamperedPayload(EncryptedPayload original, String field)
      throws Exception {
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = original.getV();
    payload.algs = original.getAlgs();
    payload.ctKem = original.getCtKem();
    payload.nonce = original.getNonce();
    payload.aad = original.getAad();
    payload.ciphertext = original.getCiphertext();
    payload.sig = original.getSig();
    payload.serverSigPk = original.getServerSigPk();

    // Tamper with the specified field
    switch (field) {
      case "ciphertext":
        byte[] ct = Base64Url.decode(payload.ciphertext);
        ct[0] ^= 0xFF;
        payload.ciphertext = Base64Url.encode(ct);
        break;
      case "nonce":
        byte[] n = Base64Url.decode(payload.nonce);
        n[0] ^= 0xFF;
        payload.nonce = Base64Url.encode(n);
        break;
      default:
        break;
    }

    return payload;
  }

  private byte[] buildTranscript(TestEncryptedPayload p) throws Exception {
    String context = "vaultsandbox:email:v1";
    String algsCiphersuite =
        String.format(
            "%s:%s:%s:%s", p.algs.getKem(), p.algs.getSig(), p.algs.getAead(), p.algs.getKdf());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bos.write(new byte[] {(byte) p.v});
    bos.write(algsCiphersuite.getBytes(StandardCharsets.UTF_8));
    bos.write(context.getBytes(StandardCharsets.UTF_8));
    bos.write(Base64Url.decode(p.ctKem));
    bos.write(Base64Url.decode(p.nonce));
    bos.write(Base64Url.decode(p.aad));
    bos.write(Base64Url.decode(p.ciphertext));
    bos.write(Base64Url.decode(p.serverSigPk));
    return bos.toByteArray();
  }

  private byte[] sign(byte[] data) throws Exception {
    MLDSAPrivateKeyParameters privParams = (MLDSAPrivateKeyParameters) signingKeyPair.getPrivate();
    MLDSASigner signer = new MLDSASigner();
    signer.init(true, privParams);
    signer.update(data, 0, data.length);
    return signer.generateSignature();
  }

  /** Test implementation of EncryptedPayload with writable fields. */
  private static final class TestEncryptedPayload extends EncryptedPayload {
    int v;
    Algorithms algs;
    String ctKem;
    String nonce;
    String aad;
    String ciphertext;
    String sig;
    String serverSigPk;

    @Override
    public int getV() {
      return v;
    }

    @Override
    public Algorithms getAlgs() {
      return algs;
    }

    @Override
    public String getCtKem() {
      return ctKem;
    }

    @Override
    public String getNonce() {
      return nonce;
    }

    @Override
    public String getAad() {
      return aad;
    }

    @Override
    public String getCiphertext() {
      return ciphertext;
    }

    @Override
    public String getSig() {
      return sig;
    }

    @Override
    public String getServerSigPk() {
      return serverSigPk;
    }
  }
}
