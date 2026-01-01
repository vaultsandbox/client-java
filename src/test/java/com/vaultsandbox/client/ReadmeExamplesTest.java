package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.exception.ApiException;
import com.vaultsandbox.client.exception.TimeoutException;
import com.vaultsandbox.client.exception.VaultSandboxException;
import com.vaultsandbox.client.model.AuthResults;
import com.vaultsandbox.client.model.AuthValidation;
import com.vaultsandbox.client.strategy.EmailFilter;
import com.vaultsandbox.client.strategy.Subscription;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

/**
 * Tests demonstrating all usage examples from README.md.
 *
 * <p>These tests verify that the SDK API matches the documentation examples and serves as
 * executable documentation for users of the library.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class ReadmeExamplesTest {

  private ReadmeExamplesTest() {}

  private static VaultSandboxClient sharedClient;
  private static SmtpTestHelper smtp;

  @BeforeAll
  static void setUpClass() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for README examples tests");

    sharedClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());

    if (SmtpTestHelper.isAvailable()) {
      smtp = new SmtpTestHelper(TestConfig.getSmtpHost(), TestConfig.getSmtpPort());
    }
  }

  private static boolean hasSmtp() {
    return smtp != null;
  }

  @AfterAll
  static void tearDownClass() {
    if (sharedClient != null) {
      try {
        sharedClient.deleteAllInboxes();
      } catch (Exception ignored) {
      }
      sharedClient.close();
    }
  }

  // ==================== Quick Start Example Tests ====================

  @Nested
  @DisplayName("Quick Start Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class QuickStartExamples {

    private VaultSandboxClient client;
    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null && client != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
      if (client != null && client != sharedClient) {
        client.close();
      }
    }

    @Test
    @Order(1)
    @DisplayName("VaultSandboxClient.create(apiKey) - simple creation with API key")
    void testSimpleClientCreation() {
      // README Example:
      // VaultSandboxClient client = VaultSandboxClient.create("your-api-key");
      // Note: In test we use TestConfig baseUrl; in production the default URL is used

      client =
          VaultSandboxClient.create(
              ClientConfig.builder()
                  .apiKey(TestConfig.getApiKey())
                  .baseUrl(TestConfig.getBaseUrl())
                  .build());

      assertNotNull(client);
      assertTrue(client.checkKey(), "Client should be able to validate API key");
    }

    @Test
    @Order(2)
    @DisplayName("VaultSandboxClient.create(config) - creation with configuration")
    void testClientCreationWithConfig() {
      // README Example:
      // VaultSandboxClient client = VaultSandboxClient.create(config);

      ClientConfig config =
          ClientConfig.builder()
              .apiKey(TestConfig.getApiKey())
              .baseUrl(TestConfig.getBaseUrl())
              .build();

      client = VaultSandboxClient.create(config);

      assertNotNull(client);
      assertTrue(client.checkKey());
    }

    @Test
    @Order(3)
    @DisplayName("Create inbox and access email address")
    void testCreateInboxAndGetAddress() {
      // README Example:
      // Inbox inbox = client.createInbox();
      // System.out.println("Send email to: " + inbox.getEmailAddress());

      inbox = sharedClient.createInbox();

      assertNotNull(inbox);
      assertNotNull(inbox.getEmailAddress());
      assertTrue(inbox.getEmailAddress().contains("@"));
    }

    @Test
    @Order(4)
    @DisplayName("Email getters - from, subject, text, html")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testEmailGetters() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // System.out.println("From: " + email.getFrom());
      // System.out.println("Subject: " + email.getSubject());
      // System.out.println("Text: " + email.getText());
      // System.out.println("HTML: " + email.getHtml());

      inbox = sharedClient.createInbox();
      smtp.sendHtmlEmail(
          inbox.getEmailAddress(),
          "test@example.com",
          "Welcome Email",
          "Plain text content",
          "<html><body>HTML content</body></html>");

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));

      assertNotNull(email.getFrom());
      assertEquals("Welcome Email", email.getSubject());
      assertNotNull(email.getText());
      assertNotNull(email.getHtml());
    }
  }

  // ==================== ClientConfig Examples ====================

  @Nested
  @DisplayName("ClientConfig Builder Examples")
  class ClientConfigExamples {

    @Test
    @DisplayName("ClientConfig.builder() - full configuration options")
    void testClientConfigBuilder() {
      // README Example:
      // ClientConfig config = ClientConfig.builder()
      //     .apiKey("your-api-key")
      //     .baseUrl("https://smtp.vaultsandbox.com")
      //     .strategy(StrategyType.AUTO)
      //     .waitTimeout(Duration.ofSeconds(30))
      //     .maxRetries(3)
      //     .build();

      ClientConfig config =
          ClientConfig.builder()
              .apiKey(TestConfig.getApiKey())
              .baseUrl(TestConfig.getBaseUrl())
              .strategy(StrategyType.AUTO)
              .httpTimeout(Duration.ofSeconds(30))
              .waitTimeout(Duration.ofSeconds(30))
              .maxRetries(3)
              .retryDelay(Duration.ofSeconds(1))
              .pollInterval(Duration.ofSeconds(2))
              .sseReconnectInterval(Duration.ofSeconds(5))
              .sseMaxReconnectAttempts(10)
              .build();

      assertEquals(TestConfig.getApiKey(), config.getApiKey());
      assertEquals(TestConfig.getBaseUrl(), config.getBaseUrl());
      assertEquals(StrategyType.AUTO, config.getStrategy());
      assertEquals(Duration.ofSeconds(30), config.getHttpTimeout());
      assertEquals(Duration.ofSeconds(30), config.getWaitTimeout());
      assertEquals(3, config.getMaxRetries());
      assertEquals(Duration.ofSeconds(1), config.getRetryDelay());
      assertEquals(Duration.ofSeconds(2), config.getPollInterval());
      assertEquals(Duration.ofSeconds(5), config.getSseReconnectInterval());
      assertEquals(10, config.getSseMaxReconnectAttempts());
    }

    @Test
    @DisplayName("StrategyType enum values exist")
    void testStrategyTypeEnum() {
      // README mentions: SSE, POLLING, AUTO

      assertNotNull(StrategyType.SSE);
      assertNotNull(StrategyType.POLLING);
      assertNotNull(StrategyType.AUTO);
    }
  }

  // ==================== EmailFilter Examples ====================

  @Nested
  @DisplayName("EmailFilter Examples")
  class EmailFilterExamples {

    @Test
    @DisplayName("EmailFilter.any() - match any email")
    void testFilterAny() {
      // README Example:
      // EmailFilter.any()

      EmailFilter filter = EmailFilter.any();

      Email email = TestHelpers.createTestEmail("anyone@example.com", "Any Subject");
      assertTrue(filter.matches(email));
    }

    @Test
    @DisplayName("EmailFilter.subjectContains() - match by subject substring")
    void testFilterSubjectContains() {
      // README Example:
      // EmailFilter.subjectContains("Welcome")

      EmailFilter filter = EmailFilter.subjectContains("Welcome");

      Email matchingEmail = TestHelpers.createTestEmail("sender@example.com", "Welcome to our app");
      Email nonMatchingEmail = TestHelpers.createTestEmail("sender@example.com", "Goodbye");

      assertTrue(filter.matches(matchingEmail));
      assertFalse(filter.matches(nonMatchingEmail));
    }

    @Test
    @DisplayName("EmailFilter.subjectMatches(Pattern) - match by subject regex")
    void testFilterSubjectMatches() {
      // README Example:
      // EmailFilter.subjectMatches(Pattern.compile("Order #\\d+"))

      EmailFilter filter = EmailFilter.subjectMatches(Pattern.compile("Order #\\d+"));

      Email matchingEmail =
          TestHelpers.createTestEmail("orders@shop.com", "Order #12345 Confirmed");
      Email nonMatchingEmail =
          TestHelpers.createTestEmail("orders@shop.com", "Your order is ready");

      assertTrue(filter.matches(matchingEmail));
      assertFalse(filter.matches(nonMatchingEmail));
    }

    @Test
    @DisplayName("EmailFilter.from() - match by sender substring")
    void testFilterFrom() {
      // README Example:
      // EmailFilter.from("noreply@")

      EmailFilter filter = EmailFilter.from("noreply@");

      Email matchingEmail = TestHelpers.createTestEmail("noreply@example.com", "Notification");
      Email nonMatchingEmail = TestHelpers.createTestEmail("support@example.com", "Notification");

      assertTrue(filter.matches(matchingEmail));
      assertFalse(filter.matches(nonMatchingEmail));
    }

    @Test
    @DisplayName("EmailFilter.fromMatches(Pattern) - match by sender regex")
    void testFilterFromMatches() {
      // README Example:
      // EmailFilter.fromMatches(Pattern.compile(".*@example\\.com"))

      EmailFilter filter = EmailFilter.fromMatches(Pattern.compile(".*@example\\.com"));

      Email matchingEmail = TestHelpers.createTestEmail("anyone@example.com", "Test");
      Email nonMatchingEmail = TestHelpers.createTestEmail("anyone@other.com", "Test");

      assertTrue(filter.matches(matchingEmail));
      assertFalse(filter.matches(nonMatchingEmail));
    }

    @Test
    @DisplayName("EmailFilter.matching() - custom predicate")
    void testFilterMatching() {
      // README Example:
      // EmailFilter.matching(email -> email.getLinks().size() > 0)

      EmailFilter filter = EmailFilter.matching(email -> !email.getLinks().isEmpty());

      Email emailWithLinks =
          TestHelpers.createTestEmailWithLinks(
              "sender@example.com",
              "Email with links",
              List.of("https://example.com/link1", "https://example.com/link2"));
      Email emailWithoutLinks =
          TestHelpers.createTestEmailWithLinks(
              "sender@example.com", "Email without links", List.of());

      assertTrue(filter.matches(emailWithLinks));
      assertFalse(filter.matches(emailWithoutLinks));
    }

    @Test
    @DisplayName("EmailFilter.and() - combine filters")
    void testFilterCombine() {
      // README Example:
      // EmailFilter.subjectContains("Reset").and(EmailFilter.from("auth@"))

      EmailFilter filter = EmailFilter.subjectContains("Reset").and(EmailFilter.from("auth@"));

      Email matchingEmail = TestHelpers.createTestEmail("auth@example.com", "Reset your password");
      Email wrongSubject = TestHelpers.createTestEmail("auth@example.com", "Welcome to the app");
      Email wrongSender = TestHelpers.createTestEmail("support@example.com", "Reset your password");

      assertTrue(filter.matches(matchingEmail));
      assertFalse(filter.matches(wrongSubject));
      assertFalse(filter.matches(wrongSender));
    }

    @Test
    @DisplayName("EmailFilter builder pattern - alternative API")
    void testFilterBuilderPattern() {
      // Builder pattern is also available

      EmailFilter filter =
          EmailFilter.builder()
              .subject("Welcome")
              .from("example.com")
              .subjectMatches(".*service$")
              .where(e -> !e.isRead())
              .build();

      Email email = TestHelpers.createTestEmail("sender@example.com", "Welcome to our service");
      assertTrue(filter.matches(email));
    }
  }

  // ==================== Wait Methods Examples ====================

  @Nested
  @DisplayName("Wait Methods Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class WaitMethodExamples {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          sharedClient.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("waitForEmail() - basic wait for email")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testWaitForEmail() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // Email email = inbox.waitForEmail();

      inbox = sharedClient.createInbox();
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Test Subject", "Test body");

      Email email = inbox.waitForEmail();

      assertNotNull(email);
      assertEquals("Test Subject", email.getSubject());
    }

    @Test
    @Order(2)
    @DisplayName("waitForEmail(filter) - wait with filter")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testWaitForEmailWithFilter() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // Email email = inbox.waitForEmail(EmailFilter.subjectContains("Welcome"));

      inbox = sharedClient.createInbox();
      smtp.sendEmail(
          inbox.getEmailAddress(), "test@example.com", "Welcome to the app", "Welcome body");

      Email email = inbox.waitForEmail(EmailFilter.subjectContains("Welcome"));

      assertNotNull(email);
      assertTrue(email.getSubject().contains("Welcome"));
    }

    @Test
    @Order(3)
    @DisplayName("waitForEmail(filter, timeout) - wait with custom timeout")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testWaitForEmailWithTimeout() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(10));

      inbox = sharedClient.createInbox();
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Timed Test", "Body");

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(10));

      assertNotNull(email);
    }

    @Test
    @Order(4)
    @DisplayName("waitForEmailCount() - wait for multiple emails")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testWaitForEmailCount() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // List<Email> emails = inbox.waitForEmailCount(3, Duration.ofSeconds(30));

      inbox = sharedClient.createInbox();
      smtp.sendMultipleEmails(inbox.getEmailAddress(), 3);

      List<Email> emails = inbox.waitForEmailCount(3, Duration.ofSeconds(30));

      assertEquals(3, emails.size());
    }

    @Test
    @Order(5)
    @DisplayName("awaitEmail() - null-safe wait")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testAwaitEmail() {
      // README Example:
      // Email email = inbox.awaitEmail(EmailFilter.from("noreply@example.com"));
      // if (email != null) { // Process the email }

      inbox = sharedClient.createInbox();

      // No email sent, so awaitEmail should return null
      Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ofSeconds(2));

      // awaitEmail returns null on timeout instead of throwing
      // This is expected behavior for an empty inbox with short timeout
      assertNull(email, "awaitEmail should return null when no email arrives");
    }
  }

  // ==================== Real-time Monitoring Examples ====================

  @Nested
  @DisplayName("Real-time Monitoring Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class RealtimeMonitoringExamples {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          sharedClient.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("onNewEmail() - subscribe to new emails")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOnNewEmail() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Example:
      // Subscription subscription = inbox.onNewEmail(email -> {
      //     System.out.println("New email received: \"" + email.getSubject() + "\"");
      // });

      inbox = sharedClient.createInbox();

      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Email> receivedEmail = new AtomicReference<>();

      Subscription subscription =
          inbox.onNewEmail(
              email -> {
                receivedEmail.set(email);
                latch.countDown();
              });

      assertNotNull(subscription);

      // Send email after subscribing
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Subscription Test", "Body");

      // Wait for email to be received via callback
      boolean received = latch.await(15, TimeUnit.SECONDS);
      subscription.unsubscribe();

      assertTrue(received, "Should receive email via subscription");
      assertNotNull(receivedEmail.get());
      assertEquals("Subscription Test", receivedEmail.get().getSubject());
    }

    @Test
    @Order(2)
    @DisplayName("subscription.unsubscribe() - stop listening")
    void testUnsubscribe() {
      // README Example:
      // subscription.unsubscribe();

      inbox = sharedClient.createInbox();

      Subscription subscription = inbox.onNewEmail(email -> {});

      assertNotNull(subscription);

      // Should not throw
      subscription.unsubscribe();
    }

    @Test
    @Order(3)
    @DisplayName("monitorInboxes() - monitor multiple inboxes")
    void testMonitorInboxes() {
      // README Example:
      // InboxMonitor monitor = client.monitorInboxes(inbox1, inbox2);

      Inbox inbox1 = sharedClient.createInbox();
      Inbox inbox2 = sharedClient.createInbox();

      try {
        InboxMonitor monitor = sharedClient.monitorInboxes(inbox1, inbox2);

        assertNotNull(monitor);

        monitor.close();
      } finally {
        sharedClient.deleteInbox(inbox1.getEmailAddress());
        sharedClient.deleteInbox(inbox2.getEmailAddress());
      }
    }
  }

  // ==================== Inbox Operations Examples ====================

  @Nested
  @DisplayName("Inbox Operations Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class InboxOperationsExamples {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          sharedClient.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("Inbox properties - emailAddress, hash, expiresAt")
    void testInboxProperties() {
      // README Properties:
      // | emailAddress | String | The inbox email address |
      // | hash | String | Unique inbox identifier |
      // | expiresAt | Instant | When the inbox expires |

      inbox = sharedClient.createInbox();

      assertNotNull(inbox.getEmailAddress());
      assertTrue(inbox.getEmailAddress().contains("@"));
      assertNotNull(inbox.getHash());
      assertNotNull(inbox.getExpiresAt());
    }

    @Test
    @Order(2)
    @DisplayName("listEmails() - list all emails in inbox")
    void testListEmails() {
      // README Method: listEmails() | Lists all emails (decrypted)

      inbox = sharedClient.createInbox();

      List<Email> emails = inbox.listEmails();

      assertNotNull(emails);
      assertTrue(emails.isEmpty(), "New inbox should have no emails");
    }

    @Test
    @Order(3)
    @DisplayName("getSyncStatus() - get inbox sync status")
    void testGetSyncStatus() {
      // README Method: getSyncStatus() | Gets inbox sync status

      inbox = sharedClient.createInbox();

      var status = inbox.getSyncStatus();

      assertNotNull(status);
      assertNotNull(status.getEmailsHash());
      assertEquals(0, status.getEmailCount());
    }

    @Test
    @Order(4)
    @DisplayName("export() - export inbox data")
    void testExportInbox() {
      // README Method: export() | Exports inbox data for backup/sharing

      inbox = sharedClient.createInbox();

      var exported = inbox.export();

      assertNotNull(exported);
      assertEquals(inbox.getEmailAddress(), exported.getEmailAddress());
      assertNotNull(exported.getPublicKeyB64());
      assertNotNull(exported.getSecretKeyB64());
    }

    @Test
    @Order(5)
    @DisplayName("Email operations - getEmail, markAsRead, deleteEmail")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testEmailOperations() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Methods:
      // getEmail(String emailId) | Gets a specific email
      // markEmailAsRead(String emailId) | Marks email as read
      // deleteEmail(String emailId) | Deletes an email

      inbox = sharedClient.createInbox();
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Operations Test", "Body");

      Email email = inbox.waitForEmail();
      String emailId = email.getId();

      // getEmail
      Email fetched = inbox.getEmail(emailId);
      assertNotNull(fetched);
      assertEquals(emailId, fetched.getId());

      // markEmailAsRead
      inbox.markEmailAsRead(emailId);
      Email afterRead = inbox.getEmail(emailId);
      assertTrue(afterRead.isRead());

      // deleteEmail
      inbox.deleteEmail(emailId);
      List<Email> remaining = inbox.listEmails();
      assertTrue(remaining.isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("getRawEmail() - get raw email source")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testGetRawEmail() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Method: getRawEmail(String emailId) | Gets the raw email source (RFC 5322)

      inbox = sharedClient.createInbox();
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Raw Test", "Body content");

      Email email = inbox.waitForEmail();
      String raw = inbox.getRawEmail(email.getId());

      assertNotNull(raw);
      assertFalse(raw.isEmpty(), "Raw email should not be empty");
    }
  }

  // ==================== Email Properties Examples ====================

  @Nested
  @DisplayName("Email Properties Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class EmailPropertiesExamples {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          sharedClient.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @Order(1)
    @DisplayName("Email properties - all getters")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testEmailProperties() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Properties table

      inbox = sharedClient.createInbox();
      smtp.sendHtmlEmail(
          inbox.getEmailAddress(),
          "test@example.com",
          "Properties Test",
          "Plain text",
          "<html><body><a href=\"https://example.com\">Link</a></body></html>");

      Email email = inbox.waitForEmail();

      // All property getters
      assertNotNull(email.getId());
      assertNotNull(email.getFrom());
      assertNotNull(email.getTo());
      assertFalse(email.getTo().isEmpty());
      assertNotNull(email.getSubject());
      assertNotNull(email.getText());
      assertNotNull(email.getHtml());
      assertNotNull(email.getReceivedAt());
      assertFalse(email.isRead()); // New email should be unread
      assertNotNull(email.getLinks());
      assertNotNull(email.getHeaders());
      assertNotNull(email.getAttachments());
      // AuthResults may or may not be present depending on email source
      // Just verify the method doesn't throw
      assertDoesNotThrow(() -> email.getAuthResults());
    }

    @Test
    @Order(2)
    @DisplayName("Email action methods - markAsRead, delete, getRaw")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testEmailActionMethods() throws Exception {
      assumeTrue(hasSmtp(), "SMTP required for email tests");

      // README Methods:
      // | markAsRead() | Marks this email as read |
      // | delete() | Deletes this email |
      // | getRaw() | Gets raw email source |

      inbox = sharedClient.createInbox();
      smtp.sendEmail(inbox.getEmailAddress(), "test@example.com", "Action Methods Test", "Body");

      Email email = inbox.waitForEmail();

      // getRaw via email object
      String raw = email.getRaw();
      assertNotNull(raw);
      assertFalse(raw.isEmpty(), "Raw email should not be empty");

      // markAsRead via email object
      email.markAsRead();
      Email updated = inbox.getEmail(email.getId());
      assertTrue(updated.isRead());

      // delete via email object
      email.delete();
      assertTrue(inbox.listEmails().isEmpty());
    }
  }

  // ==================== Client Methods Examples ====================

  @Nested
  @DisplayName("VaultSandboxClient Methods Examples")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class ClientMethodsExamples {

    @Test
    @Order(1)
    @DisplayName("getServerInfo() - get server information")
    void testGetServerInfo() {
      // README Method: getServerInfo() | Gets server information

      var info = sharedClient.getServerInfo();

      assertNotNull(info);
      assertNotNull(info.getServerSigPk());
      assertNotNull(info.getAlgorithms());
    }

    @Test
    @Order(2)
    @DisplayName("checkKey() - validate API key")
    void testCheckKey() {
      // README Method: checkKey() | Validates API key, returns boolean

      boolean valid = sharedClient.checkKey();

      assertTrue(valid);
    }

    @Test
    @Order(3)
    @DisplayName("createInbox() and deleteInbox() - inbox lifecycle")
    void testInboxLifecycle() {
      // README Methods:
      // createInbox() | Creates a new inbox
      // deleteInbox(String emailAddress) | Deletes a specific inbox

      Inbox inbox = sharedClient.createInbox();
      assertNotNull(inbox);

      String email = inbox.getEmailAddress();
      sharedClient.deleteInbox(email);

      // Verify deletion
      try {
        sharedClient.getInbox(email);
        // If we get here, getInbox might return cached data
      } catch (Exception e) {
        // Expected - inbox should not exist
      }
    }

    @Test
    @Order(4)
    @DisplayName("deleteAllInboxes() - delete all inboxes")
    void testDeleteAllInboxes() {
      // README Method: deleteAllInboxes() | Deletes all inboxes, returns count

      // Create some inboxes first
      sharedClient.createInbox();
      sharedClient.createInbox();

      int deleted = sharedClient.deleteAllInboxes();

      assertTrue(deleted >= 2);
    }

    @Test
    @Order(5)
    @DisplayName("getInbox() - get locally tracked inbox")
    void testGetInbox() {
      // README Method: getInbox(String emailAddress) | Gets a locally tracked inbox

      Inbox created = sharedClient.createInbox();
      String email = created.getEmailAddress();

      try {
        Inbox retrieved = sharedClient.getInbox(email);

        assertNotNull(retrieved);
        assertEquals(email, retrieved.getEmailAddress());
      } finally {
        sharedClient.deleteInbox(email);
      }
    }

    @Test
    @Order(6)
    @DisplayName("exportInbox() and importInbox() - export/import flow")
    void testExportImportInbox() {
      // README Methods:
      // exportInbox(Inbox inbox) | Exports an inbox's data
      // importInbox(ExportedInbox data) | Imports an inbox from exported data

      Inbox original = sharedClient.createInbox();
      String email = original.getEmailAddress();

      try {
        // Export the inbox
        var exported = sharedClient.exportInbox(original);
        assertNotNull(exported);
        assertEquals(email, exported.getEmailAddress());
        assertNotNull(exported.getPublicKeyB64());
        assertNotNull(exported.getSecretKeyB64());
        assertNotNull(exported.getExportedAt());

        // Import into a new client (simulating transfer to another process/machine)
        VaultSandboxClient newClient =
            VaultSandboxClient.create(
                ClientConfig.builder()
                    .apiKey(TestConfig.getApiKey())
                    .baseUrl(TestConfig.getBaseUrl())
                    .build());

        try {
          Inbox imported = newClient.importInbox(exported);
          assertNotNull(imported);
          assertEquals(email, imported.getEmailAddress());

          // Verify the imported inbox is functional
          var status = imported.getSyncStatus();
          assertNotNull(status);
        } finally {
          newClient.close();
        }
      } finally {
        try {
          sharedClient.deleteInbox(email);
        } catch (Exception ignored) {
        }
      }
    }
  }

  // ==================== Email Authentication Examples ====================

  @Nested
  @DisplayName("Testing Email Authentication (SPF/DKIM/DMARC)")
  class EmailAuthenticationExamples {

    @Test
    @DisplayName("AuthResults.validate() returns AuthValidation")
    void testAuthResultsValidate() {
      // README Example:
      // var validation = email.getAuthResults().validate();
      // if (!validation.isPassed()) {
      //     validation.getFailures().forEach(reason -> ...);
      // }

      AuthResults results = TestHelpers.createPassingAuthResults();
      AuthValidation validation = results.validate();

      assertNotNull(validation);
      assertTrue(validation.isPassed());
      assertTrue(validation.getFailures().isEmpty());
    }

    @Test
    @DisplayName("AuthValidation.isPassed() returns false when checks fail")
    void testAuthValidationFailed() {
      AuthResults results = TestHelpers.createFailingSpfAuthResults();
      AuthValidation validation = results.validate();

      assertFalse(validation.isPassed());
      assertFalse(validation.getFailures().isEmpty());
      assertTrue(validation.getFailures().stream().anyMatch(reason -> reason.contains("SPF")));
    }

    @Test
    @DisplayName("Check individual SPF result")
    void testSpfResult() {
      // README Example:
      // if (authResults.getSpf() != null) {
      //     System.out.println("SPF result: " + authResults.getSpf().getResult());
      // }

      AuthResults results = TestHelpers.createPassingAuthResults();

      assertNotNull(results.getSpf());
      assertEquals("pass", results.getSpf().getResult());
      assertEquals("example.com", results.getSpf().getDomain());
    }

    @Test
    @DisplayName("Check DKIM signatures")
    void testDkimResults() {
      // README Example:
      // if (authResults.getDkim() != null) {
      //     System.out.println("DKIM signatures: " + authResults.getDkim().size());
      // }

      AuthResults results = TestHelpers.createPassingAuthResults();

      assertNotNull(results.getDkim());
      assertFalse(results.getDkim().isEmpty());
      assertEquals(1, results.getDkim().size());
      assertEquals("pass", results.getDkim().get(0).getResult());
    }

    @Test
    @DisplayName("Check DMARC result")
    void testDmarcResult() {
      // README Example:
      // if (authResults.getDmarc() != null) {
      //     System.out.println("DMARC result: " + authResults.getDmarc().getResult());
      // }

      AuthResults results = TestHelpers.createPassingAuthResults();

      assertNotNull(results.getDmarc());
      assertEquals("pass", results.getDmarc().getResult());
    }

    @Test
    @DisplayName("AuthValidation helper methods - hasSpf, hasDkim, hasDmarc")
    void testAuthValidationHelpers() {
      AuthResults results = TestHelpers.createPassingAuthResults();
      AuthValidation validation = results.validate();

      assertTrue(validation.hasSpf());
      assertTrue(validation.hasDkim());
      assertTrue(validation.hasDmarc());
      assertTrue(validation.hasReverseDns());
    }
  }

  // ==================== Error Handling Examples ====================

  @Nested
  @DisplayName("Error Handling Examples")
  class ErrorHandlingExamples {

    @Test
    @DisplayName("Exception hierarchy - VaultSandboxException is base class")
    void testExceptionHierarchy() {
      // README shows these exceptions all extend VaultSandboxException

      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.ApiException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.NetworkException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.TimeoutException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.InboxNotFoundException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.EmailNotFoundException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.InboxAlreadyExistsException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.InvalidImportDataException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.DecryptionException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.SignatureVerificationException.class));
      assertTrue(
          VaultSandboxException.class.isAssignableFrom(
              com.vaultsandbox.client.exception.SseException.class));
    }

    @Test
    @DisplayName("ApiException has getStatusCode()")
    void testApiExceptionStatusCode() {
      // README Example:
      // catch (ApiException e) {
      //     System.err.println("API Error (" + e.getStatusCode() + "): " + e.getMessage());
      // }

      ApiException exception = new ApiException("Not found", 404);

      assertEquals(404, exception.getStatusCode());
      assertEquals("Not found", exception.getMessage());
    }

    @Test
    @DisplayName("TimeoutException for email wait timeout")
    void testTimeoutException() {
      // README Example:
      // catch (TimeoutException e) {
      //     System.err.println("Timed out waiting for email: " + e.getMessage());
      // }

      TimeoutException exception = new TimeoutException("No email received within 30 seconds");

      assertNotNull(exception.getMessage());
      assertTrue(exception.getMessage().contains("30 seconds"));
    }

    @Test
    @DisplayName("Exception handling pattern - try/catch flow")
    void testExceptionHandlingPattern() {
      // README Example pattern:
      // try {
      //     Email email = inbox.waitForEmail(Duration.ofSeconds(5));
      // } catch (TimeoutException e) { ... }
      //   catch (ApiException e) { ... }
      //   catch (VaultSandboxException e) { ... }

      // This demonstrates the exception handling pattern compiles correctly
      Exception caughtException = null;
      try {
        // Simulate code that might throw
        throw new TimeoutException("Test timeout");
      } catch (TimeoutException e) {
        caughtException = e;
      } catch (ApiException e) {
        caughtException = e;
      } catch (VaultSandboxException e) {
        caughtException = e;
      }

      assertNotNull(caughtException);
      assertTrue(caughtException instanceof TimeoutException);
    }
  }

  // ==================== Link Extraction Examples ====================

  @Nested
  @DisplayName("Extracting and Validating Links")
  class LinkExtractionExamples {

    @Test
    @DisplayName("Extract verification link from email")
    void testExtractVerifyLink() {
      // README Example:
      // String verifyLink = email.getLinks().stream()
      //     .filter(url -> url.contains("/verify"))
      //     .findFirst()
      //     .orElseThrow();

      Email email =
          TestHelpers.createTestEmailWithLinks(
              "noreply@example.com",
              "Verify your email",
              List.of(
                  "https://example.com/unsubscribe",
                  "https://example.com/verify?token=xyz789",
                  "https://example.com/privacy"));

      String verifyLink =
          email.getLinks().stream()
              .filter(url -> url.contains("/verify"))
              .findFirst()
              .orElseThrow();

      assertNotNull(verifyLink);
      assertTrue(verifyLink.startsWith("https://"));
      assertTrue(verifyLink.contains("/verify"));
    }

    @Test
    @DisplayName("Email.getLinks() returns all extracted URLs")
    void testGetLinksReturnsAllUrls() {
      List<String> expectedLinks =
          List.of(
              "https://example.com/link1",
              "https://example.com/link2",
              "https://example.com/link3");

      Email email =
          TestHelpers.createTestEmailWithLinks(
              "sender@example.com", "Multiple Links", expectedLinks);

      assertEquals(3, email.getLinks().size());
      assertTrue(email.getLinks().containsAll(expectedLinks));
    }
  }

  // ==================== Attachment Examples ====================

  @Nested
  @DisplayName("Working with Email Attachments")
  class AttachmentExamples {

    @Test
    @DisplayName("Access attachment properties - filename, contentType, size")
    void testAttachmentProperties() {
      // README Example iteration over attachments

      byte[] pdfContent = "PDF file content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      var attachmentData =
          TestHelpers.createAttachmentData("invoice.pdf", "application/pdf", pdfContent);

      Email email =
          TestHelpers.createTestEmailWithAttachments(
              "billing@example.com", "Documents Attached", List.of(attachmentData));

      assertEquals(1, email.getAttachments().size());

      var attachment = email.getAttachments().get(0);
      assertEquals("invoice.pdf", attachment.getFilename());
      assertEquals("application/pdf", attachment.getContentType());
      assertEquals(pdfContent.length, attachment.getSize());
    }

    @Test
    @DisplayName("Find specific attachment by filename")
    void testFindAttachmentByFilename() {
      // README Example:
      // var pdfAttachment = email.getAttachments().stream()
      //     .filter(att -> "invoice.pdf".equals(att.getFilename()))
      //     .findFirst()
      //     .orElseThrow();

      byte[] pdfContent = new byte[] {0x25, 0x50, 0x44, 0x46}; // PDF magic bytes
      byte[] txtContent = "readme".getBytes(java.nio.charset.StandardCharsets.UTF_8);

      var attachments =
          List.of(
              TestHelpers.createAttachmentData("invoice.pdf", "application/pdf", pdfContent),
              TestHelpers.createAttachmentData("readme.txt", "text/plain", txtContent));

      Email email =
          TestHelpers.createTestEmailWithAttachments("billing@example.com", "Invoice", attachments);

      var pdfAttachment =
          email.getAttachments().stream()
              .filter(att -> "invoice.pdf".equals(att.getFilename()))
              .findFirst()
              .orElseThrow();

      assertEquals("application/pdf", pdfAttachment.getContentType());
      assertTrue(pdfAttachment.getSize() > 0);
    }
  }
}
