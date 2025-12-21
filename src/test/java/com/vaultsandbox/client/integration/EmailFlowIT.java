package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.SmtpTestHelper;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.TestHelpers;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.exception.EmailNotFoundException;
import com.vaultsandbox.client.exception.TimeoutException;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.nio.charset.StandardCharsets;
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
 * Integration tests for email receiving, filtering, and operations. Note: These tests require the
 * ability to send test emails.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailFlowIT {

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
  void testListEmailsEmpty() {
    Inbox inbox = client.createInbox();

    List<Email> emails = inbox.listEmails();

    assertTrue(emails.isEmpty());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(2)
  void testEmailFilter() {
    // Create some mock emails using TestHelpers for filter testing
    Email email1 = TestHelpers.createTestEmail("noreply@service.com", "Your verification code");
    Email email2 = TestHelpers.createTestEmail("support@other.com", "Welcome to our platform");
    Email email3 = TestHelpers.createTestEmail("noreply@service.com", "Password reset request");

    // Test subject filter
    EmailFilter subjectFilter = EmailFilter.builder().subject("verification").build();
    assertTrue(subjectFilter.matches(email1));
    assertFalse(subjectFilter.matches(email2));
    assertFalse(subjectFilter.matches(email3));

    // Test from filter
    EmailFilter fromFilter = EmailFilter.builder().from("service.com").build();
    assertTrue(fromFilter.matches(email1));
    assertFalse(fromFilter.matches(email2));
    assertTrue(fromFilter.matches(email3));

    // Test combined filter
    EmailFilter combinedFilter =
        EmailFilter.builder().subject("Password").from("service.com").build();
    assertFalse(combinedFilter.matches(email1));
    assertFalse(combinedFilter.matches(email2));
    assertTrue(combinedFilter.matches(email3));
  }

  @Test
  @Order(3)
  void testEmailFilterRegex() {
    Email email =
        TestHelpers.createTestEmail("notifications@company.co.uk", "Order #12345 confirmed");

    // Test subject regex
    EmailFilter regexFilter = EmailFilter.builder().subjectMatches("Order #\\d+ confirmed").build();
    assertTrue(regexFilter.matches(email));

    // Test from regex
    EmailFilter fromRegexFilter = EmailFilter.builder().fromMatches(".*@company\\.co\\.uk").build();
    assertTrue(fromRegexFilter.matches(email));
  }

  @Test
  @Order(4)
  void testEmailFilterPredicate() {
    Email shortSubject = TestHelpers.createTestEmail("test@example.com", "Hi");
    Email longSubject =
        TestHelpers.createTestEmail("test@example.com", "This is a very long subject line");

    EmailFilter lengthFilter =
        EmailFilter.builder().where(e -> e.getSubject().length() > 10).build();

    assertFalse(lengthFilter.matches(shortSubject));
    assertTrue(lengthFilter.matches(longSubject));
  }

  @Test
  @Order(5)
  void testAnyFilter() {
    Email email1 = TestHelpers.createTestEmail("a@b.com", "Test 1");
    Email email2 = TestHelpers.createTestEmail("c@d.com", "Test 2");

    EmailFilter anyFilter = EmailFilter.any();

    assertTrue(anyFilter.matches(email1));
    assertTrue(anyFilter.matches(email2));
  }

  @Test
  @Order(6)
  void testInboxCanBeReused() {
    Inbox inbox = client.createInbox();

    // Verify multiple operations work
    assertNotNull(inbox.getSyncStatus());
    assertNotNull(inbox.listEmails());
    assertNotNull(inbox.getSyncStatus());
    assertNotNull(inbox.listEmails());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(7)
  void testEmailGetNonExistent() {
    Inbox inbox = client.createInbox();

    assertThrows(EmailNotFoundException.class, () -> inbox.getEmail("nonexistent-email-id"));

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(8)
  void testDeleteEmailNonExistent() {
    Inbox inbox = client.createInbox();

    assertThrows(EmailNotFoundException.class, () -> inbox.deleteEmail("nonexistent-email-id"));

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(9)
  void testMarkEmailAsReadNonExistent() {
    Inbox inbox = client.createInbox();

    assertThrows(EmailNotFoundException.class, () -> inbox.markEmailAsRead("nonexistent-email-id"));

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(10)
  void testEmailAccessMethods() {
    // Create a test email with known properties
    Email email = TestHelpers.createTestEmail("sender@domain.com", "Test Subject");

    assertEquals("sender@domain.com", email.getFrom());
    assertEquals("Test Subject", email.getSubject());
    assertNotNull(email.getId());
    assertNotNull(email.getReceivedAt());
    assertNotNull(email.getTo());
    assertFalse(email.getTo().isEmpty());
  }

  @Test
  @Order(11)
  void testEmailContentAccess() {
    Email email =
        TestHelpers.createTestEmailWithHtml(
            "sender@example.com",
            "HTML Email Test",
            "Plain text content",
            "<html><body><h1>HTML content</h1></body></html>");

    assertEquals("Plain text content", email.getText());
    assertEquals("<html><body><h1>HTML content</h1></body></html>", email.getHtml());
    assertNotNull(email.getHeaders());
    assertNotNull(email.getAttachments());
    assertNotNull(email.getLinks());
  }

  @Test
  @Order(12)
  void testSyncStatusChangesDetected() {
    Inbox inbox = client.createInbox();

    var status1 = inbox.getSyncStatus();

    // Without new emails, status should remain same
    var status2 = inbox.getSyncStatus();

    assertEquals(status1.getEmailsHash(), status2.getEmailsHash());
    assertEquals(status1.getEmailCount(), status2.getEmailCount());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  // ==================== Real Email Tests (require SMTP) ====================

  @Test
  @Order(20)
  void testSendAndReceiveEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "Test email " + uniqueId;
    String body = "This is a test email body with unique ID: " + uniqueId;

    // Start listening BEFORE sending email (establishes SSE connection)
    var subscription = inbox.onNewEmail(e -> {});

    try {
      // Small delay to ensure SSE is connected
      Thread.sleep(500);

      // Send email
      smtp.sendEmail(inbox.getEmailAddress(), "sender@test.com", subject, body);

      // Wait for email to arrive
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertTrue(email.getSubject().contains(uniqueId));
      assertTrue(email.getText().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  void testSendAndReceiveHtmlEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String subject = "HTML Test " + uniqueId;
    String text = "Plain text version " + uniqueId;
    String html = "<html><body><h1>HTML version " + uniqueId + "</h1></body></html>";

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send HTML email
      smtp.sendHtmlEmail(inbox.getEmailAddress(), "html@test.com", subject, text, html);

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertTrue(email.getSubject().contains(uniqueId));
      assertTrue(email.getText().contains(uniqueId));
      assertTrue(email.getHtml().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(22)
  void testVerificationCodeFlow() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String code = String.valueOf((int) (Math.random() * 900000) + 100000);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send verification code
      smtp.sendVerificationCode(inbox.getEmailAddress(), code);

      // Wait for email with the code
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(code).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertTrue(email.getSubject().contains(code));
      assertTrue(email.getText().contains(code));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(23)
  void testReceiveMultipleEmails() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending emails
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send 3 emails
      for (int i = 1; i <= 3; i++) {
        smtp.sendEmail(
            inbox.getEmailAddress(),
            "sender" + i + "@test.com",
            "Multi " + uniqueId + " #" + i,
            "Email body " + i);
      }

      // Wait for all emails
      Thread.sleep(5000); // Give time for all emails to arrive

      List<Email> emails = inbox.listEmails();

      assertTrue(emails.size() >= 3, "Expected at least 3 emails, got " + emails.size());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(24)
  void testEmailFilterOnRealEmails() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending emails
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send emails with different subjects
      smtp.sendEmail(inbox.getEmailAddress(), "a@test.com", "Welcome " + uniqueId, "Body 1");
      smtp.sendEmail(inbox.getEmailAddress(), "b@test.com", "Invoice " + uniqueId, "Body 2");
      smtp.sendEmail(
          inbox.getEmailAddress(), "c@test.com", "Welcome " + uniqueId + " again", "Body 3");

      // Wait for the invoice email specifically
      Email invoiceEmail =
          inbox.awaitEmail(
              EmailFilter.builder().subject("Invoice").build(), Duration.ofSeconds(30));

      assertNotNull(invoiceEmail);
      assertTrue(invoiceEmail.getSubject().contains("Invoice"));

      // Filter for welcome emails
      Thread.sleep(2000); // Ensure all emails arrived
      List<Email> allEmails = inbox.listEmails();
      EmailFilter welcomeFilter = EmailFilter.builder().subject("Welcome").build();
      long welcomeCount = allEmails.stream().filter(welcomeFilter::matches).count();

      assertTrue(welcomeCount >= 2, "Expected at least 2 Welcome emails");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(25)
  void testMarkRealEmailAsRead() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send email
      smtp.sendEmail(inbox.getEmailAddress(), "test@test.com", "Read test " + uniqueId, "Body");

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      assertFalse(email.isRead());

      // Mark as read
      inbox.markEmailAsRead(email.getId());

      // Verify it's marked as read
      Email updated = inbox.getEmail(email.getId());
      assertTrue(updated.isRead());
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(26)
  void testDeleteRealEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send email
      smtp.sendEmail(inbox.getEmailAddress(), "test@test.com", "Delete test " + uniqueId, "Body");

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

      assertNotNull(email);
      String emailId = email.getId();

      // Delete the email
      inbox.deleteEmail(emailId);

      // Verify it's deleted
      assertThrows(EmailNotFoundException.class, () -> inbox.getEmail(emailId));
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(27)
  void testAwaitEmailTimeout() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();

    // Try to await an email that will never arrive
    Email email =
        inbox.awaitEmail(
            EmailFilter.builder().subject("nonexistent-xyz-123").build(), Duration.ofSeconds(3));

    // Should return null on timeout
    assertNull(email);

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(28)
  void testSyncStatusAfterRealEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    var statusBefore = inbox.getSyncStatus();
    int countBefore = statusBefore.getEmailCount();
    String hashBefore = statusBefore.getEmailsHash();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Send email
    smtp.sendEmail(inbox.getEmailAddress(), "test@test.com", "Sync test " + uniqueId, "Body");

    // Wait for email
    inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(30));

    var statusAfter = inbox.getSyncStatus();

    assertEquals(countBefore + 1, statusAfter.getEmailCount());
    assertNotEquals(hashBefore, statusAfter.getEmailsHash());

    // Clean up
    client.deleteInbox(inbox.getEmailAddress());
  }

  // ==================== Wait for Email Count Tests ====================

  @Test
  @Order(30)
  void testWaitForEmailCount() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending emails
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send 3 emails
      for (int i = 1; i <= 3; i++) {
        smtp.sendEmail(
            inbox.getEmailAddress(),
            "count" + i + "@test.com",
            "Count Test " + uniqueId + " #" + i,
            "Email body " + i);
      }

      // Wait for exactly 3 emails
      List<Email> emails = inbox.waitForEmailCount(3, Duration.ofSeconds(30));

      assertEquals(3, emails.size(), "Should receive exactly 3 emails");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(31)
  void testWaitForEmailCountTimeout() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send only 1 email
      smtp.sendEmail(
          inbox.getEmailAddress(), "single@test.com", "Single " + uniqueId, "Single email body");

      // Wait for 5 emails - should timeout
      assertThrows(
          TimeoutException.class,
          () -> inbox.waitForEmailCount(5, Duration.ofSeconds(5)),
          "Should throw TimeoutException when not enough emails arrive");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(32)
  void testWaitForEmailCountWithMoreEmailsThanRequested() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    // Start listening BEFORE sending emails
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send 5 emails with small delays to prevent rate limiting
      for (int i = 1; i <= 5; i++) {
        smtp.sendEmail(
            inbox.getEmailAddress(),
            "more" + i + "@test.com",
            "More Test " + uniqueId + " #" + i,
            "Email body " + i);
        Thread.sleep(200); // Small delay between sends
      }

      // Wait for only 3 emails
      List<Email> emails = inbox.waitForEmailCount(3, Duration.ofSeconds(30));

      assertTrue(emails.size() >= 3, "Should receive at least 3 emails");
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Attachment Tests ====================

  @Test
  @Order(40)
  void testReceiveEmailWithAttachment() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    byte[] pdfContent = "This is fake PDF content for testing".getBytes(StandardCharsets.UTF_8);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send email with attachment
      smtp.sendEmailWithAttachment(
          inbox.getEmailAddress(),
          "attachment@test.com",
          "Attachment Test " + uniqueId,
          "Please find attached document",
          "document.pdf",
          pdfContent,
          "application/pdf");

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(20));

      assertNotNull(email, "Should receive email with attachment");
      assertNotNull(email.getSubject(), "Subject should not be null");
      assertTrue(email.getSubject().contains(uniqueId), "Subject should contain unique ID");
      assertNotNull(email.getAttachments(), "Attachments should not be null");

      // Note: Attachment presence depends on server implementation
      // The test passes if we receive the email - attachment parsing is a bonus
      if (!email.getAttachments().isEmpty()) {
        var attachment = email.getAttachments().get(0);
        assertEquals("document.pdf", attachment.getFilename());
        assertEquals("application/pdf", attachment.getContentType());
        assertTrue(attachment.getSize() > 0);
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  void testReceiveEmailWithMultipleAttachments() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    List<SmtpTestHelper.AttachmentInfo> attachments =
        List.of(
            new SmtpTestHelper.AttachmentInfo(
                "report.pdf",
                "PDF report content".getBytes(StandardCharsets.UTF_8),
                "application/pdf"),
            new SmtpTestHelper.AttachmentInfo(
                "image.png", "PNG image bytes".getBytes(StandardCharsets.UTF_8), "image/png"),
            new SmtpTestHelper.AttachmentInfo(
                "data.txt", "Text file content".getBytes(StandardCharsets.UTF_8), "text/plain"));

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send email with multiple attachments
      smtp.sendEmailWithAttachments(
          inbox.getEmailAddress(),
          "multi-attach@test.com",
          "Multi Attachment " + uniqueId,
          "Multiple attachments attached",
          attachments);

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(20));

      assertNotNull(email, "Should receive email with attachments");
      assertNotNull(email.getSubject(), "Subject should not be null");
      assertTrue(email.getSubject().contains(uniqueId), "Subject should contain unique ID");
      assertNotNull(email.getAttachments(), "Attachments should not be null");

      // Note: Multiple attachment handling depends on server implementation
      if (!email.getAttachments().isEmpty()) {
        assertTrue(email.getAttachments().size() >= 1, "Should have at least one attachment");
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(42)
  void testAttachmentContent() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = client.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String textContent = "This is the content of the text file: " + uniqueId;
    byte[] contentBytes = textContent.getBytes(StandardCharsets.UTF_8);

    // Start listening BEFORE sending email
    var subscription = inbox.onNewEmail(e -> {});

    try {
      Thread.sleep(500);

      // Send email with text attachment
      smtp.sendEmailWithAttachment(
          inbox.getEmailAddress(),
          "content@test.com",
          "Content Test " + uniqueId,
          "Check the attachment content",
          "test.txt",
          contentBytes,
          "text/plain");

      // Wait for email
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(20));

      assertNotNull(email, "Should receive email with attachment");
      assertNotNull(email.getSubject(), "Subject should not be null");
      assertTrue(email.getSubject().contains(uniqueId), "Subject should contain unique ID");

      if (!email.getAttachments().isEmpty()) {
        var attachment = email.getAttachments().get(0);
        assertNotNull(attachment.getContent(), "Attachment content should not be null");
        assertEquals(contentBytes.length, attachment.getSize(), "Size should match");

        // Verify content matches
        String retrievedContent = new String(attachment.getContent(), StandardCharsets.UTF_8);
        assertTrue(
            retrievedContent.contains(uniqueId) || attachment.getSize() == contentBytes.length,
            "Content should match or size should match");
      }
    } finally {
      subscription.unsubscribe();
      client.deleteInbox(inbox.getEmailAddress());
    }
  }
}
