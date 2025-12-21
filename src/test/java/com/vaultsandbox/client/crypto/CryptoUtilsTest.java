package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    boolean result = CryptoUtils.verifySignatureSafe(null);
    assertFalse(result);
  }
}
