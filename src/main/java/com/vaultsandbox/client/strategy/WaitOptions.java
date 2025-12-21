package com.vaultsandbox.client.strategy;

import java.time.Duration;

/**
 * Options for waiting for emails.
 *
 * <p>Combines filter criteria with timeout and polling configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WaitOptions options = WaitOptions.builder()
 *     .filter(EmailFilter.builder().subject("Welcome").build())
 *     .timeout(Duration.ofSeconds(60))
 *     .pollInterval(Duration.ofMillis(500))
 *     .build();
 *
 * Email email = inbox.waitForEmail(options);
 * }</pre>
 *
 * @see EmailFilter
 */
public final class WaitOptions {
  private final EmailFilter filter;
  private final Duration timeout;
  private final Duration pollInterval;

  private WaitOptions(Builder builder) {
    this.filter = builder.filter != null ? builder.filter : EmailFilter.any();
    this.timeout = builder.timeout;
    this.pollInterval = builder.pollInterval;
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
   * Returns the email filter criteria.
   *
   * @return the filter, never null
   */
  public EmailFilter getFilter() {
    return filter;
  }

  /**
   * Returns the timeout duration.
   *
   * @return the timeout, or null to use the default
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Returns the poll interval for polling-based strategies.
   *
   * @return the poll interval, or null to use the default
   */
  public Duration getPollInterval() {
    return pollInterval;
  }

  /** Builder for creating {@link WaitOptions} instances. */
  public static class Builder {
    private EmailFilter filter;
    private Duration timeout;
    private Duration pollInterval;

    /**
     * Sets the email filter criteria.
     *
     * @param filter the filter to use
     * @return this builder
     */
    public Builder filter(EmailFilter filter) {
      this.filter = filter;
      return this;
    }

    /**
     * Sets the timeout duration.
     *
     * @param timeout the maximum time to wait
     * @return this builder
     */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Sets the poll interval for polling-based strategies.
     *
     * <p>This overrides the default poll interval configured on the client.
     *
     * @param pollInterval the interval between polls
     * @return this builder
     */
    public Builder pollInterval(Duration pollInterval) {
      this.pollInterval = pollInterval;
      return this;
    }

    /**
     * Builds the options.
     *
     * @return a new WaitOptions instance
     */
    public WaitOptions build() {
      return new WaitOptions(this);
    }
  }
}
