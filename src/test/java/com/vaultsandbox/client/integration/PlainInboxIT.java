package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.CreateInboxOptions;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.SmtpTestHelper;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.model.EmailMetadata;
import com.vaultsandbox.client.model.ExportedInbox;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for plain (unencrypted) inboxes.
 *
 * <p>These tests verify that plain inboxes work correctly without encryption, covering the Base64
 * decoding code paths instead of the cryptographic decryption paths.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlainInboxIT {

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

  // ==================== Basic Plain Inbox Creation Tests ====================

  @Test
  @Order(1)
  void testCreatePlainInboxWithPlainMethod() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();

    Inbox inbox = client.createInbox(options);

    try {
      assertNotNull(inbox);
      assertNotNull(inbox.getEmailAddress());
      assertTrue(inbox.getEmailAddress().contains("@"));
      assertFalse(inbox.isEncrypted(), "Plain inbox should not be encrypted");
      assertNull(inbox.getServerSigPk(), "Plain inbox should not have server signature key");
      assertNotNull(inbox.getHash());
      assertNotNull(inbox.getExpiresAt());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(2)
  void testCreatePlainInboxWithEncryptionString() {
    CreateInboxOptions options = CreateInboxOptions.builder().encryption("plain").build();

    Inbox inbox = client.createInbox(options);

    try {
      assertNotNull(inbox);
      assertFalse(inbox.isEncrypted(), "Plain inbox should not be encrypted");
      assertNull(inbox.getServerSigPk(), "Plain inbox should not have server signature key");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(3)
  void testPlainInboxListEmailsEmpty() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    try {
      List<Email> emails = inbox.listEmails();
      assertNotNull(emails);
      assertTrue(emails.isEmpty());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(4)
  void testPlainInboxSyncStatus() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    try {
      var status = inbox.getSyncStatus();
      assertNotNull(status);
      assertEquals(0, status.getEmailCount());
      assertNotNull(status.getEmailsHash());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Plain Inbox Email Reception Tests ====================

  @Test
  @Order(10)
  void testPlainInboxReceiveEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Plain Test " + uniqueId;
    String body = "This is a plain inbox test email body";

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "plain@test.com", subject, body);

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email, "Email should be received in plain inbox");
      assertEquals(subject, email.getSubject());
      assertEquals("plain@test.com", email.getFrom());
      assertTrue(email.getText().contains(body));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(11)
  void testPlainInboxListEmails() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Plain List Test " + uniqueId;
    String textBody = "Plain text body " + uniqueId;
    String htmlBody = "<html><body><p>HTML body " + uniqueId + "</p></body></html>";

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendHtmlEmail(
          inbox.getEmailAddress(), "plainlist@test.com", subject, textBody, htmlBody);

      Email awaited =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(awaited, "Email should be received");

      // Test listEmails returns full content for plain inbox
      List<Email> emails = inbox.listEmails();
      assertFalse(emails.isEmpty(), "Should have at least one email");

      Email listed =
          emails.stream().filter(e -> e.getSubject().contains(uniqueId)).findFirst().orElse(null);
      assertNotNull(listed, "Our email should be in the list");
      assertNotNull(listed.getText(), "Text should be present");
      assertNotNull(listed.getHtml(), "HTML should be present");
      assertTrue(listed.getText().contains(uniqueId));
      assertTrue(listed.getHtml().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(12)
  void testPlainInboxListEmailsMetadataOnly() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Plain Metadata Test " + uniqueId;

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "plainmeta@test.com", subject, "Body");

      Email awaited =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(awaited);

      // Test metadata-only listing
      List<EmailMetadata> metadataList = inbox.listEmailsMetadataOnly();
      assertFalse(metadataList.isEmpty());

      EmailMetadata metadata =
          metadataList.stream()
              .filter(m -> m.getSubject().contains(uniqueId))
              .findFirst()
              .orElse(null);
      assertNotNull(metadata);
      assertEquals("plainmeta@test.com", metadata.getFrom());
      assertTrue(metadata.getSubject().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(13)
  void testPlainInboxGetEmailById() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "plainbyid@test.com", "Plain By ID " + uniqueId, "Body content");

      Email awaited =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(awaited);

      // Get email by ID
      Email retrieved = inbox.getEmail(awaited.getId());
      assertNotNull(retrieved);
      assertEquals(awaited.getId(), retrieved.getId());
      assertEquals(awaited.getSubject(), retrieved.getSubject());
      assertEquals(awaited.getFrom(), retrieved.getFrom());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Plain Inbox Raw Email Tests ====================

  @Test
  @Order(20)
  void testPlainInboxGetRawEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Plain Raw " + uniqueId;

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "plainraw@test.com", subject, "Raw body content");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(email);

      // Get raw email - this tests the Base64 decoding path for plain inboxes
      String raw = inbox.getRawEmail(email.getId());

      assertNotNull(raw, "Raw email should not be null");
      assertFalse(raw.isEmpty(), "Raw email should not be empty");
      assertTrue(raw.contains("Subject:"), "Raw email should contain Subject header");
      assertTrue(raw.contains(uniqueId), "Raw email should contain subject content");
      assertTrue(raw.contains("From:"), "Raw email should contain From header");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  void testPlainInboxGetRawViaEmailObject() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "plainrawobj@test.com", "Plain Raw Obj " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(email);

      // Get raw via email.getRaw() method - tests the plain inbox code path
      String raw = email.getRaw();

      assertNotNull(raw, "Raw email should not be null");
      assertFalse(raw.isEmpty(), "Raw email should not be empty");
      assertTrue(raw.contains("Subject:"), "Raw email should contain Subject header");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Plain Inbox Export/Import Tests ====================

  @Test
  @Order(30)
  void testPlainInboxExport() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    try {
      ExportedInbox exported = client.exportInbox(inbox);

      assertNotNull(exported);
      assertEquals(1, exported.getVersion());
      assertEquals(inbox.getEmailAddress(), exported.getEmailAddress());
      assertEquals(inbox.getHash(), exported.getInboxHash());
      assertFalse(exported.isEncrypted(), "Exported plain inbox should have encrypted=false");
      // Plain inboxes should NOT have cryptographic keys
      assertNull(exported.getServerSigPk(), "Plain inbox export should not have serverSigPk");
      assertNull(exported.getSecretKey(), "Plain inbox export should not have secretKey");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(31)
  void testPlainInboxExportFromInboxMethod() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    try {
      ExportedInbox exported = inbox.export();

      assertNotNull(exported);
      assertEquals(inbox.getEmailAddress(), exported.getEmailAddress());
      assertFalse(exported.isEncrypted());
      assertNull(exported.getServerSigPk());
      assertNull(exported.getSecretKey());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(32)
  void testPlainInboxExportImportRoundtrip() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox original = client.createInbox(options);
    String email = original.getEmailAddress();

    try {
      ExportedInbox exported = client.exportInbox(original);

      // Create a new client to simulate importing in a fresh context
      VaultSandboxClient newClient =
          VaultSandboxClient.create(
              ClientConfig.builder()
                  .apiKey(TestConfig.getApiKey())
                  .baseUrl(TestConfig.getBaseUrl())
                  .build());

      try {
        Inbox restored = newClient.importInbox(exported);

        assertEquals(email, restored.getEmailAddress());
        assertEquals(original.getExpiresAt(), restored.getExpiresAt());
        assertEquals(original.getHash(), restored.getHash());
        assertFalse(restored.isEncrypted(), "Restored inbox should be plain");
        assertNull(restored.getServerSigPk());

        // Verify we can still use it
        var syncStatus = restored.getSyncStatus();
        assertNotNull(syncStatus);
      } finally {
        newClient.close();
      }
    } finally {
      client.deleteInbox(email);
    }
  }

  @Test
  @Order(33)
  void testPlainInboxExportImportToFile() throws IOException {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    String email = inbox.getEmailAddress();

    Path tempFile = Files.createTempFile("plain-inbox-export-", ".json");

    try {
      client.exportInboxToFile(inbox, tempFile);

      assertTrue(Files.exists(tempFile));
      String content = Files.readString(tempFile);
      assertTrue(content.contains(email));
      assertTrue(content.contains("\"encrypted\":false"));
      // Plain inbox export should NOT contain secretKey
      assertFalse(content.contains("\"secretKey\""), "Plain inbox should not have secretKey");

      // Import into new client
      VaultSandboxClient newClient =
          VaultSandboxClient.create(
              ClientConfig.builder()
                  .apiKey(TestConfig.getApiKey())
                  .baseUrl(TestConfig.getBaseUrl())
                  .build());

      try {
        Inbox restored = newClient.importInboxFromFile(tempFile);
        assertEquals(email, restored.getEmailAddress());
        assertFalse(restored.isEncrypted());
      } finally {
        newClient.close();
      }
    } finally {
      Files.deleteIfExists(tempFile);
      client.deleteInbox(email);
    }
  }

  @Test
  @Order(34)
  void testPlainInboxExportByEmailAddress() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    String email = inbox.getEmailAddress();

    try {
      ExportedInbox exported = client.exportInbox(email);

      assertNotNull(exported);
      assertEquals(email, exported.getEmailAddress());
      assertFalse(exported.isEncrypted());
      assertNull(exported.getSecretKey());
    } finally {
      client.deleteInbox(email);
    }
  }

  // ==================== Plain Inbox Email Operations Tests ====================

  @Test
  @Order(40)
  void testPlainInboxMarkEmailAsRead() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "plainread@test.com", "Plain Read " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(email);
      assertFalse(email.isRead());

      // Mark as read
      email.markAsRead();

      // Verify
      Email updated = inbox.getEmail(email.getId());
      assertTrue(updated.isRead());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  void testPlainInboxDeleteEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "plaindel@test.com", "Plain Del " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      assertNotNull(email);
      String emailId = email.getId();

      // Delete
      email.delete();

      // Verify deletion
      try {
        inbox.getEmail(emailId);
        assertTrue(false, "Should have thrown EmailNotFoundException");
      } catch (Exception e) {
        assertTrue(e.getClass().getSimpleName().contains("EmailNotFoundException"));
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Plain vs Encrypted Comparison Tests ====================

  @Test
  @Order(50)
  void testPlainVsEncryptedInboxComparison() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    // Create one plain and one encrypted inbox
    CreateInboxOptions plainOptions = CreateInboxOptions.builder().plain().build();
    CreateInboxOptions encryptedOptions = CreateInboxOptions.builder().encrypted().build();

    Inbox plainInbox = client.createInbox(plainOptions);
    Inbox encryptedInbox = client.createInbox(encryptedOptions);

    SmtpTestHelper smtp = new SmtpTestHelper();
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Compare Test " + uniqueId;
    String body = "Comparison body content";

    var plainSub = plainInbox.onNewEmail(e -> {});
    var encryptedSub = encryptedInbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send same email to both inboxes
      smtp.sendEmail(plainInbox.getEmailAddress(), "compare@test.com", subject, body);
      smtp.sendEmail(encryptedInbox.getEmailAddress(), "compare@test.com", subject, body);

      // Receive from both
      Email plainEmail =
          plainInbox.awaitEmail(
              EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));
      Email encryptedEmail =
          encryptedInbox.awaitEmail(
              EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(plainEmail);
      assertNotNull(encryptedEmail);

      // Both should have the same content
      assertEquals(subject, plainEmail.getSubject());
      assertEquals(subject, encryptedEmail.getSubject());
      assertEquals("compare@test.com", plainEmail.getFrom());
      assertEquals("compare@test.com", encryptedEmail.getFrom());

      // Verify encryption states
      assertFalse(plainInbox.isEncrypted());
      assertTrue(encryptedInbox.isEncrypted());
      assertNull(plainInbox.getServerSigPk());
      assertNotNull(encryptedInbox.getServerSigPk());
    } finally {
      plainSub.unsubscribe();
      encryptedSub.unsubscribe();
      client.deleteInbox(plainInbox.getEmailAddress());
      client.deleteInbox(encryptedInbox.getEmailAddress());
    }
  }

  // ==================== Plain Inbox With Options Tests ====================

  @Test
  @Order(60)
  void testPlainInboxWithCustomTtl() {
    CreateInboxOptions options =
        CreateInboxOptions.builder().plain().ttl(Duration.ofHours(1)).build();

    Inbox inbox = client.createInbox(options);

    try {
      assertNotNull(inbox);
      assertFalse(inbox.isEncrypted());
      // Verify TTL is approximately correct
      long diffSeconds =
          Math.abs(
              inbox.getExpiresAt().getEpochSecond()
                  - java.time.Instant.now().plusSeconds(3600).getEpochSecond());
      assertTrue(diffSeconds < 120, "Expiration should be approximately 1 hour from now");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(61)
  void testPlainInboxWithEmailAuth() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    CreateInboxOptions options = CreateInboxOptions.builder().plain().emailAuth(true).build();

    Inbox inbox = client.createInbox(options);
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "auth@test.com", "Plain Auth " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      // Auth results may be present, depending on server config
      // The key is that the plain inbox path works with emailAuth
      assertNotNull(email.getSubject());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Edge Cases ====================

  @Test
  @Order(70)
  void testPlainInboxDelete() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    // Delete via inbox method
    inbox.delete();

    // Verify it's deleted by calling an API method on the deleted inbox
    // getSyncStatus makes an API call which should fail for deleted inbox
    assertThrows(
        com.vaultsandbox.client.exception.InboxNotFoundException.class, inbox::getSyncStatus);
  }

  @Test
  @Order(71)
  void testImportPlainInboxAlreadyExistsLocally() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);
    ExportedInbox exported = client.exportInbox(inbox);

    try {
      // Try to import when it already exists locally
      assertThrows(
          com.vaultsandbox.client.exception.InboxAlreadyExistsException.class,
          () -> client.importInbox(exported));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(72)
  void testPlainInboxExportValidation() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();
    Inbox inbox = client.createInbox(options);

    try {
      ExportedInbox exported = client.exportInbox(inbox);

      // Validation should not throw for plain inbox
      exported.validate();
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }
}
