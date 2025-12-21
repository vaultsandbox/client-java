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

  private CreateInboxOptions(String emailAddress, Duration ttl) {
    this.emailAddress = emailAddress;
    this.ttl = ttl;
  }

  /**
   * Returns default options for inbox creation.
   *
   * @return default options
   */
  public static CreateInboxOptions defaults() {
    return new CreateInboxOptions(null, null);
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

  /** Builder for creating {@link CreateInboxOptions} instances. */
  public static class Builder {
    private String emailAddress;
    private Duration ttl;

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
     * Builds the options.
     *
     * @return a new CreateInboxOptions instance
     */
    public CreateInboxOptions build() {
      return new CreateInboxOptions(emailAddress, ttl);
    }
  }
}
