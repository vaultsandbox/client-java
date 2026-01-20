package com.vaultsandbox.client;

import java.time.Duration;

/**
 * Options for creating a new inbox.
 *
 * <p>Use {@link #defaults()} for default options or {@link #builder()} to customize inbox creation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CreateInboxOptions options = CreateInboxOptions.builder()
 *     .emailAddress("test-user@yourdomain.com")
 *     .ttl(Duration.ofHours(1))
 *     .build();
 *
 * Inbox inbox = client.createInbox(options);
 * }</pre>
 *
 * @see VaultSandboxClient#createInbox(CreateInboxOptions)
 */
public final class CreateInboxOptions {
  private final String emailAddress;
  private final Duration ttl;
  private final Boolean emailAuth;
  private final String encryption;
  private final Boolean spamAnalysis;

  private CreateInboxOptions(
      String emailAddress,
      Duration ttl,
      Boolean emailAuth,
      String encryption,
      Boolean spamAnalysis) {
    this.emailAddress = emailAddress;
    this.ttl = ttl;
    this.emailAuth = emailAuth;
    this.encryption = encryption;
    this.spamAnalysis = spamAnalysis;
  }

  /**
   * Returns default options for inbox creation.
   *
   * @return default options
   */
  public static CreateInboxOptions defaults() {
    return new CreateInboxOptions(null, null, null, null, null);
  }

  /**
   * Creates options for a plain (unencrypted) inbox.
   *
   * @return options configured for plain inbox
   */
  public static CreateInboxOptions plain() {
    return builder().plain().build();
  }

  /**
   * Creates options for an encrypted inbox.
   *
   * @return options configured for encrypted inbox
   */
  public static CreateInboxOptions encrypted() {
    return builder().encrypted().build();
  }

  /**
   * Creates a new options builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the email address, if set.
   *
   * <p>This can be a full email address or just a domain part to request a specific format.
   *
   * @return the email address, or {@code null} if not set
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Returns the inbox TTL (time-to-live), if set.
   *
   * @return the TTL duration, or {@code null} if using server default
   */
  public Duration getTtl() {
    return ttl;
  }

  /**
   * Returns the email authentication setting, if set.
   *
   * <p>When {@code true}, SPF/DKIM/DMARC/PTR checks are enabled for this inbox. When {@code false},
   * all authentication checks are skipped. When {@code null}, the server default is used.
   *
   * @return the email auth setting, or {@code null} if using server default
   */
  public Boolean getEmailAuth() {
    return emailAuth;
  }

  /**
   * Returns the encryption setting, if set.
   *
   * <p>Values:
   *
   * <ul>
   *   <li>{@code "encrypted"} - Request an encrypted inbox
   *   <li>{@code "plain"} - Request a plain (unencrypted) inbox
   *   <li>{@code null} - Use server default based on encryption policy
   * </ul>
   *
   * <p>The server may reject the request if the encryption policy does not allow overrides.
   *
   * @return the encryption setting, or {@code null} if using server default
   */
  public String getEncryption() {
    return encryption;
  }

  /**
   * Returns the spam analysis setting, if set.
   *
   * <p>When {@code true}, spam analysis is enabled for emails to this inbox. When {@code false},
   * spam analysis is disabled. When {@code null}, the server default is used.
   *
   * @return the spam analysis setting, or {@code null} if using server default
   */
  public Boolean getSpamAnalysis() {
    return spamAnalysis;
  }

  /** Builder for creating {@link CreateInboxOptions} instances. */
  public static class Builder {
    private String emailAddress;
    private Duration ttl;
    private Boolean emailAuth;
    private String encryption;
    private Boolean spamAnalysis;

    /**
     * Sets a custom email address for the inbox.
     *
     * <p>This can be a full email address or just a domain to request a specific format.
     *
     * @param emailAddress the email address or domain
     * @return this builder
     */
    public Builder emailAddress(String emailAddress) {
      this.emailAddress = emailAddress;
      return this;
    }

    /**
     * Sets the time-to-live for the inbox.
     *
     * <p>The inbox will be automatically deleted after this duration.
     *
     * @param ttl the TTL duration
     * @return this builder
     */
    public Builder ttl(Duration ttl) {
      this.ttl = ttl;
      return this;
    }

    /**
     * Sets whether email authentication (SPF, DKIM, DMARC, PTR) is enabled for this inbox.
     *
     * <p>When {@code true}, authentication checks are performed on incoming emails. When {@code
     * false}, all checks are skipped and return status 'skipped'. When {@code null} (the default),
     * the server default is used.
     *
     * @param emailAuth true to enable, false to disable, null for server default
     * @return this builder
     */
    public Builder emailAuth(Boolean emailAuth) {
      this.emailAuth = emailAuth;
      return this;
    }

    /**
     * Sets the encryption mode for this inbox.
     *
     * <p>Values:
     *
     * <ul>
     *   <li>{@code "encrypted"} - Request an encrypted inbox (end-to-end encrypted)
     *   <li>{@code "plain"} - Request a plain inbox (no encryption)
     *   <li>{@code null} - Use server default based on encryption policy
     * </ul>
     *
     * <p>The server may reject the request if the encryption policy does not allow overrides.
     *
     * @param encryption the encryption mode ("encrypted", "plain", or null for server default)
     * @return this builder
     */
    public Builder encryption(String encryption) {
      this.encryption = encryption;
      return this;
    }

    /**
     * Convenience method to request a plain (unencrypted) inbox.
     *
     * @return this builder
     */
    public Builder plain() {
      this.encryption = "plain";
      return this;
    }

    /**
     * Convenience method to request an encrypted inbox.
     *
     * @return this builder
     */
    public Builder encrypted() {
      this.encryption = "encrypted";
      return this;
    }

    /**
     * Sets whether spam analysis is enabled for this inbox.
     *
     * <p>When {@code true}, spam analysis is performed on incoming emails. When {@code false}, spam
     * analysis is skipped. When {@code null} (the default), the server default is used.
     *
     * @param spamAnalysis true to enable, false to disable, null for server default
     * @return this builder
     */
    public Builder spamAnalysis(Boolean spamAnalysis) {
      this.spamAnalysis = spamAnalysis;
      return this;
    }

    /**
     * Builds the options.
     *
     * @return a new CreateInboxOptions instance
     */
    public CreateInboxOptions build() {
      return new CreateInboxOptions(emailAddress, ttl, emailAuth, encryption, spamAnalysis);
    }
  }
}
