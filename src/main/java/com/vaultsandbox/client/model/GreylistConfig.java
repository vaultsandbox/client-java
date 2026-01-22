package com.vaultsandbox.client.model;

/**
 * Configuration for greylisting simulation during chaos testing.
 *
 * <p>Simulates greylisting behavior by rejecting initial delivery attempts and accepting on retry.
 *
 * @see ChaosConfig
 */
public class GreylistConfig {
  private Boolean enabled;
  private Integer retryWindowMs;
  private Integer maxAttempts;
  private GreylistTrackBy trackBy;

  public GreylistConfig() {}

  /**
   * Creates a greylist configuration.
   *
   * @param enabled whether greylisting is enabled
   */
  public GreylistConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether greylisting is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether greylisting is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether greylisting is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the retry window in milliseconds.
   *
   * @return window for tracking retry attempts (default: 300000)
   */
  public Integer getRetryWindowMs() {
    return retryWindowMs;
  }

  /**
   * Sets the retry window in milliseconds.
   *
   * @param retryWindowMs window for tracking retry attempts
   */
  public void setRetryWindowMs(Integer retryWindowMs) {
    this.retryWindowMs = retryWindowMs;
  }

  /**
   * Returns the number of attempts before accepting.
   *
   * @return max attempts (default: 2)
   */
  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * Sets the number of attempts before accepting.
   *
   * @param maxAttempts number of attempts (first maxAttempts-1 are rejected)
   */
  public void setMaxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Returns how to identify unique senders.
   *
   * @return tracking method (default: IP_SENDER)
   */
  public GreylistTrackBy getTrackBy() {
    return trackBy;
  }

  /**
   * Sets how to identify unique senders.
   *
   * @param trackBy tracking method (IP, SENDER, or IP_SENDER)
   */
  public void setTrackBy(GreylistTrackBy trackBy) {
    this.trackBy = trackBy;
  }

  /** Builder for creating GreylistConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for GreylistConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Integer retryWindowMs;
    private Integer maxAttempts;
    private GreylistTrackBy trackBy;

    /**
     * Sets whether greylisting is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the retry window in milliseconds.
     *
     * @param retryWindowMs window for tracking retry attempts
     * @return this builder
     */
    public Builder retryWindowMs(int retryWindowMs) {
      this.retryWindowMs = retryWindowMs;
      return this;
    }

    /**
     * Sets the number of attempts before accepting.
     *
     * @param maxAttempts number of attempts
     * @return this builder
     */
    public Builder maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
    }

    /**
     * Sets how to identify unique senders.
     *
     * @param trackBy tracking method
     * @return this builder
     */
    public Builder trackBy(GreylistTrackBy trackBy) {
      this.trackBy = trackBy;
      return this;
    }

    /**
     * Builds the greylist configuration.
     *
     * @return the greylist configuration
     */
    public GreylistConfig build() {
      GreylistConfig config = new GreylistConfig(enabled);
      config.setRetryWindowMs(retryWindowMs);
      config.setMaxAttempts(maxAttempts);
      config.setTrackBy(trackBy);
      return config;
    }
  }
}
