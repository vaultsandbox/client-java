package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.CreateInboxOptions;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.exception.InboxNotFoundException;
import com.vaultsandbox.client.exception.NetworkException;
import com.vaultsandbox.client.model.ServerInfo;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Integration tests for inbox creation and management. */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateInboxIT {

  private static VaultSandboxClient client;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    client =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  @Order(1)
  void testGetServerInfo() {
    ServerInfo info = client.getServerInfo();

    assertNotNull(info);
    assertNotNull(info.getServerSigPk());
    assertNotNull(info.getAlgorithms());
    assertEquals("ML-KEM-768", info.getAlgorithms().getKem());
    assertEquals("ML-DSA-65", info.getAlgorithms().getSig());
    assertEquals("AES-256-GCM", info.getAlgorithms().getAead());
    assertEquals("HKDF-SHA-512", info.getAlgorithms().getKdf());
  }

  @Test
  @Order(2)
  void testCheckKey() {
    assertTrue(client.checkKey());
  }

  @Test
  @Order(3)
  void testCreateInbox() {
    Inbox inbox = client.createInbox();

    assertNotNull(inbox);
    assertNotNull(inbox.getEmailAddress());
    assertTrue(inbox.getEmailAddress().contains("@"));
    assertTrue(inbox.getExpiresAt().isAfter(Instant.now()));
    assertNotNull(inbox.getHash());
    assertNotNull(inbox.getServerSigPk());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(4)
  void testInboxExpiration() {
    Inbox inbox = client.createInbox();

    // Verify expiration is in the future
    Instant expiresAt = inbox.getExpiresAt();
    assertTrue(expiresAt.isAfter(Instant.now()));

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(5)
  void testDeleteInbox() {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    // First delete should succeed
    client.deleteInbox(email);

    // Server should return 404 for operations on deleted inbox
    // Use the original inbox reference to call the API directly
    assertThrows(InboxNotFoundException.class, inbox::getSyncStatus);
  }

  @Test
  @Order(6)
  void testDeleteAllInboxes() {
    // Create multiple inboxes
    Inbox inbox1 = client.createInbox();
    Inbox inbox2 = client.createInbox();

    assertNotNull(inbox1);
    assertNotNull(inbox2);

    // Delete all
    int deleted = client.deleteAllInboxes();

    assertTrue(deleted >= 2);

    // Verify they're gone - use original inbox references to call API
    assertThrows(InboxNotFoundException.class, inbox1::getSyncStatus);
    assertThrows(InboxNotFoundException.class, inbox2::getSyncStatus);
  }

  @Test
  @Order(7)
  void testGetInbox() {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    Inbox retrieved = client.getInbox(email);

    assertNotNull(retrieved);
    assertEquals(email, retrieved.getEmailAddress());

    // Clean up
    client.deleteInbox(email);
  }

  @Test
  @Order(8)
  void testMultipleInboxCreation() {
    Inbox inbox1 = client.createInbox();
    Inbox inbox2 = client.createInbox();
    Inbox inbox3 = client.createInbox();

    // All should have unique addresses
    assertNotEquals(inbox1.getEmailAddress(), inbox2.getEmailAddress());
    assertNotEquals(inbox2.getEmailAddress(), inbox3.getEmailAddress());
    assertNotEquals(inbox1.getEmailAddress(), inbox3.getEmailAddress());

    // Clean up
    client.deleteInbox(inbox1.getEmailAddress());
    client.deleteInbox(inbox2.getEmailAddress());
    client.deleteInbox(inbox3.getEmailAddress());
  }

  @Test
  @Order(9)
  void testListEmailsOnEmptyInbox() {
    Inbox inbox = client.createInbox();

    var emails = inbox.listEmails();

    assertNotNull(emails);
    assertTrue(emails.isEmpty());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(10)
  void testSyncStatus() {
    Inbox inbox = client.createInbox();

    var status = inbox.getSyncStatus();

    assertNotNull(status);
    assertNotNull(status.getEmailsHash());
    assertEquals(0, status.getEmailCount());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(11)
  void testInvalidApiKey() {
    VaultSandboxClient invalidClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey("invalid-key-12345")
                .baseUrl(TestConfig.getBaseUrl())
                .build());

    try {
      assertFalse(invalidClient.checkKey(), "Invalid API key should return false");
    } finally {
      invalidClient.close();
    }
  }

  @Test
  @Order(12)
  void testCreateInboxWithCustomTtl() {
    // Create inbox with 1 hour TTL
    CreateInboxOptions options = CreateInboxOptions.builder().ttl(Duration.ofHours(1)).build();

    Inbox inbox = client.createInbox(options);

    assertNotNull(inbox);
    assertNotNull(inbox.getEmailAddress());
    assertTrue(inbox.getEmailAddress().contains("@"));

    // Verify expiration is approximately 1 hour from now (within 2 minutes tolerance)
    Instant expectedExpiry = Instant.now().plusSeconds(3600);
    long diffSeconds =
        Math.abs(inbox.getExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond());
    assertTrue(diffSeconds < 120, "Expiration should be approximately 1 hour from now");

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(13)
  void testNetworkErrorInvalidHost() {
    VaultSandboxClient badClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey("test-key")
                .baseUrl("http://nonexistent.invalid:9999")
                .httpTimeout(Duration.ofSeconds(5))
                .build());

    try {
      assertThrows(NetworkException.class, badClient::getServerInfo);
    } finally {
      badClient.close();
    }
  }
}
