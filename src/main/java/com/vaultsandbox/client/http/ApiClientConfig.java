package com.vaultsandbox.client.http;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import okhttp3.OkHttpClient;

public final class ApiClientConfig {
  private final String baseUrl;
  private final String apiKey;
  private final Duration timeout;
  private final int maxRetries;
  private final Duration retryDelay;
  private final Set<Integer> retryOn;
  private final OkHttpClient httpClient;

  private ApiClientConfig(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey is required");
    this.timeout = builder.timeout;
    this.maxRetries = builder.maxRetries;
    this.retryDelay = builder.retryDelay;
    this.retryOn = builder.retryOn;
    this.httpClient = builder.httpClient;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public Duration getRetryDelay() {
    return retryDelay;
  }

  public Set<Integer> getRetryOn() {
    return Set.copyOf(retryOn);
  }

  public OkHttpClient getHttpClient() {
    return httpClient;
  }

  public static class Builder {
    private String baseUrl = "https://smtp.vaultsandbox.com";
    private String apiKey;
    private Duration timeout = Duration.ofSeconds(30);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private Set<Integer> retryOn = Set.of(408, 429, 500, 502, 503, 504);
    private OkHttpClient httpClient;

    public Builder baseUrl(String url) {
      this.baseUrl = url;
      return this;
    }

    public Builder apiKey(String key) {
      this.apiKey = key;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder maxRetries(int retries) {
      this.maxRetries = retries;
      return this;
    }

    public Builder retryDelay(Duration delay) {
      this.retryDelay = delay;
      return this;
    }

    public Builder retryOn(Set<Integer> statusCodes) {
      this.retryOn = Set.copyOf(statusCodes);
      return this;
    }

    public Builder httpClient(OkHttpClient client) {
      this.httpClient = client;
      return this;
    }

    public ApiClientConfig build() {
      return new ApiClientConfig(this);
    }
  }
}
