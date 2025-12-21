package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vaultsandbox.client.exception.DecryptionException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
}
