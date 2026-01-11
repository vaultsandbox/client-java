package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.exception.DecryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeypairGeneratorTest {

  private KeypairGenerator keypairGenerator;

  @BeforeEach
  void setUp() {
    keypairGenerator = new KeypairGenerator();
  }

  @Test
  void testKeypairGeneration() {
    Keypair kp = keypairGenerator.generate();

    assertNotNull(kp);
    assertNotNull(kp.getPublicKey());
    assertNotNull(kp.getSecretKey());

    // ML-KEM-768 key sizes (encoded format may vary slightly from raw)
    assertTrue(kp.getPublicKey().length > 0, "Public key should not be empty");
    assertTrue(kp.getSecretKey().length > 0, "Secret key should not be empty");
  }

  @Test
  void testKeypairUniqueness() {
    Keypair kp1 = keypairGenerator.generate();
    Keypair kp2 = keypairGenerator.generate();

    assertFalse(
        java.util.Arrays.equals(kp1.getPublicKey(), kp2.getPublicKey()),
        "Generated keypairs should be unique");
    assertFalse(
        java.util.Arrays.equals(kp1.getSecretKey(), kp2.getSecretKey()),
        "Generated keypairs should be unique");
  }

  @Test
  void testBase64UrlEncoding() {
    Keypair kp = keypairGenerator.generate();

    String publicKeyBase64 = kp.getPublicKeyBase64Url();
    String secretKeyBase64 = kp.getSecretKeyBase64Url();

    assertNotNull(publicKeyBase64);
    assertNotNull(secretKeyBase64);
    assertFalse(publicKeyBase64.isEmpty());
    assertFalse(secretKeyBase64.isEmpty());

    // Verify roundtrip
    assertArrayEquals(kp.getPublicKey(), Base64Url.decode(publicKeyBase64));
    assertArrayEquals(kp.getSecretKey(), Base64Url.decode(secretKeyBase64));
  }

  @Test
  void testKeyImmutability() {
    Keypair kp = keypairGenerator.generate();

    byte[] publicKey1 = kp.getPublicKey();
    byte[] publicKey2 = kp.getPublicKey();

    // Modify the first copy
    publicKey1[0] = (byte) ~publicKey1[0];

    // Second copy should be unchanged
    assertNotEquals(publicKey1[0], publicKey2[0], "Keypair should return defensive copies");
  }

  @Test
  void testCorrectKeySizes() {
    Keypair kp = keypairGenerator.generate();

    // ML-KEM-768 standard key sizes
    assertEquals(1184, kp.getPublicKey().length, "Public key should be 1184 bytes for ML-KEM-768");
    assertEquals(2400, kp.getSecretKey().length, "Secret key should be 2400 bytes for ML-KEM-768");
  }

  @Test
  void testDerivePublicKeyFromSecretKey() {
    Keypair kp = keypairGenerator.generate();

    byte[] derivedPublicKey = keypairGenerator.derivePublicKey(kp.getSecretKey());

    assertNotNull(derivedPublicKey);
    assertEquals(KeypairGenerator.PUBLIC_KEY_SIZE, derivedPublicKey.length);
    assertArrayEquals(
        kp.getPublicKey(), derivedPublicKey, "Derived public key should match original");
  }

  @Test
  void testDerivePublicKeyMultipleKeypairs() {
    // Verify derivation works correctly for multiple different keypairs
    for (int i = 0; i < 3; i++) {
      Keypair kp = keypairGenerator.generate();
      byte[] derivedPublicKey = keypairGenerator.derivePublicKey(kp.getSecretKey());
      assertArrayEquals(
          kp.getPublicKey(),
          derivedPublicKey,
          "Derived public key should match original for keypair " + i);
    }
  }

  @Test
  void testDerivePublicKeyWithInvalidSizeThrows() {
    byte[] invalidSecretKey = new byte[100]; // Wrong size

    DecryptionException ex =
        assertThrows(
            DecryptionException.class, () -> keypairGenerator.derivePublicKey(invalidSecretKey));
    assertTrue(ex.getMessage().contains("Invalid secret key size"));
  }

  @Test
  void testDerivePublicKeyWithEmptyArrayThrows() {
    byte[] emptyKey = new byte[0];

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> keypairGenerator.derivePublicKey(emptyKey));
    assertTrue(ex.getMessage().contains("Invalid secret key size"));
  }

  @Test
  void testDerivePublicKeyWithSlightlyWrongSizeThrows() {
    // Off by one from expected size
    byte[] wrongSize = new byte[KeypairGenerator.SECRET_KEY_SIZE - 1];

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> keypairGenerator.derivePublicKey(wrongSize));
    assertTrue(ex.getMessage().contains("Invalid secret key size"));
    assertTrue(ex.getMessage().contains("expected " + KeypairGenerator.SECRET_KEY_SIZE));
  }
}
