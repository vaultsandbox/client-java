package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.CreateInboxOptions;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.model.BlackholeConfig;
import com.vaultsandbox.client.model.ChaosConfig;
import com.vaultsandbox.client.model.ChaosErrorType;
import com.vaultsandbox.client.model.ConnectionDropConfig;
import com.vaultsandbox.client.model.GreylistConfig;
import com.vaultsandbox.client.model.GreylistTrackBy;
import com.vaultsandbox.client.model.LatencyConfig;
import com.vaultsandbox.client.model.RandomErrorConfig;
import com.vaultsandbox.client.model.ServerInfo;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Integration tests for chaos configuration on inboxes. */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChaosIT {

  private static VaultSandboxClient client;
  private static boolean chaosEnabled;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    client =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());

    // Check if chaos is enabled on the server
    ServerInfo serverInfo = client.getServerInfo();
    chaosEnabled = serverInfo.isChaosEnabled();
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  private void assumeChaosEnabled() {
    assumeTrue(chaosEnabled, "Chaos must be enabled on server for these tests");
  }

  // ==================== Get Chaos Config Tests ====================

  @Test
  @Order(1)
  void testGetChaosConfigDefault() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config = inbox.getChaos();

      assertNotNull(config);
      // Default should be disabled
      assertFalse(config.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Set Chaos Config Tests ====================

  @Test
  @Order(10)
  void testSetChaosConfigEnabled() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config = ChaosConfig.builder().enabled(true).build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertTrue(result.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(11)
  void testSetChaosConfigDisabled() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      // First enable
      inbox.setChaos(ChaosConfig.builder().enabled(true).build());

      // Then disable
      ChaosConfig result = inbox.setChaos(ChaosConfig.disabled());

      assertNotNull(result);
      assertFalse(result.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(12)
  void testSetChaosConfigWithExpiration() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder().enabled(true).expiresAt("2099-12-31T23:59:59.000Z").build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertTrue(result.isEnabled());
      assertEquals("2099-12-31T23:59:59.000Z", result.getExpiresAt());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Latency Config Tests ====================

  @Test
  @Order(20)
  void testSetLatencyConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .latency(
                  LatencyConfig.builder()
                      .enabled(true)
                      .minDelayMs(1000)
                      .maxDelayMs(5000)
                      .jitter(true)
                      .probability(0.5)
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertTrue(result.isEnabled());
      assertNotNull(result.getLatency());
      assertTrue(result.getLatency().isEnabled());
      assertEquals(Integer.valueOf(1000), result.getLatency().getMinDelayMs());
      assertEquals(Integer.valueOf(5000), result.getLatency().getMaxDelayMs());
      assertTrue(result.getLatency().isJitter());
      assertEquals(Double.valueOf(0.5), result.getLatency().getProbability());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  void testSetLatencyConfigMinimal() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .latency(LatencyConfig.builder().enabled(true).build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getLatency());
      assertTrue(result.getLatency().isEnabled());
      // Server should apply defaults
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Connection Drop Config Tests ====================

  @Test
  @Order(30)
  void testSetConnectionDropConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .connectionDrop(
                  ConnectionDropConfig.builder()
                      .enabled(true)
                      .probability(0.3)
                      .graceful(false)
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getConnectionDrop());
      assertTrue(result.getConnectionDrop().isEnabled());
      assertEquals(Double.valueOf(0.3), result.getConnectionDrop().getProbability());
      assertFalse(result.getConnectionDrop().isGraceful());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Random Error Config Tests ====================

  @Test
  @Order(40)
  void testSetRandomErrorConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .randomError(
                  RandomErrorConfig.builder()
                      .enabled(true)
                      .errorRate(0.2)
                      .errorTypes(ChaosErrorType.TEMPORARY)
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getRandomError());
      assertTrue(result.getRandomError().isEnabled());
      assertEquals(Double.valueOf(0.2), result.getRandomError().getErrorRate());
      assertTrue(result.getRandomError().getErrorTypes().contains(ChaosErrorType.TEMPORARY));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  void testSetRandomErrorConfigMultipleTypes() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .randomError(
                  RandomErrorConfig.builder()
                      .enabled(true)
                      .errorRate(0.1)
                      .errorTypes(List.of(ChaosErrorType.TEMPORARY, ChaosErrorType.PERMANENT))
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getRandomError());
      assertEquals(2, result.getRandomError().getErrorTypes().size());
      assertTrue(result.getRandomError().getErrorTypes().contains(ChaosErrorType.TEMPORARY));
      assertTrue(result.getRandomError().getErrorTypes().contains(ChaosErrorType.PERMANENT));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Greylist Config Tests ====================

  @Test
  @Order(50)
  void testSetGreylistConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .greylist(
                  GreylistConfig.builder()
                      .enabled(true)
                      .retryWindowMs(600000)
                      .maxAttempts(3)
                      .trackBy(GreylistTrackBy.IP_SENDER)
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getGreylist());
      assertTrue(result.getGreylist().isEnabled());
      assertEquals(Integer.valueOf(600000), result.getGreylist().getRetryWindowMs());
      assertEquals(Integer.valueOf(3), result.getGreylist().getMaxAttempts());
      assertEquals(GreylistTrackBy.IP_SENDER, result.getGreylist().getTrackBy());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(51)
  void testSetGreylistConfigTrackByIp() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .greylist(GreylistConfig.builder().enabled(true).trackBy(GreylistTrackBy.IP).build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getGreylist());
      assertEquals(GreylistTrackBy.IP, result.getGreylist().getTrackBy());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(52)
  void testSetGreylistConfigTrackBySender() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .greylist(
                  GreylistConfig.builder().enabled(true).trackBy(GreylistTrackBy.SENDER).build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getGreylist());
      assertEquals(GreylistTrackBy.SENDER, result.getGreylist().getTrackBy());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Blackhole Config Tests ====================

  @Test
  @Order(60)
  void testSetBlackholeConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .blackhole(BlackholeConfig.builder().enabled(true).triggerWebhooks(false).build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getBlackhole());
      assertTrue(result.getBlackhole().isEnabled());
      assertFalse(result.getBlackhole().isTriggerWebhooks());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(61)
  void testSetBlackholeConfigWithWebhooks() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .blackhole(BlackholeConfig.builder().enabled(true).triggerWebhooks(true).build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertNotNull(result.getBlackhole());
      assertTrue(result.getBlackhole().isEnabled());
      assertTrue(result.getBlackhole().isTriggerWebhooks());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Combined Chaos Config Tests ====================

  @Test
  @Order(70)
  void testSetCombinedChaosConfig() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      ChaosConfig config =
          ChaosConfig.builder()
              .enabled(true)
              .expiresAt("2099-12-31T23:59:59.000Z")
              .latency(
                  LatencyConfig.builder()
                      .enabled(true)
                      .minDelayMs(500)
                      .maxDelayMs(3000)
                      .probability(0.8)
                      .build())
              .randomError(
                  RandomErrorConfig.builder()
                      .enabled(true)
                      .errorRate(0.1)
                      .errorTypes(ChaosErrorType.TEMPORARY)
                      .build())
              .build();

      ChaosConfig result = inbox.setChaos(config);

      assertNotNull(result);
      assertTrue(result.isEnabled());
      assertEquals("2099-12-31T23:59:59.000Z", result.getExpiresAt());
      assertNotNull(result.getLatency());
      assertNotNull(result.getRandomError());
      assertTrue(result.getLatency().isEnabled());
      assertTrue(result.getRandomError().isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Delete/Disable Chaos Tests ====================

  @Test
  @Order(80)
  void testDisableChaos() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      // Enable chaos first
      inbox.setChaos(
          ChaosConfig.builder()
              .enabled(true)
              .latency(LatencyConfig.builder().enabled(true).build())
              .build());

      // Disable chaos
      inbox.disableChaos();

      // Verify disabled
      ChaosConfig result = inbox.getChaos();
      assertFalse(result.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(81)
  void testDisableChaosIdempotent() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      // Disable chaos when already disabled should succeed
      inbox.disableChaos();
      inbox.disableChaos();
      // No exception means idempotent disable succeeded
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Create Inbox with Chaos Tests ====================

  @Test
  @Order(90)
  void testCreateInboxWithChaos() {
    assumeChaosEnabled();

    ChaosConfig chaos =
        ChaosConfig.builder()
            .enabled(true)
            .latency(
                LatencyConfig.builder().enabled(true).minDelayMs(1000).maxDelayMs(5000).build())
            .build();

    Inbox inbox = client.createInbox(CreateInboxOptions.builder().chaos(chaos).build());

    try {
      // Verify chaos was applied
      ChaosConfig result = inbox.getChaos();
      assertNotNull(result);
      assertTrue(result.isEnabled());
      assertNotNull(result.getLatency());
      assertTrue(result.getLatency().isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(91)
  void testCreateInboxWithChaosDisabled() {
    assumeChaosEnabled();

    ChaosConfig chaos = ChaosConfig.disabled();

    Inbox inbox = client.createInbox(CreateInboxOptions.builder().chaos(chaos).build());

    try {
      ChaosConfig result = inbox.getChaos();
      assertNotNull(result);
      assertFalse(result.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Chaos Lifecycle Test ====================

  @Test
  @Order(100)
  void testChaosLifecycle() {
    assumeChaosEnabled();
    Inbox inbox = client.createInbox();

    try {
      // Initial state - chaos disabled
      ChaosConfig initial = inbox.getChaos();
      assertFalse(initial.isEnabled());

      // Enable latency
      inbox.setChaos(
          ChaosConfig.builder()
              .enabled(true)
              .latency(
                  LatencyConfig.builder().enabled(true).minDelayMs(100).maxDelayMs(500).build())
              .build());

      ChaosConfig withLatency = inbox.getChaos();
      assertTrue(withLatency.isEnabled());
      assertNotNull(withLatency.getLatency());

      // Update to add random errors
      inbox.setChaos(
          ChaosConfig.builder()
              .enabled(true)
              .latency(
                  LatencyConfig.builder().enabled(true).minDelayMs(100).maxDelayMs(500).build())
              .randomError(
                  RandomErrorConfig.builder()
                      .enabled(true)
                      .errorRate(0.1)
                      .errorTypes(ChaosErrorType.TEMPORARY)
                      .build())
              .build());

      ChaosConfig withBoth = inbox.getChaos();
      assertTrue(withBoth.isEnabled());
      assertNotNull(withBoth.getLatency());
      assertNotNull(withBoth.getRandomError());

      // Disable chaos
      inbox.disableChaos();

      ChaosConfig disabled = inbox.getChaos();
      assertFalse(disabled.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Multiple Inboxes with Different Chaos ====================

  @Test
  @Order(110)
  void testMultipleInboxesWithDifferentChaos() {
    assumeChaosEnabled();

    Inbox inbox1 = client.createInbox();
    Inbox inbox2 = client.createInbox();

    try {
      // Configure different chaos for each inbox
      inbox1.setChaos(
          ChaosConfig.builder()
              .enabled(true)
              .latency(LatencyConfig.builder().enabled(true).minDelayMs(1000).build())
              .build());

      inbox2.setChaos(
          ChaosConfig.builder()
              .enabled(true)
              .blackhole(BlackholeConfig.builder().enabled(true).build())
              .build());

      // Verify each inbox has its own config
      ChaosConfig config1 = inbox1.getChaos();
      ChaosConfig config2 = inbox2.getChaos();

      assertTrue(config1.isEnabled());
      assertNotNull(config1.getLatency());
      assertNull(config1.getBlackhole());

      assertTrue(config2.isEnabled());
      assertNull(config2.getLatency());
      assertNotNull(config2.getBlackhole());
    } finally {
      client.deleteInbox(inbox1.getEmailAddress());
      client.deleteInbox(inbox2.getEmailAddress());
    }
  }
}
