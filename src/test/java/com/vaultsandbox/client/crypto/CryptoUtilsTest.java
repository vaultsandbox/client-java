package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.exception.DecryptionException;
import com.vaultsandbox.client.exception.SignatureVerificationException;
import com.vaultsandbox.client.model.Algorithms;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.junit.jupiter.api.Test;

class CryptoUtilsTest {

  @Test
  void testGenerateKeypair() {
    Keypair keypair = CryptoUtils.generateKeypair();

    assertNotNull(keypair);
    assertNotNull(keypair.getPublicKey());
    assertNotNull(keypair.getSecretKey());
    assertTrue(keypair.getPublicKey().length > 0);
    assertTrue(keypair.getSecretKey().length > 0);
  }

  @Test
  void testGenerateKeypairUniqueness() {
    Keypair kp1 = CryptoUtils.generateKeypair();
    Keypair kp2 = CryptoUtils.generateKeypair();

    assertFalse(
        java.util.Arrays.equals(kp1.getPublicKey(), kp2.getPublicKey()),
        "Generated keypairs should be unique");
    assertFalse(
        java.util.Arrays.equals(kp1.getSecretKey(), kp2.getSecretKey()),
        "Generated keypairs should be unique");
  }

  @Test
  void testBase64UrlEncode() {
    byte[] data = {0x00, 0x01, 0x02, 0x03, (byte) 0xff};
    String encoded = CryptoUtils.toBase64Url(data);

    assertNotNull(encoded);
    assertFalse(encoded.isEmpty());
    // Base64url should not contain padding
    assertFalse(encoded.contains("="));
    // Base64url should not contain + or /
    assertFalse(encoded.contains("+"));
    assertFalse(encoded.contains("/"));
  }

  @Test
  void testBase64UrlDecode() {
    String base64url = "AAECAw";
    byte[] decoded = CryptoUtils.fromBase64Url(base64url);

    assertNotNull(decoded);
    assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03}, decoded);
  }

  @Test
  void testBase64UrlRoundtrip() {
    byte[] original = {0x00, 0x10, 0x20, 0x30, 0x40, 0x50, (byte) 0xfe, (byte) 0xff};
    String encoded = CryptoUtils.toBase64Url(original);
    byte[] decoded = CryptoUtils.fromBase64Url(encoded);

    assertArrayEquals(original, decoded);
  }

  @Test
  void testBase64UrlWithKeypair() {
    Keypair keypair = CryptoUtils.generateKeypair();

    // Encode and decode public key
    String encodedPk = CryptoUtils.toBase64Url(keypair.getPublicKey());
    byte[] decodedPk = CryptoUtils.fromBase64Url(encodedPk);
    assertArrayEquals(keypair.getPublicKey(), decodedPk);

    // Encode and decode secret key
    String encodedSk = CryptoUtils.toBase64Url(keypair.getSecretKey());
    byte[] decodedSk = CryptoUtils.fromBase64Url(encodedSk);
    assertArrayEquals(keypair.getSecretKey(), decodedSk);
  }

  @Test
  void testVerifySignatureSafeReturnsFalseOnInvalidPayload() {
    // verifySignatureSafe should return false instead of throwing
    boolean result = CryptoUtils.verifySignatureSafe(null, null);
    assertFalse(result);
  }

  @Test
  void testVerifySignatureSafeReturnsFalseOnNullPinnedKey() {
    // Even with null pinned key, should return false not throw
    boolean result = CryptoUtils.verifySignatureSafe(null, "someKey");
    assertFalse(result);
  }

  @Test
  void testVerifySignatureSafeReturnsTrueOnValidPayload() throws Exception {
    CryptoProvider.ensureInitialized();

    Keypair keypair = CryptoUtils.generateKeypair();

    MLDSAKeyPairGenerator sigKeyGen = new MLDSAKeyPairGenerator();
    sigKeyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair sigKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters sigPubParams = (MLDSAPublicKeyParameters) sigKeyPair.getPublic();
    MLDSAPrivateKeyParameters sigPrivParams = (MLDSAPrivateKeyParameters) sigKeyPair.getPrivate();

    byte[] plaintext = "Signed message for safe verify".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload =
        createEncryptedPayload(keypair, plaintext, sigPubParams, sigPrivParams);

    String pinnedServerKey = Base64Url.encode(sigPubParams.getEncoded());

    // verifySignatureSafe should return true for valid payload
    boolean result = CryptoUtils.verifySignatureSafe(payload, pinnedServerKey);
    assertTrue(result);
  }

  @Test
  void testComputeEmailsHashSingleId() {
    String hash = CryptoUtils.computeEmailsHash(List.of("email-123"));

    assertNotNull(hash);
    assertFalse(hash.isEmpty());
    // Base64url should not contain padding
    assertFalse(hash.contains("="));
    // SHA-256 produces 32 bytes, base64url encoded is ~43 chars
    assertTrue(hash.length() >= 40 && hash.length() <= 50);
  }

  @Test
  void testComputeEmailsHashMultipleIds() {
    String hash = CryptoUtils.computeEmailsHash(Arrays.asList("id1", "id2", "id3"));

    assertNotNull(hash);
    assertFalse(hash.isEmpty());
  }

  @Test
  void testComputeEmailsHashSortsIds() {
    String hash1 = CryptoUtils.computeEmailsHash(Arrays.asList("z", "a", "m"));
    String hash2 = CryptoUtils.computeEmailsHash(Arrays.asList("a", "m", "z"));

    assertEquals(hash1, hash2, "Hash should be the same regardless of input order");
  }

  @Test
  void testComputeEmailsHashDifferentInputsProduceDifferentHashes() {
    String hash1 = CryptoUtils.computeEmailsHash(List.of("email-1"));
    String hash2 = CryptoUtils.computeEmailsHash(List.of("email-2"));

    assertNotEquals(hash1, hash2);
  }

  @Test
  void testComputeEmailsHashEmptyList() {
    String hash = CryptoUtils.computeEmailsHash(List.of());

    assertNotNull(hash);
    // SHA-256 of empty string in base64url
    assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", hash);
  }

  @Test
  void testBase64UrlEmptyArray() {
    byte[] empty = new byte[0];
    String encoded = CryptoUtils.toBase64Url(empty);
    byte[] decoded = CryptoUtils.fromBase64Url(encoded);

    assertEquals("", encoded);
    assertArrayEquals(empty, decoded);
  }

  @Test
  void testBase64UrlLargeData() {
    byte[] largeData = new byte[10000];
    for (int i = 0; i < largeData.length; i++) {
      largeData[i] = (byte) (i % 256);
    }

    String encoded = CryptoUtils.toBase64Url(largeData);
    byte[] decoded = CryptoUtils.fromBase64Url(encoded);

    assertArrayEquals(largeData, decoded);
    assertFalse(encoded.contains("+"));
    assertFalse(encoded.contains("/"));
    assertFalse(encoded.contains("="));
  }

  @Test
  void testGenerateKeypairCorrectSizes() {
    Keypair keypair = CryptoUtils.generateKeypair();

    assertEquals(1184, keypair.getPublicKey().length, "ML-KEM-768 public key should be 1184 bytes");
    assertEquals(2400, keypair.getSecretKey().length, "ML-KEM-768 secret key should be 2400 bytes");
  }

  @Test
  void testDecryptValidPayload() throws Exception {
    CryptoProvider.ensureInitialized();

    // Generate ML-KEM keypair for encryption
    Keypair keypair = CryptoUtils.generateKeypair();

    // Generate ML-DSA keypair for signing
    MLDSAKeyPairGenerator sigKeyGen = new MLDSAKeyPairGenerator();
    sigKeyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair sigKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters sigPubParams = (MLDSAPublicKeyParameters) sigKeyPair.getPublic();
    MLDSAPrivateKeyParameters sigPrivParams = (MLDSAPrivateKeyParameters) sigKeyPair.getPrivate();

    // Create encrypted payload
    byte[] plaintext = "Hello, CryptoUtils!".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload =
        createEncryptedPayload(keypair, plaintext, sigPubParams, sigPrivParams);

    // Decrypt using CryptoUtils
    byte[] decrypted = CryptoUtils.decrypt(payload, keypair.getSecretKey());

    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void testDecryptWithInvalidSecretKey() throws Exception {
    CryptoProvider.ensureInitialized();

    Keypair keypair = CryptoUtils.generateKeypair();
    Keypair wrongKeypair = CryptoUtils.generateKeypair();

    MLDSAKeyPairGenerator sigKeyGen = new MLDSAKeyPairGenerator();
    sigKeyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair sigKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters sigPubParams = (MLDSAPublicKeyParameters) sigKeyPair.getPublic();
    MLDSAPrivateKeyParameters sigPrivParams = (MLDSAPrivateKeyParameters) sigKeyPair.getPrivate();

    byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload =
        createEncryptedPayload(keypair, plaintext, sigPubParams, sigPrivParams);

    // Decrypting with wrong key should fail
    assertThrows(
        DecryptionException.class, () -> CryptoUtils.decrypt(payload, wrongKeypair.getSecretKey()));
  }

  @Test
  void testVerifySignatureValid() throws Exception {
    CryptoProvider.ensureInitialized();

    Keypair keypair = CryptoUtils.generateKeypair();

    MLDSAKeyPairGenerator sigKeyGen = new MLDSAKeyPairGenerator();
    sigKeyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair sigKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters sigPubParams = (MLDSAPublicKeyParameters) sigKeyPair.getPublic();
    MLDSAPrivateKeyParameters sigPrivParams = (MLDSAPrivateKeyParameters) sigKeyPair.getPrivate();

    byte[] plaintext = "Signed message".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload =
        createEncryptedPayload(keypair, plaintext, sigPubParams, sigPrivParams);

    String pinnedServerKey = Base64Url.encode(sigPubParams.getEncoded());

    // Verify using CryptoUtils
    boolean result = CryptoUtils.verifySignature(payload, pinnedServerKey);
    assertTrue(result);
  }

  @Test
  void testVerifySignatureThrowsOnMismatchedKey() throws Exception {
    CryptoProvider.ensureInitialized();

    Keypair keypair = CryptoUtils.generateKeypair();

    MLDSAKeyPairGenerator sigKeyGen = new MLDSAKeyPairGenerator();
    sigKeyGen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
    AsymmetricCipherKeyPair sigKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters sigPubParams = (MLDSAPublicKeyParameters) sigKeyPair.getPublic();
    MLDSAPrivateKeyParameters sigPrivParams = (MLDSAPrivateKeyParameters) sigKeyPair.getPrivate();

    // Generate a different keypair for pinned key
    AsymmetricCipherKeyPair differentKeyPair = sigKeyGen.generateKeyPair();
    MLDSAPublicKeyParameters differentPubParams =
        (MLDSAPublicKeyParameters) differentKeyPair.getPublic();
    String differentPinnedKey = Base64Url.encode(differentPubParams.getEncoded());

    byte[] plaintext = "Signed message".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload =
        createEncryptedPayload(keypair, plaintext, sigPubParams, sigPrivParams);

    // Should throw because pinned key doesn't match
    assertThrows(
        SignatureVerificationException.class,
        () -> CryptoUtils.verifySignature(payload, differentPinnedKey));
  }

  // Helper method to create a valid encrypted payload
  private EncryptedPayload createEncryptedPayload(
      Keypair kemKeypair,
      byte[] plaintext,
      MLDSAPublicKeyParameters sigPubParams,
      MLDSAPrivateKeyParameters sigPrivParams)
      throws Exception {

    SecureRandom random = new SecureRandom();

    // Step 1: Encapsulate using ML-KEM
    MLKEMPublicKeyParameters kemPubParams =
        new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, kemKeypair.getPublicKey());
    MLKEMGenerator generator = new MLKEMGenerator(random);
    SecretWithEncapsulation encapsulated = generator.generateEncapsulated(kemPubParams);

    byte[] ctKem = encapsulated.getEncapsulation();
    byte[] sharedSecret = encapsulated.getSecret();

    // Step 2: Derive AES key using HKDF
    byte[] aad = "test-aad".getBytes(StandardCharsets.UTF_8);
    HkdfDeriver hkdfDeriver = new HkdfDeriver();
    byte[] aesKey = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);

    // Step 3: Encrypt using AES-GCM
    byte[] nonce = new byte[12];
    random.nextBytes(nonce);

    GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
    cipher.updateAAD(aad);
    byte[] ciphertext = cipher.doFinal(plaintext);

    // Step 4: Build payload and sign
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(ctKem);
    payload.nonce = Base64Url.encode(nonce);
    payload.aad = Base64Url.encode(aad);
    payload.ciphertext = Base64Url.encode(ciphertext);
    payload.serverSigPk = Base64Url.encode(sigPubParams.getEncoded());

    // Build transcript and sign
    byte[] transcript = buildTranscript(payload);
    MLDSASigner signer = new MLDSASigner();
    signer.init(true, sigPrivParams);
    signer.update(transcript, 0, transcript.length);
    byte[] signature = signer.generateSignature();
    payload.sig = Base64Url.encode(signature);

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
