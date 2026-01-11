package com.vaultsandbox.client.strategy;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.StrategyType;
import okhttp3.OkHttpClient;

/** Factory for creating delivery strategies. */
public final class StrategyFactory {

  private StrategyFactory() {}

  /**
   * Create a delivery strategy based on the specified type.
   *
   * @param type the strategy type
   * @param httpClient OkHttp client for SSE connections
   * @param config client configuration
   * @return the appropriate delivery strategy
   */
  public static DeliveryStrategy create(
      StrategyType type, OkHttpClient httpClient, ClientConfig config) {
    return switch (type) {
      case SSE -> new SseStrategy(httpClient, config);
      case POLLING -> new PollingStrategy(config);
    };
  }
}
