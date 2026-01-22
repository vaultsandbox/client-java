package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChaosConfigTest {

  private static final Gson GSON = new GsonBuilder().create();

  // ==================== ChaosConfig Tests ====================

  @Test
  void testChaosConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "expiresAt": "2024-12-31T23:59:59.000Z",
          "latency": {
            "enabled": true,
            "minDelayMs": 500,
            "maxDelayMs": 10000,
            "jitter": true,
            "probability": 1.0
          },
          "connectionDrop": {
            "enabled": true,
            "probability": 0.5,
            "graceful": false
          },
          "randomError": {
            "enabled": true,
            "errorRate": 0.2,
            "errorTypes": ["temporary", "permanent"]
          },
          "greylist": {
            "enabled": true,
            "retryWindowMs": 300000,
            "maxAttempts": 3,
            "trackBy": "ip_sender"
          },
          "blackhole": {
            "enabled": true,
            "triggerWebhooks": false
          }
        }
        """;
    ChaosConfig config = GSON.fromJson(json, ChaosConfig.class);

    assertTrue(config.isEnabled());
    assertEquals("2024-12-31T23:59:59.000Z", config.getExpiresAt());
    assertNotNull(config.getLatency());
    assertNotNull(config.getConnectionDrop());
    assertNotNull(config.getRandomError());
    assertNotNull(config.getGreylist());
    assertNotNull(config.getBlackhole());
  }

  @Test
  void testChaosConfigBuilder() {
    ChaosConfig config =
        ChaosConfig.builder()
            .enabled(true)
            .expiresAt("2024-12-31T23:59:59.000Z")
            .latency(LatencyConfig.builder().minDelayMs(1000).maxDelayMs(5000).build())
            .build();

    assertTrue(config.isEnabled());
    assertEquals("2024-12-31T23:59:59.000Z", config.getExpiresAt());
    assertNotNull(config.getLatency());
    assertTrue(config.getLatency().isEnabled());
    assertEquals(Integer.valueOf(1000), config.getLatency().getMinDelayMs());
    assertEquals(Integer.valueOf(5000), config.getLatency().getMaxDelayMs());
  }

  @Test
  void testChaosConfigDisabled() {
    ChaosConfig config = ChaosConfig.disabled();

    assertFalse(config.isEnabled());
  }

  @Test
  void testChaosConfigEnabledNull() {
    ChaosConfig config = new ChaosConfig();

    assertFalse(config.isEnabled());
    assertNull(config.getEnabled());
  }

  // ==================== LatencyConfig Tests ====================

  @Test
  void testLatencyConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "minDelayMs": 500,
          "maxDelayMs": 10000,
          "jitter": true,
          "probability": 0.5
        }
        """;
    LatencyConfig config = GSON.fromJson(json, LatencyConfig.class);

    assertTrue(config.isEnabled());
    assertEquals(Integer.valueOf(500), config.getMinDelayMs());
    assertEquals(Integer.valueOf(10000), config.getMaxDelayMs());
    assertTrue(config.isJitter());
    assertEquals(Double.valueOf(0.5), config.getProbability());
  }

  @Test
  void testLatencyConfigBuilder() {
    LatencyConfig config =
        LatencyConfig.builder()
            .enabled(true)
            .minDelayMs(1000)
            .maxDelayMs(5000)
            .jitter(false)
            .probability(0.8)
            .build();

    assertTrue(config.isEnabled());
    assertEquals(Integer.valueOf(1000), config.getMinDelayMs());
    assertEquals(Integer.valueOf(5000), config.getMaxDelayMs());
    assertFalse(config.isJitter());
    assertEquals(Double.valueOf(0.8), config.getProbability());
  }

  @Test
  void testLatencyConfigJitterDefaultTrue() {
    LatencyConfig config = new LatencyConfig(true);

    assertTrue(config.isJitter());
  }

  @Test
  void testLatencyConfigSetters() {
    LatencyConfig config = new LatencyConfig();
    config.setEnabled(true);
    config.setMinDelayMs(100);
    config.setMaxDelayMs(200);
    config.setJitter(false);
    config.setProbability(0.3);

    assertTrue(config.isEnabled());
    assertEquals(Integer.valueOf(100), config.getMinDelayMs());
    assertEquals(Integer.valueOf(200), config.getMaxDelayMs());
    assertFalse(config.isJitter());
    assertEquals(Double.valueOf(0.3), config.getProbability());
  }

  // ==================== ConnectionDropConfig Tests ====================

  @Test
  void testConnectionDropConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "probability": 0.3,
          "graceful": false
        }
        """;
    ConnectionDropConfig config = GSON.fromJson(json, ConnectionDropConfig.class);

    assertTrue(config.isEnabled());
    assertEquals(Double.valueOf(0.3), config.getProbability());
    assertFalse(config.isGraceful());
  }

  @Test
  void testConnectionDropConfigBuilder() {
    ConnectionDropConfig config =
        ConnectionDropConfig.builder().enabled(true).probability(0.5).graceful(true).build();

    assertTrue(config.isEnabled());
    assertEquals(Double.valueOf(0.5), config.getProbability());
    assertTrue(config.isGraceful());
  }

  @Test
  void testConnectionDropConfigGracefulDefaultTrue() {
    ConnectionDropConfig config = new ConnectionDropConfig(true);

    assertTrue(config.isGraceful());
  }

  // ==================== RandomErrorConfig Tests ====================

  @Test
  void testRandomErrorConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "errorRate": 0.2,
          "errorTypes": ["temporary", "permanent"]
        }
        """;
    RandomErrorConfig config = GSON.fromJson(json, RandomErrorConfig.class);

    assertTrue(config.isEnabled());
    assertEquals(Double.valueOf(0.2), config.getErrorRate());
    assertEquals(
        List.of(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT), config.getErrorTypes());
  }

  @Test
  void testRandomErrorConfigBuilder() {
    RandomErrorConfig config =
        RandomErrorConfig.builder()
            .enabled(true)
            .errorRate(0.1)
            .errorTypes(ChaosErrorType.TEMPORARY)
            .build();

    assertTrue(config.isEnabled());
    assertEquals(Double.valueOf(0.1), config.getErrorRate());
    assertEquals(List.of(ChaosErrorType.TEMPORARY), config.getErrorTypes());
  }

  @Test
  void testRandomErrorConfigBuilderWithList() {
    RandomErrorConfig config =
        RandomErrorConfig.builder()
            .enabled(true)
            .errorTypes(List.of(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT))
            .build();

    assertEquals(
        List.of(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT), config.getErrorTypes());
  }

  @Test
  void testChaosErrorTypeEnum() {
    String json = "{\"errorTypes\": [\"temporary\"]}";
    RandomErrorConfig config = GSON.fromJson(json, RandomErrorConfig.class);
    assertEquals(List.of(ChaosErrorType.TEMPORARY), config.getErrorTypes());

    json = "{\"errorTypes\": [\"permanent\"]}";
    config = GSON.fromJson(json, RandomErrorConfig.class);
    assertEquals(List.of(ChaosErrorType.PERMANENT), config.getErrorTypes());
  }

  // ==================== GreylistConfig Tests ====================

  @Test
  void testGreylistConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "retryWindowMs": 600000,
          "maxAttempts": 3,
          "trackBy": "ip_sender"
        }
        """;
    GreylistConfig config = GSON.fromJson(json, GreylistConfig.class);

    assertTrue(config.isEnabled());
    assertEquals(Integer.valueOf(600000), config.getRetryWindowMs());
    assertEquals(Integer.valueOf(3), config.getMaxAttempts());
    assertEquals(GreylistTrackBy.IP_SENDER, config.getTrackBy());
  }

  @Test
  void testGreylistConfigBuilder() {
    GreylistConfig config =
        GreylistConfig.builder()
            .enabled(true)
            .retryWindowMs(300000)
            .maxAttempts(2)
            .trackBy(GreylistTrackBy.SENDER)
            .build();

    assertTrue(config.isEnabled());
    assertEquals(Integer.valueOf(300000), config.getRetryWindowMs());
    assertEquals(Integer.valueOf(2), config.getMaxAttempts());
    assertEquals(GreylistTrackBy.SENDER, config.getTrackBy());
  }

  @Test
  void testGreylistTrackByEnum() {
    String json = "{\"trackBy\": \"ip\"}";
    GreylistConfig config = GSON.fromJson(json, GreylistConfig.class);
    assertEquals(GreylistTrackBy.IP, config.getTrackBy());

    json = "{\"trackBy\": \"sender\"}";
    config = GSON.fromJson(json, GreylistConfig.class);
    assertEquals(GreylistTrackBy.SENDER, config.getTrackBy());

    json = "{\"trackBy\": \"ip_sender\"}";
    config = GSON.fromJson(json, GreylistConfig.class);
    assertEquals(GreylistTrackBy.IP_SENDER, config.getTrackBy());
  }

  // ==================== BlackholeConfig Tests ====================

  @Test
  void testBlackholeConfigDeserialization() {
    String json =
        """
        {
          "enabled": true,
          "triggerWebhooks": true
        }
        """;
    BlackholeConfig config = GSON.fromJson(json, BlackholeConfig.class);

    assertTrue(config.isEnabled());
    assertTrue(config.isTriggerWebhooks());
  }

  @Test
  void testBlackholeConfigBuilder() {
    BlackholeConfig config = BlackholeConfig.builder().enabled(true).triggerWebhooks(false).build();

    assertTrue(config.isEnabled());
    assertFalse(config.isTriggerWebhooks());
  }

  @Test
  void testBlackholeConfigTriggerWebhooksDefaultFalse() {
    BlackholeConfig config = new BlackholeConfig(true);

    assertFalse(config.isTriggerWebhooks());
  }

  @Test
  void testBlackholeConfigSetters() {
    BlackholeConfig config = new BlackholeConfig();
    config.setEnabled(true);
    config.setTriggerWebhooks(true);

    assertTrue(config.isEnabled());
    assertTrue(config.isTriggerWebhooks());
  }

  // ==================== Serialization Tests ====================

  @Test
  void testChaosConfigSerialization() {
    ChaosConfig config =
        ChaosConfig.builder()
            .enabled(true)
            .expiresAt("2024-12-31T23:59:59.000Z")
            .latency(
                LatencyConfig.builder()
                    .minDelayMs(1000)
                    .maxDelayMs(5000)
                    .jitter(true)
                    .probability(0.5)
                    .build())
            .build();

    String json = GSON.toJson(config);
    ChaosConfig deserialized = GSON.fromJson(json, ChaosConfig.class);

    assertTrue(deserialized.isEnabled());
    assertEquals("2024-12-31T23:59:59.000Z", deserialized.getExpiresAt());
    assertNotNull(deserialized.getLatency());
    assertEquals(Integer.valueOf(1000), deserialized.getLatency().getMinDelayMs());
    assertEquals(Integer.valueOf(5000), deserialized.getLatency().getMaxDelayMs());
  }

  @Test
  void testCombinedChaosScenario() {
    ChaosConfig config =
        ChaosConfig.builder()
            .enabled(true)
            .expiresAt("2024-12-31T23:59:59.000Z")
            .latency(
                LatencyConfig.builder().minDelayMs(500).maxDelayMs(3000).probability(0.8).build())
            .randomError(
                RandomErrorConfig.builder()
                    .errorRate(0.1)
                    .errorTypes(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT)
                    .build())
            .build();

    String json = GSON.toJson(config);
    ChaosConfig deserialized = GSON.fromJson(json, ChaosConfig.class);

    assertTrue(deserialized.isEnabled());
    assertNotNull(deserialized.getLatency());
    assertNotNull(deserialized.getRandomError());
    assertEquals(
        List.of(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT),
        deserialized.getRandomError().getErrorTypes());
  }
}
