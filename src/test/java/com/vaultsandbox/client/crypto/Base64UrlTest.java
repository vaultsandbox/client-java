package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Base64UrlTest {

  @Test
  void testEncodeDecodeRoundtrip() {
    byte[] data = new byte[] {0x00, (byte) 0xFF, 0x7F, 0x01, (byte) 0x80};
    String encoded = Base64Url.encode(data);
    byte[] decoded = Base64Url.decode(encoded);
    assertArrayEquals(data, decoded);
  }

  @Test
  void testEncodeEmpty() {
    byte[] data = new byte[0];
    String encoded = Base64Url.encode(data);
    assertEquals("", encoded);
  }

  @Test
  void testDecodeEmpty() {
    byte[] decoded = Base64Url.decode("");
    assertEquals(0, decoded.length);
  }

  @Test
  void testEncodeNoPadding() {
    // Base64URL should not have padding
    byte[] data = new byte[] {0x01, 0x02};
    String encoded = Base64Url.encode(data);
    assertFalse(encoded.contains("="));
  }

  @Test
  void testDecodeValidUnpadded() {
    // Should handle valid unpadded inputs
    byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
    String encoded = Base64Url.encode(data);
    byte[] decoded = Base64Url.decode(encoded);
    assertArrayEquals(data, decoded);
  }

  @Test
  void testUrlSafeCharacters() {
    // Test that URL-unsafe characters are properly encoded
    // Standard base64 uses + and /, URL-safe uses - and _
    byte[] data = new byte[] {(byte) 0xfb, (byte) 0xff, (byte) 0xfe};
    String encoded = Base64Url.encode(data);
    assertFalse(encoded.contains("+"));
    assertFalse(encoded.contains("/"));
  }

  @Test
  void testLargeDataRoundtrip() {
    byte[] data = new byte[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    String encoded = Base64Url.encode(data);
    byte[] decoded = Base64Url.decode(encoded);
    assertArrayEquals(data, decoded);
  }

  // ==================== Spec §2.2: MUST reject +, /, = ====================

  @Test
  void testDecodeRejectsPlusCharacter() {
    // Per spec §2.2: MUST reject input containing '+'
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Base64Url.decode("AB+C"));
    assertTrue(ex.getMessage().contains("+"));
  }

  @Test
  void testDecodeRejectsSlashCharacter() {
    // Per spec §2.2: MUST reject input containing '/'
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Base64Url.decode("AB/C"));
    assertTrue(ex.getMessage().contains("/"));
  }

  @Test
  void testDecodeRejectsPaddingCharacter() {
    // Per spec §2.2: MUST reject input containing '='
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Base64Url.decode("ABCD=="));
    assertTrue(ex.getMessage().contains("="));
  }

  @Test
  void testDecodeRejectsStandardBase64() {
    // Standard base64 "+w==" should be rejected
    assertThrows(IllegalArgumentException.class, () -> Base64Url.decode("+w=="));
  }

  @Test
  void testSpecTestVectors() {
    // Test vectors from spec §12.1
    assertArrayEquals(new byte[0], Base64Url.decode(""));
    assertArrayEquals(new byte[] {0x00}, Base64Url.decode("AA"));
    assertArrayEquals(new byte[] {(byte) 0xfb}, Base64Url.decode("-w"));
    assertArrayEquals(new byte[] {(byte) 0xfb, (byte) 0xff}, Base64Url.decode("-_8"));
    assertArrayEquals(
        new byte[] {
          0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
          0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
        },
        Base64Url.decode("AAECAwQFBgcICQoLDA0ODw"));
  }
}
