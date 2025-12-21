package com.vaultsandbox.client;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the VaultSandbox client.
 *
 * <p>Use {@link #builder()} to create a configuration with custom settings. All settings have
 * sensible defaults, so only the API key is required.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ClientConfig config = ClientConfig.builder()
 *     .apiKey("your-api-key")
 *     .waitTimeout(Duration.ofSeconds(60))
 *     .strategy(StrategyType.SSE)
 *     .build();
 *
 * VaultSandboxClient client = VaultSandboxClient.create(config);
 * }</pre>
 *
 * @see VaultSandboxClient#create(ClientConfig)
 * @see StrategyType
 */
public final class ClientConfig {
  private final String baseUrl;
  private final String apiKey;
  private final StrategyType strategy;
  private final Duration httpTimeout;
  private final Duration waitTimeout;
  private final int maxRetries;
  private final Duration retryDelay;

  // SSE settings
  private final Duration sseReconnectInterval;
  private final int sseMaxReconnectAttempts;

  // Polling settings
  private final Duration pollInterval;
  private final Duration maxBackoff;
  private final double backoffMultiplier;
  private final double jitterFactor;

  // Retry settings
  private final Set<Integer> retryOn;

  private ClientConfig(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey is required");
    this.strategy = builder.strategy;
    this.httpTimeout = builder.httpTimeout;
    this.waitTimeout = builder.waitTimeout;
    this.maxRetries = builder.maxRetries;
    this.retryDelay = builder.retryDelay;
    this.sseReconnectInterval = builder.sseReconnectInterval;
    this.sseMaxReconnectAttempts = builder.sseMaxReconnectAttempts;
    this.pollInterval = builder.pollInterval;
    this.maxBackoff = builder.maxBackoff;
    this.backoffMultiplier = builder.backoffMultiplier;
    this.jitterFactor = builder.jitterFactor;
    this.retryOn = builder.retryOn;
  }

  /**
   * Creates a new configuration builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the base URL of the VaultSandbox API. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** Returns the API key. */
  public String getApiKey() {
    return apiKey;
  }

  /** Returns the email delivery strategy (SSE, polling, or auto). */
  public StrategyType getStrategy() {
    return strategy;
  }

  /** Returns the HTTP request timeout. */
  public Duration getHttpTimeout() {
    return httpTimeout;
  }

  /** Returns the default timeout for waiting for emails. */
  public Duration getWaitTimeout() {
    return waitTimeout;
  }

  /** Returns the maximum number of retry attempts for failed requests. */
  public int getMaxRetries() {
    return maxRetries;
  }

  /** Returns the delay between retry attempts. */
  public Duration getRetryDelay() {
    return retryDelay;
  }

  /** Returns the interval between SSE reconnection attempts. */
  public Duration getSseReconnectInterval() {
    return sseReconnectInterval;
  }

  /** Returns the maximum number of SSE reconnection attempts. */
  public int getSseMaxReconnectAttempts() {
    return sseMaxReconnectAttempts;
  }

  /** Returns the polling interval for the polling strategy. */
  public Duration getPollInterval() {
    return pollInterval;
  }

  /** Returns the maximum backoff duration for exponential backoff. */
  public Duration getMaxBackoff() {
    return maxBackoff;
  }

  /** Returns the backoff multiplier for exponential backoff. */
  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  /** Returns the jitter factor for randomizing backoff delays. */
  public double getJitterFactor() {
    return jitterFactor;
  }

  /** Returns the HTTP status codes that should trigger a retry. */
  public Set<Integer> getRetryOn() {
    return Set.copyOf(retryOn);
  }

  /** Builder for creating {@link ClientConfig} instances. */
  public static class Builder {
    private String baseUrl = "https://smtp.vaultsandbox.com";
    private String apiKey;
    private StrategyType strategy = StrategyType.AUTO;
    private Duration httpTimeout = Duration.ofSeconds(30);
    private Duration waitTimeout = Duration.ofSeconds(30);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private Duration sseReconnectInterval = Duration.ofSeconds(5);
    private int sseMaxReconnectAttempts = 10;
    private Duration pollInterval = Duration.ofSeconds(2);
    private Duration maxBackoff = Duration.ofSeconds(30);
    private double backoffMultiplier = 1.5;
    private double jitterFactor = 0.3;
    private Set<Integer> retryOn = Set.of(408, 429, 500, 502, 503, 504);

    /**
     * Sets the base URL of the VaultSandbox API.
     *
     * @param url the base URL (default: https://smtp.vaultsandbox.com)
     * @return this builder
     */
    public Builder baseUrl(String url) {
      this.baseUrl = url;
      return this;
    }

    /**
     * Sets the API key (required).
     *
     * @param key the VaultSandbox API key
     * @return this builder
     */
    public Builder apiKey(String key) {
      this.apiKey = key;
      return this;
    }

    /**
     * Sets the email delivery strategy.
     *
     * @param type the strategy type (default: AUTO)
     * @return this builder
     * @see StrategyType
     */
    public Builder strategy(StrategyType type) {
      this.strategy = type;
      return this;
    }

    /**
     * Sets the HTTP request timeout.
     *
     * @param timeout the timeout duration (default: 30 seconds)
     * @return this builder
     */
    public Builder httpTimeout(Duration timeout) {
      this.httpTimeout = timeout;
      return this;
    }

    /**
     * Sets the default timeout for waiting for emails.
     *
     * @param timeout the timeout duration (default: 30 seconds)
     * @return this builder
     */
    public Builder waitTimeout(Duration timeout) {
      this.waitTimeout = timeout;
      return this;
    }

    /**
     * Sets the maximum number of retry attempts for failed requests.
     *
     * @param retries the number of retries (default: 3)
     * @return this builder
     */
    public Builder maxRetries(int retries) {
      this.maxRetries = retries;
      return this;
    }

    /**
     * Sets the delay between retry attempts.
     *
     * @param delay the delay duration (default: 1 second)
     * @return this builder
     */
    public Builder retryDelay(Duration delay) {
      this.retryDelay = delay;
      return this;
    }

    /**
     * Sets the interval between SSE reconnection attempts.
     *
     * @param interval the reconnect interval (default: 5 seconds)
     * @return this builder
     */
    public Builder sseReconnectInterval(Duration interval) {
      this.sseReconnectInterval = interval;
      return this;
    }

    /**
     * Sets the maximum number of SSE reconnection attempts.
     *
     * @param attempts the max attempts (default: 10)
     * @return this builder
     */
    public Builder sseMaxReconnectAttempts(int attempts) {
      this.sseMaxReconnectAttempts = attempts;
      return this;
    }

    /**
     * Sets the polling interval for the polling strategy.
     *
     * @param interval the poll interval (default: 2 seconds)
     * @return this builder
     */
    public Builder pollInterval(Duration interval) {
      this.pollInterval = interval;
      return this;
    }

    /**
     * Sets the maximum backoff duration for exponential backoff.
     *
     * @param maxBackoff the max backoff (default: 30 seconds)
     * @return this builder
     */
    public Builder maxBackoff(Duration maxBackoff) {
      this.maxBackoff = maxBackoff;
      return this;
    }

    /**
     * Sets the backoff multiplier for exponential backoff.
     *
     * @param multiplier the multiplier (default: 1.5)
     * @return this builder
     */
    public Builder backoffMultiplier(double multiplier) {
      this.backoffMultiplier = multiplier;
      return this;
    }

    /**
     * Sets the jitter factor for randomizing backoff delays.
     *
     * @param factor the jitter factor between 0 and 1 (default: 0.3)
     * @return this builder
     */
    public Builder jitterFactor(double factor) {
      this.jitterFactor = factor;
      return this;
    }

    /**
     * Sets the HTTP status codes that should trigger a retry.
     *
     * @param statusCodes the set of status codes to retry on (default: 408, 429, 500, 502, 503,
     *     504)
     * @return this builder
     */
    public Builder retryOn(Set<Integer> statusCodes) {
      this.retryOn = Set.copyOf(statusCodes);
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return a new ClientConfig instance
     * @throws NullPointerException if apiKey is not set
     */
    public ClientConfig build() {
      return new ClientConfig(this);
    }
  }
}
