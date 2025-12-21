package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.exception.InboxAlreadyExistsException;
import com.vaultsandbox.client.exception.InvalidImportDataException;
import com.vaultsandbox.client.model.ExportedInbox;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Integration tests for inbox export/import functionality. */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExportImportIT {

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
      try {
        client.deleteAllInboxes();
      } catch (Exception ignored) {
      }
      client.close();
    }
  }

  @Test
  @Order(1)
  void testExportInbox() {
    Inbox inbox = client.createInbox();

    ExportedInbox exported = client.exportInbox(inbox);

    assertNotNull(exported);
    assertEquals(inbox.getEmailAddress(), exported.getEmailAddress());
    assertEquals(inbox.getExpiresAt().toString(), exported.getExpiresAt());
    assertEquals(inbox.getHash(), exported.getInboxHash());
    assertEquals(inbox.getServerSigPk(), exported.getServerSigPk());
    assertNotNull(exported.getPublicKeyB64());
    assertNotNull(exported.getSecretKeyB64());
    assertNotNull(exported.getExportedAt());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(2)
  void testExportFromInbox() {
    Inbox inbox = client.createInbox();

    ExportedInbox exported = inbox.export();

    assertNotNull(exported);
    assertEquals(inbox.getEmailAddress(), exported.getEmailAddress());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(3)
  void testExportImportRoundtrip() {
    // Create original inbox
    Inbox original = client.createInbox();
    String email = original.getEmailAddress();
    Instant expiresAt = original.getExpiresAt();
    String hash = original.getHash();

    // Export it
    ExportedInbox exported = client.exportInbox(original);

    // Remove from local tracking (simulate losing local state)
    // We need to create a new client to forget the inbox
    VaultSandboxClient newClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());

    try {
      // Import into new client
      Inbox restored = newClient.importInbox(exported);

      assertEquals(email, restored.getEmailAddress());
      assertEquals(expiresAt, restored.getExpiresAt());
      assertEquals(hash, restored.getHash());

      // Verify we can still use it
      var syncStatus = restored.getSyncStatus();
      assertNotNull(syncStatus);
    } finally {
      newClient.close();
    }

    // Clean up
    client.deleteInbox(email);
  }

  @Test
  @Order(4)
  void testExportImportToFile() throws IOException {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    Path tempFile = Files.createTempFile("inbox-export-", ".json");

    try {
      // Export to file
      client.exportInboxToFile(inbox, tempFile);

      // Verify file exists and has content
      assertTrue(Files.exists(tempFile));
      String content = Files.readString(tempFile);
      assertTrue(content.contains(email));

      // Create new client for import
      VaultSandboxClient newClient =
          VaultSandboxClient.create(
              ClientConfig.builder()
                  .apiKey(TestConfig.getApiKey())
                  .baseUrl(TestConfig.getBaseUrl())
                  .build());

      try {
        // Import from file
        Inbox restored = newClient.importInboxFromFile(tempFile);

        assertEquals(email, restored.getEmailAddress());
      } finally {
        newClient.close();
      }
    } finally {
      Files.deleteIfExists(tempFile);
      client.deleteInbox(email);
    }
  }

  @Test
  @Order(5)
  void testImportAlreadyExistsLocally() {
    Inbox inbox = client.createInbox();
    ExportedInbox exported = client.exportInbox(inbox);

    // Try to import when it already exists locally
    assertThrows(InboxAlreadyExistsException.class, () -> client.importInbox(exported));

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(6)
  void testImportDeletedInbox() {
    Inbox inbox = client.createInbox();
    ExportedInbox exported = client.exportInbox(inbox);

    // Delete from server
    client.deleteInbox(inbox.getEmailAddress());

    // Create new client for import
    VaultSandboxClient newClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());

    try {
      // Import should fail because inbox no longer exists on server
      assertThrows(InvalidImportDataException.class, () -> newClient.importInbox(exported));
    } finally {
      newClient.close();
    }
  }

  @Test
  @Order(7)
  void testImportWithInvalidData() {
    ExportedInbox invalid = new ExportedInbox();
    invalid.setEmailAddress(null); // Required field missing

    assertThrows(InvalidImportDataException.class, () -> client.importInbox(invalid));
  }

  @Test
  @Order(8)
  void testImportWithExpiredInbox() {
    ExportedInbox expired = new ExportedInbox();
    expired.setEmailAddress("test@example.com");
    expired.setExpiresAt(Instant.now().minus(Duration.ofHours(1)).toString());
    expired.setInboxHash("hash");
    expired.setServerSigPk("key");
    expired.setPublicKeyB64("pk");
    expired.setSecretKeyB64("sk");

    assertThrows(InvalidImportDataException.class, () -> client.importInbox(expired));
  }

  @Test
  @Order(9)
  void testExportedInboxValidation() {
    Inbox inbox = client.createInbox();
    ExportedInbox exported = client.exportInbox(inbox);

    // Should not throw
    assertDoesNotThrow(exported::validate);

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  // ==================== Export by Email Address Tests ====================

  @Test
  @Order(10)
  void testExportByEmailAddress() {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    try {
      ExportedInbox exported = client.exportInbox(email);

      assertNotNull(exported);
      assertEquals(email, exported.getEmailAddress());
      assertNotNull(exported.getPublicKeyB64());
      assertNotNull(exported.getSecretKeyB64());
    } finally {
      client.deleteInbox(email);
    }
  }

  @Test
  @Order(11)
  void testExportNotFoundError() {
    assertThrows(
        com.vaultsandbox.client.exception.InboxNotFoundException.class,
        () -> client.exportInbox("nonexistent-inbox-12345@vaultsandbox.com"),
        "Should throw InboxNotFoundException for non-existent inbox");
  }

  // ==================== Import with Modified Data Tests ====================

  @Test
  @Order(12)
  void testImportWithEmptyServerSigPk() {
    Inbox inbox = client.createInbox();
    ExportedInbox exported = client.exportInbox(inbox);

    // Set server signature to empty (validation should fail)
    String originalServerSigPk = exported.getServerSigPk();
    exported.setServerSigPk("");

    try {
      // Create a new client for import
      VaultSandboxClient newClient =
          VaultSandboxClient.create(
              ClientConfig.builder()
                  .apiKey(TestConfig.getApiKey())
                  .baseUrl(TestConfig.getBaseUrl())
                  .build());

      try {
        // Import should fail due to empty server signature
        assertThrows(
            InvalidImportDataException.class,
            () -> newClient.importInbox(exported),
            "Should throw when server signature is empty");
      } finally {
        newClient.close();
      }
    } finally {
      // Restore and clean up
      exported.setServerSigPk(originalServerSigPk);
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== File Operation Edge Cases ====================

  @Test
  @Order(20)
  void testImportInvalidJsonFile() throws IOException {
    Path tempFile = Files.createTempFile("invalid-json-", ".json");

    try {
      // Write invalid JSON content
      Files.writeString(tempFile, "{ this is not valid json {{{");

      assertThrows(
          Exception.class,
          () -> client.importInboxFromFile(tempFile),
          "Should throw when importing invalid JSON file");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @Order(21)
  void testImportNonExistentFile() {
    Path nonExistent = Path.of("/tmp/does-not-exist-vaultsandbox-12345.json");

    assertThrows(
        Exception.class,
        () -> client.importInboxFromFile(nonExistent),
        "Should throw when importing from non-existent file");
  }

  @Test
  @Order(22)
  void testExportedFileContainsValidJson() throws IOException {
    Inbox inbox = client.createInbox();
    Path tempFile = Files.createTempFile("export-json-", ".json");

    try {
      client.exportInboxToFile(inbox, tempFile);
      String content = Files.readString(tempFile);

      // Verify file is not empty
      assertFalse(content.isEmpty(), "Exported file should not be empty");

      // Verify it's valid JSON by checking basic structure
      assertTrue(content.startsWith("{"), "Should be a JSON object");
      assertTrue(content.endsWith("}"), "Should end with closing brace");
      assertTrue(content.contains("\"emailAddress\""), "Should contain emailAddress field");
      assertTrue(content.contains("\"expiresAt\""), "Should contain expiresAt field");
      assertTrue(content.contains("\"publicKeyB64\""), "Should contain publicKeyB64 field");
      assertTrue(content.contains("\"secretKeyB64\""), "Should contain secretKeyB64 field");
      assertTrue(content.contains(inbox.getEmailAddress()), "Should contain actual email address");
    } finally {
      Files.deleteIfExists(tempFile);
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(23)
  void testImportEmptyFile() throws IOException {
    Path tempFile = Files.createTempFile("empty-", ".json");

    try {
      // Write empty content
      Files.writeString(tempFile, "");

      assertThrows(
          Exception.class,
          () -> client.importInboxFromFile(tempFile),
          "Should throw when importing empty file");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @Order(24)
  void testImportValidJsonButInvalidStructure() throws IOException {
    Path tempFile = Files.createTempFile("invalid-structure-", ".json");

    try {
      // Valid JSON but not an ExportedInbox structure
      Files.writeString(tempFile, "{\"foo\": \"bar\", \"baz\": 123}");

      assertThrows(
          InvalidImportDataException.class,
          () -> client.importInboxFromFile(tempFile),
          "Should throw InvalidImportDataException for invalid structure");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }
}
