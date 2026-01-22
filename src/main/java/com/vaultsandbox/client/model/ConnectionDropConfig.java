package com.vaultsandbox.client.model;

/**
 * Configuration for connection drop during chaos testing.
 *
 * <p>Simulates connection failures by dropping the SMTP connection.
 *
 * @see ChaosConfig
 */
public class ConnectionDropConfig {
  private Boolean enabled;
  private Double probability;
  private Boolean graceful;

  public ConnectionDropConfig() {}

  /**
   * Creates a connection drop configuration.
   *
   * @param enabled whether connection dropping is enabled
   */
  public ConnectionDropConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether connection dropping is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether connection dropping is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether connection dropping is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the probability of dropping the connection.
   *
   * @return probability between 0.0 and 1.0 (default: 1.0)
   */
  public Double getProbability() {
    return probability;
  }

  /**
   * Sets the probability of dropping the connection.
   *
   * @param probability probability between 0.0 and 1.0
   */
  public void setProbability(Double probability) {
    this.probability = probability;
  }

  /**
   * Returns whether graceful close is used.
   *
   * @return true for FIN, false for RST
   */
  public Boolean getGraceful() {
    return graceful;
  }

  /**
   * Sets whether to use graceful close.
   *
   * @param graceful true for FIN, false for RST
   */
  public void setGraceful(Boolean graceful) {
    this.graceful = graceful;
  }

  /**
   * Returns whether graceful close is used.
   *
   * @return true for graceful close (default: true)
   */
  public boolean isGraceful() {
    return graceful == null || graceful;
  }

  /** Builder for creating ConnectionDropConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ConnectionDropConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Double probability;
    private Boolean graceful;

    /**
     * Sets whether connection dropping is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the probability of dropping the connection.
     *
     * @param probability probability between 0.0 and 1.0
     * @return this builder
     */
    public Builder probability(double probability) {
      this.probability = probability;
      return this;
    }

    /**
     * Sets whether to use graceful close.
     *
     * @param graceful true for FIN, false for RST
     * @return this builder
     */
    public Builder graceful(boolean graceful) {
      this.graceful = graceful;
      return this;
    }

    /**
     * Builds the connection drop configuration.
     *
     * @return the connection drop configuration
     */
    public ConnectionDropConfig build() {
      ConnectionDropConfig config = new ConnectionDropConfig(enabled);
      config.setProbability(probability);
      config.setGraceful(graceful);
      return config;
    }
  }
}
