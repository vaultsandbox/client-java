package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HkdfDeriverTest {

  private HkdfDeriver hkdfDeriver;

  @BeforeEach
  void setUp() {
    hkdfDeriver = new HkdfDeriver();
  }

  @Test
  void testDeriveKeyLength() {
    byte[] sharedSecret = new byte[32];
    byte[] aad = "test-aad".getBytes();
    byte[] ctKem = new byte[64];

    byte[] derivedKey = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);

    assertEquals(32, derivedKey.length, "Derived key should be 32 bytes for AES-256");
  }

  @Test
  void testDeterministicDerivation() {
    byte[] sharedSecret = new byte[32];
    byte[] aad = "test-aad".getBytes();
    byte[] ctKem = new byte[64];

    // Fill with non-zero values
    for (int i = 0; i < sharedSecret.length; i++) {
      sharedSecret[i] = (byte) i;
    }
    for (int i = 0; i < ctKem.length; i++) {
      ctKem[i] = (byte) (i + 100);
    }

    byte[] derivedKey1 = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);
    byte[] derivedKey2 = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);

    assertArrayEquals(derivedKey1, derivedKey2, "Same inputs should produce same derived key");
  }

  @Test
  void testDifferentInputsProduceDifferentKeys() {
    byte[] sharedSecret1 = new byte[32];
    byte[] sharedSecret2 = new byte[32];
    sharedSecret2[0] = 1; // Slightly different

    byte[] aad = "test-aad".getBytes();
    byte[] ctKem = new byte[64];

    byte[] derivedKey1 = hkdfDeriver.deriveKey(sharedSecret1, aad, ctKem);
    byte[] derivedKey2 = hkdfDeriver.deriveKey(sharedSecret2, aad, ctKem);

    assertFalse(
        java.util.Arrays.equals(derivedKey1, derivedKey2),
        "Different shared secrets should produce different keys");
  }

  @Test
  void testDifferentAadProducesDifferentKeys() {
    byte[] sharedSecret = new byte[32];
    byte[] aad1 = "aad-1".getBytes();
    byte[] aad2 = "aad-2".getBytes();
    byte[] ctKem = new byte[64];

    byte[] derivedKey1 = hkdfDeriver.deriveKey(sharedSecret, aad1, ctKem);
    byte[] derivedKey2 = hkdfDeriver.deriveKey(sharedSecret, aad2, ctKem);

    assertFalse(
        java.util.Arrays.equals(derivedKey1, derivedKey2),
        "Different AAD should produce different keys");
  }

  @Test
  void testDifferentCtKemProducesDifferentKeys() {
    byte[] sharedSecret = new byte[32];
    byte[] aad = "test-aad".getBytes();
    byte[] ctKem1 = new byte[64];
    byte[] ctKem2 = new byte[64];
    ctKem2[0] = 1; // Slightly different

    byte[] derivedKey1 = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem1);
    byte[] derivedKey2 = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem2);

    assertFalse(
        java.util.Arrays.equals(derivedKey1, derivedKey2),
        "Different ctKem should produce different keys (affects salt)");
  }

  @Test
  void testEmptyAad() {
    byte[] sharedSecret = new byte[32];
    byte[] aad = new byte[0];
    byte[] ctKem = new byte[64];

    byte[] derivedKey = hkdfDeriver.deriveKey(sharedSecret, aad, ctKem);

    assertNotNull(derivedKey);
    assertEquals(32, derivedKey.length);
  }
}
