package com.vaultsandbox.client.model;

/**
 * Configuration for latency injection during chaos testing.
 *
 * <p>Injects artificial delays into email processing.
 *
 * @see ChaosConfig
 */
public class LatencyConfig {
  private Boolean enabled;
  private Integer minDelayMs;
  private Integer maxDelayMs;
  private Boolean jitter;
  private Double probability;

  public LatencyConfig() {}

  /**
   * Creates a latency configuration.
   *
   * @param enabled whether latency injection is enabled
   */
  public LatencyConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether latency injection is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether latency injection is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether latency injection is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the minimum delay in milliseconds.
   *
   * @return minimum delay (default: 500)
   */
  public Integer getMinDelayMs() {
    return minDelayMs;
  }

  /**
   * Sets the minimum delay in milliseconds.
   *
   * @param minDelayMs minimum delay (must be >= 0)
   */
  public void setMinDelayMs(Integer minDelayMs) {
    this.minDelayMs = minDelayMs;
  }

  /**
   * Returns the maximum delay in milliseconds.
   *
   * @return maximum delay (default: 10000)
   */
  public Integer getMaxDelayMs() {
    return maxDelayMs;
  }

  /**
   * Sets the maximum delay in milliseconds.
   *
   * @param maxDelayMs maximum delay (must be >= minDelayMs and <= 60000)
   */
  public void setMaxDelayMs(Integer maxDelayMs) {
    this.maxDelayMs = maxDelayMs;
  }

  /**
   * Returns whether jitter is enabled.
   *
   * @return true if delay is randomized within range
   */
  public Boolean getJitter() {
    return jitter;
  }

  /**
   * Sets whether jitter is enabled.
   *
   * @param jitter true to randomize delay within range, false for fixed at maxDelayMs
   */
  public void setJitter(Boolean jitter) {
    this.jitter = jitter;
  }

  /**
   * Returns whether jitter is enabled.
   *
   * @return true if delay is randomized (default: true)
   */
  public boolean isJitter() {
    return jitter == null || jitter;
  }

  /**
   * Returns the probability of applying delay.
   *
   * @return probability between 0.0 and 1.0 (default: 1.0)
   */
  public Double getProbability() {
    return probability;
  }

  /**
   * Sets the probability of applying delay.
   *
   * @param probability probability between 0.0 and 1.0
   */
  public void setProbability(Double probability) {
    this.probability = probability;
  }

  /** Builder for creating LatencyConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for LatencyConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Integer minDelayMs;
    private Integer maxDelayMs;
    private Boolean jitter;
    private Double probability;

    /**
     * Sets whether latency injection is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the minimum delay in milliseconds.
     *
     * @param minDelayMs minimum delay (must be >= 0)
     * @return this builder
     */
    public Builder minDelayMs(int minDelayMs) {
      this.minDelayMs = minDelayMs;
      return this;
    }

    /**
     * Sets the maximum delay in milliseconds.
     *
     * @param maxDelayMs maximum delay (must be >= minDelayMs and <= 60000)
     * @return this builder
     */
    public Builder maxDelayMs(int maxDelayMs) {
      this.maxDelayMs = maxDelayMs;
      return this;
    }

    /**
     * Sets whether jitter is enabled.
     *
     * @param jitter true to randomize delay within range
     * @return this builder
     */
    public Builder jitter(boolean jitter) {
      this.jitter = jitter;
      return this;
    }

    /**
     * Sets the probability of applying delay.
     *
     * @param probability probability between 0.0 and 1.0
     * @return this builder
     */
    public Builder probability(double probability) {
      this.probability = probability;
      return this;
    }

    /**
     * Builds the latency configuration.
     *
     * @return the latency configuration
     */
    public LatencyConfig build() {
      LatencyConfig config = new LatencyConfig(enabled);
      config.setMinDelayMs(minDelayMs);
      config.setMaxDelayMs(maxDelayMs);
      config.setJitter(jitter);
      config.setProbability(probability);
      return config;
    }
  }
}
