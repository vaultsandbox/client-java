package com.vaultsandbox.client.model;

/**
 * Configuration for blackhole mode during chaos testing.
 *
 * <p>Accepts emails but silently discards them without storing.
 *
 * @see ChaosConfig
 */
public class BlackholeConfig {
  private Boolean enabled;
  private Boolean triggerWebhooks;

  public BlackholeConfig() {}

  /**
   * Creates a blackhole configuration.
   *
   * @param enabled whether blackhole mode is enabled
   */
  public BlackholeConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether blackhole mode is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether blackhole mode is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether blackhole mode is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns whether webhooks are triggered for discarded emails.
   *
   * @return true if webhooks are triggered (default: false)
   */
  public Boolean getTriggerWebhooks() {
    return triggerWebhooks;
  }

  /**
   * Sets whether webhooks are triggered for discarded emails.
   *
   * @param triggerWebhooks true to trigger webhooks
   */
  public void setTriggerWebhooks(Boolean triggerWebhooks) {
    this.triggerWebhooks = triggerWebhooks;
  }

  /**
   * Returns whether webhooks are triggered for discarded emails.
   *
   * @return true if webhooks are triggered
   */
  public boolean isTriggerWebhooks() {
    return triggerWebhooks != null && triggerWebhooks;
  }

  /** Builder for creating BlackholeConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for BlackholeConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Boolean triggerWebhooks;

    /**
     * Sets whether blackhole mode is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets whether webhooks are triggered for discarded emails.
     *
     * @param triggerWebhooks true to trigger webhooks
     * @return this builder
     */
    public Builder triggerWebhooks(boolean triggerWebhooks) {
      this.triggerWebhooks = triggerWebhooks;
      return this;
    }

    /**
     * Builds the blackhole configuration.
     *
     * @return the blackhole configuration
     */
    public BlackholeConfig build() {
      BlackholeConfig config = new BlackholeConfig(enabled);
      config.setTriggerWebhooks(triggerWebhooks);
      return config;
    }
  }
}
