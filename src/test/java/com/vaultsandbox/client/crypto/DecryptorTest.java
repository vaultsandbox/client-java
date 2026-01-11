package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.exception.DecryptionException;
import com.vaultsandbox.client.model.EncryptedPayload;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecryptorTest {

  private Decryptor decryptor;

  @BeforeEach
  void setUp() {
    decryptor = new Decryptor();
  }

  @Test
  void testAesGcmDecryption() throws Exception {
    // Generate a random AES key
    byte[] aesKey = new byte[32];
    new SecureRandom().nextBytes(aesKey);

    // Generate a random nonce
    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);

    // AAD
    byte[] aad = "additional-data".getBytes();

    // Plaintext
    byte[] plaintext = "Hello, World!".getBytes();

    // Encrypt
    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);

    // Decrypt
    byte[] decrypted = decryptor.decryptAesGcm(aesKey, nonce, aad, ciphertext);

    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void testAesGcmDecryptionWithWrongKey() throws Exception {
    byte[] aesKey = new byte[32];
    new SecureRandom().nextBytes(aesKey);

    byte[] wrongKey = new byte[32];
    new SecureRandom().nextBytes(wrongKey);

    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);

    byte[] aad = "additional-data".getBytes();
    byte[] plaintext = "Hello, World!".getBytes();

    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);

    assertThrows(
        DecryptionException.class, () -> decryptor.decryptAesGcm(wrongKey, nonce, aad, ciphertext));
  }

  @Test
  void testAesGcmDecryptionWithWrongAad() throws Exception {
    byte[] aesKey = new byte[32];
    new SecureRandom().nextBytes(aesKey);

    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);

    byte[] aad = "additional-data".getBytes();
    byte[] wrongAad = "wrong-data".getBytes();
    byte[] plaintext = "Hello, World!".getBytes();

    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);

    assertThrows(
        DecryptionException.class,
        () -> decryptor.decryptAesGcm(aesKey, nonce, wrongAad, ciphertext));
  }

  @Test
  void testAesGcmDecryptionWithModifiedCiphertext() throws Exception {
    byte[] aesKey = new byte[32];
    new SecureRandom().nextBytes(aesKey);

    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);

    byte[] aad = "additional-data".getBytes();
    byte[] plaintext = "Hello, World!".getBytes();

    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);

    // Modify ciphertext
    ciphertext[0] ^= 0x01;

    assertThrows(
        DecryptionException.class, () -> decryptor.decryptAesGcm(aesKey, nonce, aad, ciphertext));
  }

  @Test
  void testAesGcmEmptyPlaintext() throws Exception {
    byte[] aesKey = new byte[32];
    new SecureRandom().nextBytes(aesKey);

    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);

    byte[] aad = "additional-data".getBytes();
    byte[] plaintext = new byte[0];

    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);
    byte[] decrypted = decryptor.decryptAesGcm(aesKey, nonce, aad, ciphertext);

    assertArrayEquals(plaintext, decrypted);
  }

  private byte[] encryptAesGcm(byte[] aesKey, byte[] nonce, byte[] aad, byte[] plaintext)
      throws Exception {
    GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
    SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
    cipher.updateAAD(aad);

    return cipher.doFinal(plaintext);
  }

  @Test
  void testDecryptEncryptedPayload() throws Exception {
    // Generate keypair
    KeypairGenerator keypairGenerator = new KeypairGenerator();
    Keypair keypair = keypairGenerator.generate();

    // Create a valid encrypted payload
    byte[] plaintext = "Hello, Decryptor!".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload = createEncryptedPayload(keypair, plaintext);

    // Decrypt
    byte[] decrypted = decryptor.decrypt(payload, keypair.getSecretKey());

    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void testDecryptEncryptedPayloadWithWrongKey() throws Exception {
    KeypairGenerator keypairGenerator = new KeypairGenerator();
    Keypair keypair = keypairGenerator.generate();
    Keypair wrongKeypair = keypairGenerator.generate();

    byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
    EncryptedPayload payload = createEncryptedPayload(keypair, plaintext);

    // Decrypting with wrong key should fail
    assertThrows(
        DecryptionException.class, () -> decryptor.decrypt(payload, wrongKeypair.getSecretKey()));
  }

  @Test
  void testDecryptEncryptedPayloadWithLargeData() throws Exception {
    KeypairGenerator keypairGenerator = new KeypairGenerator();
    Keypair keypair = keypairGenerator.generate();

    // Create large plaintext
    byte[] plaintext = new byte[10000];
    new SecureRandom().nextBytes(plaintext);
    EncryptedPayload payload = createEncryptedPayload(keypair, plaintext);

    byte[] decrypted = decryptor.decrypt(payload, keypair.getSecretKey());

    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void testDecapsulateWithInvalidCtKem() {
    KeypairGenerator keypairGenerator = new KeypairGenerator();
    Keypair keypair = keypairGenerator.generate();

    // Invalid ctKem (wrong size)
    byte[] invalidCtKem = new byte[100];

    DecryptionException ex =
        assertThrows(
            DecryptionException.class,
            () -> decryptor.decapsulate(invalidCtKem, keypair.getSecretKey()));
    assertTrue(ex.getMessage().contains("ML-KEM decapsulation failed"));
  }

  // Helper to create encrypted payload without signature (just for decryption tests)
  private EncryptedPayload createEncryptedPayload(Keypair keypair, byte[] plaintext)
      throws Exception {
    SecureRandom random = new SecureRandom();

    // Step 1: Encapsulate using ML-KEM
    MLKEMPublicKeyParameters kemPubParams =
        new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, keypair.getPublicKey());
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

    byte[] ciphertext = encryptAesGcm(aesKey, nonce, aad, plaintext);

    // Create payload
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.ctKem = Base64Url.encode(ctKem);
    payload.nonce = Base64Url.encode(nonce);
    payload.aad = Base64Url.encode(aad);
    payload.ciphertext = Base64Url.encode(ciphertext);

    return payload;
  }

  /** Test implementation of EncryptedPayload with writable fields. */
  private static final class TestEncryptedPayload extends EncryptedPayload {
    String ctKem;
    String nonce;
    String aad;
    String ciphertext;

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
  }
}
