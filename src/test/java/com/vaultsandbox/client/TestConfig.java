package com.vaultsandbox.client;

import io.github.cdimascio.dotenv.Dotenv;

/** Test configuration loaded from .env file. */
public final class TestConfig {
  private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

  private TestConfig() {}

  public static String getBaseUrl() {
    return DOTENV.get("VAULTSANDBOX_URL", "http://localhost:3000");
  }

  public static String getApiKey() {
    return DOTENV.get("VAULTSANDBOX_API_KEY");
  }

  public static String getSmtpHost() {
    return DOTENV.get("SMTP_HOST", "localhost");
  }

  public static int getSmtpPort() {
    return Integer.parseInt(DOTENV.get("SMTP_PORT", "25"));
  }

  public static boolean hasApiKey() {
    String key = getApiKey();
    return key != null && !key.isEmpty() && !key.equals("your-api-key-here");
  }
}
