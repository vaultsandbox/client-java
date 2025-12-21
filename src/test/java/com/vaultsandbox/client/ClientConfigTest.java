package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for ClientConfig default values and builder functionality. */
class ClientConfigTest {

  // ==================== Default Configuration Tests ====================

  @Test
  void testDefaultConfiguration() {
    ClientConfig config = ClientConfig.builder().apiKey("test-key").build();

    assertEquals("https://smtp.vaultsandbox.com", config.getBaseUrl());
    assertEquals("test-key", config.getApiKey());
    assertEquals(StrategyType.AUTO, config.getStrategy());
    assertEquals(Duration.ofSeconds(30), config.getHttpTimeout());
    assertEquals(Duration.ofSeconds(30), config.getWaitTimeout());
    assertEquals(3, config.getMaxRetries());
    assertEquals(Duration.ofSeconds(1), config.getRetryDelay());
  }

  @Test
  void testDefaultSseConfiguration() {
    ClientConfig config = ClientConfig.builder().apiKey("test-key").build();

    assertEquals(Duration.ofSeconds(5), config.getSseReconnectInterval());
    assertEquals(10, config.getSseMaxReconnectAttempts());
  }

  @Test
  void testDefaultPollingConfiguration() {
    ClientConfig config = ClientConfig.builder().apiKey("test-key").build();

    assertEquals(Duration.ofSeconds(2), config.getPollInterval());
    assertEquals(Duration.ofSeconds(30), config.getMaxBackoff());
    assertEquals(1.5, config.getBackoffMultiplier());
    assertEquals(0.3, config.getJitterFactor());
  }

  @Test
  void testDefaultRetryConfiguration() {
    ClientConfig config = ClientConfig.builder().apiKey("test-key").build();

    Set<Integer> expectedRetryCodes = Set.of(408, 429, 500, 502, 503, 504);
    assertEquals(expectedRetryCodes, config.getRetryOn());
  }

  // ==================== Required Fields Tests ====================

  @Test
  void testApiKeyRequired() {
    assertThrows(
        NullPointerException.class,
        () -> ClientConfig.builder().build(),
        "Should throw when apiKey is not set");
  }

  @Test
  void testApiKeyCannotBeNull() {
    assertThrows(
        NullPointerException.class,
        () -> ClientConfig.builder().apiKey(null).build(),
        "Should throw when apiKey is null");
  }

  // ==================== Custom Configuration Tests ====================

  @Test
  void testCustomBaseUrl() {
    ClientConfig config =
        ClientConfig.builder().apiKey("test-key").baseUrl("http://localhost:3000").build();

    assertEquals("http://localhost:3000", config.getBaseUrl());
  }

  @Test
  void testCustomStrategy() {
    ClientConfig sseConfig =
        ClientConfig.builder().apiKey("test-key").strategy(StrategyType.SSE).build();

    ClientConfig pollingConfig =
        ClientConfig.builder().apiKey("test-key").strategy(StrategyType.POLLING).build();

    assertEquals(StrategyType.SSE, sseConfig.getStrategy());
    assertEquals(StrategyType.POLLING, pollingConfig.getStrategy());
  }

  @Test
  void testCustomTimeouts() {
    ClientConfig config =
        ClientConfig.builder()
            .apiKey("test-key")
            .httpTimeout(Duration.ofSeconds(60))
            .waitTimeout(Duration.ofMinutes(2))
            .build();

    assertEquals(Duration.ofSeconds(60), config.getHttpTimeout());
    assertEquals(Duration.ofMinutes(2), config.getWaitTimeout());
  }

  @Test
  void testCustomRetryConfiguration() {
    ClientConfig config =
        ClientConfig.builder()
            .apiKey("test-key")
            .maxRetries(5)
            .retryDelay(Duration.ofMillis(500))
            .retryOn(Set.of(500, 503))
            .build();

    assertEquals(5, config.getMaxRetries());
    assertEquals(Duration.ofMillis(500), config.getRetryDelay());
    assertEquals(Set.of(500, 503), config.getRetryOn());
  }

  @Test
  void testCustomSseConfiguration() {
    ClientConfig config =
        ClientConfig.builder()
            .apiKey("test-key")
            .sseReconnectInterval(Duration.ofSeconds(10))
            .sseMaxReconnectAttempts(5)
            .build();

    assertEquals(Duration.ofSeconds(10), config.getSseReconnectInterval());
    assertEquals(5, config.getSseMaxReconnectAttempts());
  }

  @Test
  void testCustomPollingConfiguration() {
    ClientConfig config =
        ClientConfig.builder()
            .apiKey("test-key")
            .pollInterval(Duration.ofSeconds(5))
            .maxBackoff(Duration.ofMinutes(1))
            .backoffMultiplier(2.0)
            .jitterFactor(0.5)
            .build();

    assertEquals(Duration.ofSeconds(5), config.getPollInterval());
    assertEquals(Duration.ofMinutes(1), config.getMaxBackoff());
    assertEquals(2.0, config.getBackoffMultiplier());
    assertEquals(0.5, config.getJitterFactor());
  }

  // ==================== Builder Chaining Tests ====================

  @Test
  void testBuilderChaining() {
    ClientConfig config =
        ClientConfig.builder()
            .apiKey("test-key")
            .baseUrl("http://localhost:3000")
            .strategy(StrategyType.POLLING)
            .httpTimeout(Duration.ofSeconds(45))
            .waitTimeout(Duration.ofSeconds(60))
            .maxRetries(5)
            .pollInterval(Duration.ofSeconds(3))
            .build();

    assertNotNull(config);
    assertEquals("test-key", config.getApiKey());
    assertEquals("http://localhost:3000", config.getBaseUrl());
    assertEquals(StrategyType.POLLING, config.getStrategy());
    assertEquals(Duration.ofSeconds(45), config.getHttpTimeout());
    assertEquals(Duration.ofSeconds(60), config.getWaitTimeout());
    assertEquals(5, config.getMaxRetries());
    assertEquals(Duration.ofSeconds(3), config.getPollInterval());
  }

  @Test
  void testBuilderCanBeReused() {
    ClientConfig.Builder builder = ClientConfig.builder().apiKey("key1");

    ClientConfig config1 = builder.build();
    ClientConfig config2 = builder.apiKey("key2").build();

    assertEquals("key1", config1.getApiKey());
    assertEquals("key2", config2.getApiKey());
  }

  // ==================== Configuration Immutability Tests ====================

  @Test
  void testRetryOnSetIsImmutable() {
    ClientConfig config = ClientConfig.builder().apiKey("test-key").build();

    Set<Integer> retryOn = config.getRetryOn();

    assertThrows(
        UnsupportedOperationException.class,
        () -> retryOn.add(400),
        "RetryOn set should be immutable");
  }
}
