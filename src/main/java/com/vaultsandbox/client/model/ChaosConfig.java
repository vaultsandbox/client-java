package com.vaultsandbox.client.model;

/**
 * Configuration for chaos engineering on an inbox.
 *
 * <p>Chaos engineering allows controlled injection of failures and delays into email processing to
 * test system resilience. Configuration is applied per-inbox, allowing different chaos scenarios
 * for different test inboxes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Enable latency injection
 * ChaosConfig config = ChaosConfig.builder()
 *     .enabled(true)
 *     .latency(LatencyConfig.builder()
 *         .minDelayMs(1000)
 *         .maxDelayMs(5000)
 *         .probability(0.5)
 *         .build())
 *     .build();
 *
 * inbox.setChaos(config);
 * }</pre>
 *
 * <p>When multiple chaos types are enabled, they are evaluated in priority order:
 *
 * <ol>
 *   <li>Connection Drop (most disruptive)
 *   <li>Greylisting
 *   <li>Random Error
 *   <li>Blackhole
 *   <li>Latency (least disruptive)
 * </ol>
 *
 * @see LatencyConfig
 * @see ConnectionDropConfig
 * @see RandomErrorConfig
 * @see GreylistConfig
 * @see BlackholeConfig
 */
public class ChaosConfig {
  private Boolean enabled;
  private String expiresAt;
  private LatencyConfig latency;
  private ConnectionDropConfig connectionDrop;
  private RandomErrorConfig randomError;
  private GreylistConfig greylist;
  private BlackholeConfig blackhole;

  public ChaosConfig() {}

  /**
   * Creates a chaos configuration.
   *
   * @param enabled master switch for chaos on this inbox
   */
  public ChaosConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether chaos is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether chaos is enabled.
   *
   * @param enabled master switch for chaos
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether chaos is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the auto-expiration timestamp.
   *
   * @return ISO 8601 timestamp, or null if no auto-disable
   */
  public String getExpiresAt() {
    return expiresAt;
  }

  /**
   * Sets the auto-expiration timestamp.
   *
   * @param expiresAt ISO 8601 timestamp for auto-disable
   */
  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  /**
   * Returns the latency injection configuration.
   *
   * @return latency config, or null if not configured
   */
  public LatencyConfig getLatency() {
    return latency;
  }

  /**
   * Sets the latency injection configuration.
   *
   * @param latency latency config
   */
  public void setLatency(LatencyConfig latency) {
    this.latency = latency;
  }

  /**
   * Returns the connection drop configuration.
   *
   * @return connection drop config, or null if not configured
   */
  public ConnectionDropConfig getConnectionDrop() {
    return connectionDrop;
  }

  /**
   * Sets the connection drop configuration.
   *
   * @param connectionDrop connection drop config
   */
  public void setConnectionDrop(ConnectionDropConfig connectionDrop) {
    this.connectionDrop = connectionDrop;
  }

  /**
   * Returns the random error configuration.
   *
   * @return random error config, or null if not configured
   */
  public RandomErrorConfig getRandomError() {
    return randomError;
  }

  /**
   * Sets the random error configuration.
   *
   * @param randomError random error config
   */
  public void setRandomError(RandomErrorConfig randomError) {
    this.randomError = randomError;
  }

  /**
   * Returns the greylisting configuration.
   *
   * @return greylist config, or null if not configured
   */
  public GreylistConfig getGreylist() {
    return greylist;
  }

  /**
   * Sets the greylisting configuration.
   *
   * @param greylist greylist config
   */
  public void setGreylist(GreylistConfig greylist) {
    this.greylist = greylist;
  }

  /**
   * Returns the blackhole configuration.
   *
   * @return blackhole config, or null if not configured
   */
  public BlackholeConfig getBlackhole() {
    return blackhole;
  }

  /**
   * Sets the blackhole configuration.
   *
   * @param blackhole blackhole config
   */
  public void setBlackhole(BlackholeConfig blackhole) {
    this.blackhole = blackhole;
  }

  /**
   * Creates a disabled chaos configuration.
   *
   * @return a chaos config with enabled=false
   */
  public static ChaosConfig disabled() {
    return new ChaosConfig(false);
  }

  /** Builder for creating ChaosConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ChaosConfig. */
  public static class Builder {
    private boolean enabled = true;
    private String expiresAt;
    private LatencyConfig latency;
    private ConnectionDropConfig connectionDrop;
    private RandomErrorConfig randomError;
    private GreylistConfig greylist;
    private BlackholeConfig blackhole;

    /**
     * Sets whether chaos is enabled.
     *
     * @param enabled master switch for chaos
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the auto-expiration timestamp.
     *
     * @param expiresAt ISO 8601 timestamp
     * @return this builder
     */
    public Builder expiresAt(String expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    /**
     * Sets the latency injection configuration.
     *
     * @param latency latency config
     * @return this builder
     */
    public Builder latency(LatencyConfig latency) {
      this.latency = latency;
      return this;
    }

    /**
     * Sets the connection drop configuration.
     *
     * @param connectionDrop connection drop config
     * @return this builder
     */
    public Builder connectionDrop(ConnectionDropConfig connectionDrop) {
      this.connectionDrop = connectionDrop;
      return this;
    }

    /**
     * Sets the random error configuration.
     *
     * @param randomError random error config
     * @return this builder
     */
    public Builder randomError(RandomErrorConfig randomError) {
      this.randomError = randomError;
      return this;
    }

    /**
     * Sets the greylisting configuration.
     *
     * @param greylist greylist config
     * @return this builder
     */
    public Builder greylist(GreylistConfig greylist) {
      this.greylist = greylist;
      return this;
    }

    /**
     * Sets the blackhole configuration.
     *
     * @param blackhole blackhole config
     * @return this builder
     */
    public Builder blackhole(BlackholeConfig blackhole) {
      this.blackhole = blackhole;
      return this;
    }

    /**
     * Builds the chaos configuration.
     *
     * @return the chaos configuration
     */
    public ChaosConfig build() {
      ChaosConfig config = new ChaosConfig(enabled);
      config.setExpiresAt(expiresAt);
      config.setLatency(latency);
      config.setConnectionDrop(connectionDrop);
      config.setRandomError(randomError);
      config.setGreylist(greylist);
      config.setBlackhole(blackhole);
      return config;
    }
  }
}
