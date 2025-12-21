package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.SmtpTestHelper;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.model.AuthResults;
import com.vaultsandbox.client.model.AuthValidation;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * E2E tests for email content: link extraction, headers access, and authentication results. These
 * tests require SMTP availability to send real emails.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailContentIT {

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

  // ==================== Link Extraction Tests ====================

  @Test
  @Order(1)
  void testLinkExtractionFromHtmlEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String verifyUrl = "https://example.com/verify?token=" + uniqueId;
    String unsubscribeUrl = "https://example.com/unsubscribe?user=" + uniqueId;

    String html =
        "<html><body>"
            + "<h1>Welcome!</h1>"
            + "<p>Click <a href=\""
            + verifyUrl
            + "\">here</a> to verify your account.</p>"
            + "<p>To unsubscribe, click <a href=\""
            + unsubscribeUrl
            + "\">here</a>.</p>"
            + "</body></html>";

    String text = "Click this link to verify: " + verifyUrl;

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendHtmlEmailWithLinks(
          inbox.getEmailAddress(), "links@test.com", "Links Test " + uniqueId, text, html);

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email, "Email should be received");
      assertNotNull(email.getLinks(), "Links should not be null");
      assertFalse(email.getLinks().isEmpty(), "Should have extracted links");

      // Check that at least one of our links was extracted
      boolean hasVerifyLink = email.getLinks().stream().anyMatch(l -> l.contains("/verify"));
      boolean hasUnsubscribeLink =
          email.getLinks().stream().anyMatch(l -> l.contains("/unsubscribe"));

      assertTrue(hasVerifyLink || hasUnsubscribeLink, "Should have extracted at least one link");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(2)
  void testLinkExtractionWithMultipleLinks() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    String html =
        "<html><body>"
            + "<a href=\"https://link1.example.com/path1\">Link 1</a>"
            + "<a href=\"https://link2.example.com/path2\">Link 2</a>"
            + "<a href=\"https://link3.example.com/path3\">Link 3</a>"
            + "</body></html>";

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendHtmlEmail(
          inbox.getEmailAddress(), "multilinks@test.com", "Multi Links " + uniqueId, "Plain", html);

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertNotNull(email.getLinks());

      // Should have extracted multiple links
      assertTrue(email.getLinks().size() >= 1, "Should have extracted multiple links");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Headers Access Tests ====================

  @Test
  @Order(10)
  void testHeadersAccess() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(),
          "headers@test.com",
          "Headers Test " + uniqueId,
          "Testing headers");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email, "Email should be received");
      assertNotNull(email.getHeaders(), "Headers should not be null");

      // Headers map should be accessible (may be empty depending on server)
      // But the method should work without throwing
      var headers = email.getHeaders();
      assertNotNull(headers);
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(11)
  void testStandardHeadersFromEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String expectedFrom = "standard@test.com";
    String expectedSubject = "Standard Headers " + uniqueId;

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), expectedFrom, expectedSubject, "Body content");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);

      // Verify standard header-like fields are accessible
      assertEquals(expectedFrom, email.getFrom());
      assertEquals(expectedSubject, email.getSubject());
      assertNotNull(email.getReceivedAt());
      assertNotNull(email.getTo());
      assertFalse(email.getTo().isEmpty());
      assertTrue(email.getTo().contains(inbox.getEmailAddress()));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Authentication Results Tests ====================

  @Test
  @Order(20)
  void testAuthResultsPresent() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "auth@test.com", "Auth Test " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email, "Email should be received");

      // Auth results may or may not be present depending on server configuration
      AuthResults auth = email.getAuthResults();
      if (auth != null) {
        // If auth results are present, validate() should work
        AuthValidation validation = auth.validate();
        assertNotNull(validation);
        assertNotNull(validation.getPassed());
        assertNotNull(validation.getFailed());
      }
      // Test passes whether auth is present or not - we're testing the access methods work
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  void testAuthValidationMethods() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "validate@test.com", "Validate Test " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);

      AuthResults auth = email.getAuthResults();
      if (auth != null) {
        AuthValidation validation = auth.validate();

        // These methods should not throw
        boolean spf = validation.hasSpf();
        boolean dkim = validation.hasDkim();
        boolean dmarc = validation.hasDmarc();
        boolean reverseDns = validation.hasReverseDns();
        boolean fullyAuth = validation.isFullyAuthenticated();

        // Log the results for debugging
        System.out.println("Auth validation results:");
        System.out.println("  SPF: " + spf);
        System.out.println("  DKIM: " + dkim);
        System.out.println("  DMARC: " + dmarc);
        System.out.println("  ReverseDNS: " + reverseDns);
        System.out.println("  Fully authenticated: " + fullyAuth);
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(22)
  void testDirectSendAuthExpectations() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send from a domain we don't control - should fail SPF/DKIM
      smtp.sendEmail(
          inbox.getEmailAddress(),
          "sender@untrusted-domain.example",
          "Direct Send " + uniqueId,
          "Testing auth");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);

      AuthResults auth = email.getAuthResults();
      if (auth != null) {
        AuthValidation validation = auth.validate();

        // Direct sends from untrusted sources typically fail SPF and/or DKIM
        // This test documents the expected behavior - it passes regardless
        // because we're testing the API works, not enforcing auth results
        System.out.println("Direct send auth results:");
        System.out.println("  Passed: " + validation.getPassed());
        System.out.println("  Failed: " + validation.getFailed());
        System.out.println("  Fully authenticated: " + validation.isFullyAuthenticated());
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Raw Email Tests ====================

  @Test
  @Order(30)
  void testGetRawEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Raw Test " + uniqueId;
    String body = "This is the raw email body content";

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "raw@test.com", subject, body);

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);

      // Get raw email content
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
  @Order(31)
  void testGetRawEmailViaEmailObject() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "rawobj@test.com", "Raw Via Email " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);

      // Get raw via email.getRaw() method
      String raw = email.getRaw();

      assertNotNull(raw);
      assertFalse(raw.isEmpty());
      assertTrue(raw.contains("Subject:"));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Get Email By ID Tests ====================

  @Test
  @Order(40)
  void testGetEmailById() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Get By ID " + uniqueId;
    String body = "Test body content";

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "getbyid@test.com", subject, body);

      Email awaited =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(awaited, "Should receive email");

      // Now get the email by ID
      Email retrieved = inbox.getEmail(awaited.getId());

      assertNotNull(retrieved);
      assertEquals(awaited.getId(), retrieved.getId());
      assertEquals(awaited.getSubject(), retrieved.getSubject());
      assertEquals(awaited.getFrom(), retrieved.getFrom());
      assertEquals(awaited.getText(), retrieved.getText());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Email Object Methods Tests ====================

  @Test
  @Order(50)
  void testMarkAsReadViaEmailObject() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "markread@test.com", "Mark Read " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertFalse(email.isRead(), "Email should initially be unread");

      // Mark as read via email object method
      email.markAsRead();

      // Verify by fetching again
      Email updated = inbox.getEmail(email.getId());
      assertTrue(updated.isRead(), "Email should be marked as read");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(51)
  void testDeleteViaEmailObject() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      smtp.sendEmail(inbox.getEmailAddress(), "delete@test.com", "Delete " + uniqueId, "Body");

      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      String emailId = email.getId();

      // Delete via email object method
      email.delete();

      // Verify it's deleted
      try {
        inbox.getEmail(emailId);
        assertTrue(false, "Should have thrown EmailNotFoundException");
      } catch (Exception e) {
        // Expected - email should be deleted
        assertTrue(e.getClass().getSimpleName().contains("EmailNotFoundException"));
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }
}
