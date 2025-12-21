package com.vaultsandbox.client.crypto;

import java.util.Base64;

public final class Base64Url {
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private Base64Url() {}

  public static String encode(byte[] data) {
    return ENCODER.encodeToString(data);
  }

  public static byte[] decode(String s) {
    // Add padding if needed for decoder compatibility
    int padding = (4 - s.length() % 4) % 4;
    String padded = s + "=".repeat(padding);
    return DECODER.decode(padded);
  }
}
