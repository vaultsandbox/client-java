package com.vaultsandbox.client.crypto;

import java.util.Base64;

public final class Base64Url {
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private Base64Url() {}

  public static String encode(byte[] data) {
    return ENCODER.encodeToString(data);
  }

  /**
   * Decodes a Base64URL string without padding.
   *
   * <p>Per VaultSandbox spec §2.2, implementations MUST reject input containing '+', '/', or '='.
   *
   * @param s the Base64URL encoded string
   * @return the decoded bytes
   * @throws IllegalArgumentException if the input contains invalid characters
   */
  public static byte[] decode(String s) {
    // Per spec §2.2: Implementations MUST reject input containing +, /, or =
    if (s.indexOf('+') >= 0) {
      throw new IllegalArgumentException("Invalid Base64URL: contains '+' character");
    }
    if (s.indexOf('/') >= 0) {
      throw new IllegalArgumentException("Invalid Base64URL: contains '/' character");
    }
    if (s.indexOf('=') >= 0) {
      throw new IllegalArgumentException("Invalid Base64URL: contains '=' padding character");
    }

    // The Java Base64 URL decoder handles unpadded input correctly
    return DECODER.decode(s);
  }
}
