package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
  void testDecodeWithPadding() {
    // Should handle inputs that might need padding
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
}
