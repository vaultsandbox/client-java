package com.vaultsandbox.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for random error generation during chaos testing.
 *
 * <p>Returns random SMTP error codes to simulate failures.
 *
 * @see ChaosConfig
 */
public class RandomErrorConfig {
  private Boolean enabled;
  private Double errorRate;
  private List<ChaosErrorType> errorTypes;

  public RandomErrorConfig() {}

  /**
   * Creates a random error configuration.
   *
   * @param enabled whether random error generation is enabled
   */
  public RandomErrorConfig(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether random error generation is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether random error generation is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether random error generation is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the error rate.
   *
   * @return probability of returning an error (default: 0.1)
   */
  public Double getErrorRate() {
    return errorRate;
  }

  /**
   * Sets the error rate.
   *
   * @param errorRate probability between 0.0 and 1.0
   */
  public void setErrorRate(Double errorRate) {
    this.errorRate = errorRate;
  }

  /**
   * Returns the types of errors to generate.
   *
   * @return list of error types (default: [TEMPORARY])
   */
  public List<ChaosErrorType> getErrorTypes() {
    return errorTypes;
  }

  /**
   * Sets the types of errors to generate.
   *
   * @param errorTypes list of error types (TEMPORARY, PERMANENT)
   */
  public void setErrorTypes(List<ChaosErrorType> errorTypes) {
    this.errorTypes = errorTypes != null ? new ArrayList<>(errorTypes) : null;
  }

  /** Builder for creating RandomErrorConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for RandomErrorConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Double errorRate;
    private List<ChaosErrorType> errorTypes;

    /**
     * Sets whether random error generation is enabled.
     *
     * @param enabled true to enable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the error rate.
     *
     * @param errorRate probability between 0.0 and 1.0
     * @return this builder
     */
    public Builder errorRate(double errorRate) {
      this.errorRate = errorRate;
      return this;
    }

    /**
     * Sets the types of errors to generate.
     *
     * @param errorTypes error types to include
     * @return this builder
     */
    public Builder errorTypes(List<ChaosErrorType> errorTypes) {
      this.errorTypes = errorTypes != null ? new ArrayList<>(errorTypes) : null;
      return this;
    }

    /**
     * Sets the types of errors to generate.
     *
     * @param errorTypes error types to include
     * @return this builder
     */
    public Builder errorTypes(ChaosErrorType... errorTypes) {
      this.errorTypes = new ArrayList<>(Arrays.asList(errorTypes));
      return this;
    }

    /**
     * Builds the random error configuration.
     *
     * @return the random error configuration
     */
    public RandomErrorConfig build() {
      RandomErrorConfig config = new RandomErrorConfig(enabled);
      config.setErrorRate(errorRate);
      config.setErrorTypes(errorTypes);
      return config;
    }
  }
}
