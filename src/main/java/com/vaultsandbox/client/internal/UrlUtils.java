package com.vaultsandbox.client.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * URL encoding utilities.
 *
 * <p>Provides common URL encoding operations used throughout the client.
 */
public final class UrlUtils {

  private UrlUtils() {} // Prevent instantiation

  /**
   * URL-encodes a string using UTF-8 encoding.
   *
   * @param value the string to encode
   * @return the URL-encoded string
   */
  public static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is always available; this should never happen
      return value;
    }
  }
}
