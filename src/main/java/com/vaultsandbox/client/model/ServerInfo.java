package com.vaultsandbox.client.model;

import java.util.List;

public class ServerInfo {
  private String serverSigPk;
  private Algorithms algs;
  private String context;
  private int maxTtl;
  private int defaultTtl;
  private boolean sseConsole;
  private List<String> allowedDomains;
  private String encryptionPolicy;
  private boolean spamAnalysisEnabled;
  private boolean chaosEnabled;

  // Legacy fields for backward compatibility
  private String version;
  private String domain;
  private Algorithms algorithms;
  private Limits limits;

  public ServerInfo() {}

  public String getServerSigPk() {
    return serverSigPk;
  }

  public String getContext() {
    return context;
  }

  public int getMaxTtl() {
    return maxTtl;
  }

  public int getDefaultTtl() {
    return defaultTtl;
  }

  public boolean isSseConsole() {
    return sseConsole;
  }

  public List<String> getAllowedDomains() {
    return allowedDomains;
  }

  public String getVersion() {
    return version;
  }

  public String getDomain() {
    return domain;
  }

  public Algorithms getAlgorithms() {
    // Return algs first (new API format), fall back to algorithms (old format)
    return algs != null ? algs : algorithms;
  }

  public Limits getLimits() {
    return limits;
  }

  /**
   * Returns the server's encryption policy.
   *
   * <p>Policy values:
   *
   * <ul>
   *   <li>{@code "always"} - All inboxes are encrypted, no override allowed
   *   <li>{@code "enabled"} - Default encrypted, can request plain
   *   <li>{@code "disabled"} - Default plain, can request encrypted
   *   <li>{@code "never"} - All inboxes are plain, no override allowed
   * </ul>
   *
   * @return the encryption policy, or {@code null} if not specified (defaults to "always" for
   *     backward compatibility)
   */
  public String getEncryptionPolicy() {
    return encryptionPolicy;
  }

  /**
   * Returns whether spam analysis (Rspamd) is enabled on this server.
   *
   * <p>When enabled, incoming emails can be analyzed for spam. Individual inboxes may still opt out
   * of spam analysis when creating the inbox.
   *
   * @return {@code true} if spam analysis is available on the server
   */
  public boolean isSpamAnalysisEnabled() {
    return spamAnalysisEnabled;
  }

  /**
   * Returns whether chaos engineering is enabled on this server.
   *
   * <p>When enabled, inboxes can be configured with chaos settings for testing system resilience.
   * If {@code false}, all chaos endpoints will return 403 Forbidden.
   *
   * @return {@code true} if chaos is available on the server
   */
  public boolean isChaosEnabled() {
    return chaosEnabled;
  }

  /**
   * Returns whether encryption can be overridden per-inbox.
   *
   * @return {@code true} if policy is "enabled" or "disabled"
   */
  public boolean canOverrideEncryption() {
    return "enabled".equals(encryptionPolicy) || "disabled".equals(encryptionPolicy);
  }

  /**
   * Returns whether encryption is the default for new inboxes.
   *
   * @return {@code true} if policy is "always", "enabled", or not specified
   */
  public boolean isDefaultEncrypted() {
    // Backward compatibility: null/missing means encryption enabled
    return encryptionPolicy == null
        || "always".equals(encryptionPolicy)
        || "enabled".equals(encryptionPolicy);
  }

  public static class Limits {
    private int maxInboxes;
    private int maxEmailsPerInbox;
    private int inboxTtlSeconds;

    public Limits() {}

    public int getMaxInboxes() {
      return maxInboxes;
    }

    public int getMaxEmailsPerInbox() {
      return maxEmailsPerInbox;
    }

    public int getInboxTtlSeconds() {
      return inboxTtlSeconds;
    }
  }
}
