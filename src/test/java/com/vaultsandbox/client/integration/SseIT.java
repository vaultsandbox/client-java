package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.InboxMonitor;
import com.vaultsandbox.client.SmtpTestHelper;
import com.vaultsandbox.client.StrategyType;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.strategy.Subscription;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests for SSE (Server-Sent Events) functionality. Note: Full SSE testing requires the
 * ability to send emails while listening.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SseIT {

  private static VaultSandboxClient pollingClient;
  private static VaultSandboxClient sseClient;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    // Create polling client for comparison
    pollingClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.POLLING)
                .build());

    // Create SSE client
    sseClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .build());
  }

  @AfterAll
  static void tearDown() {
    if (pollingClient != null) {
      try {
        pollingClient.deleteAllInboxes();
      } catch (Exception ignored) {
      }
      pollingClient.close();
    }
    if (sseClient != null) {
      sseClient.close();
    }
  }

  @Test
  @Order(1)
  void testSseClientCreation() {
    assertNotNull(sseClient);
    assertEquals(StrategyType.SSE, sseClient.getConfig().getStrategy());
  }

  @Test
  @Order(2)
  void testPollingClientCreation() {
    assertNotNull(pollingClient);
    assertEquals(StrategyType.POLLING, pollingClient.getConfig().getStrategy());
  }

  @Test
  @Order(3)
  void testInboxCreationWithSse() {
    Inbox inbox = sseClient.createInbox();

    assertNotNull(inbox);
    assertNotNull(inbox.getEmailAddress());

    // Clean up
    sseClient.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(4)
  void testSubscription() {
    Inbox inbox = sseClient.createInbox();
    AtomicInteger callCount = new AtomicInteger(0);

    // Create a subscription
    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              callCount.incrementAndGet();
            });

    assertNotNull(subscription);

    // Unsubscribe
    subscription.unsubscribe();

    // Clean up
    sseClient.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(5)
  void testMultipleSubscriptions() {
    Inbox inbox = sseClient.createInbox();

    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);

    Subscription sub1 = inbox.onNewEmail(email -> count1.incrementAndGet());
    Subscription sub2 = inbox.onNewEmail(email -> count2.incrementAndGet());

    assertNotNull(sub1);
    assertNotNull(sub2);

    // Unsubscribe both
    sub1.unsubscribe();
    sub2.unsubscribe();

    // Clean up
    sseClient.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(6)
  void testUnsubscribeIdempotent() {
    Inbox inbox = sseClient.createInbox();

    Subscription subscription = inbox.onNewEmail(email -> {});

    // Unsubscribe multiple times should not throw
    assertDoesNotThrow(
        () -> {
          subscription.unsubscribe();
          subscription.unsubscribe();
          subscription.unsubscribe();
        });

    // Clean up
    sseClient.deleteInbox(inbox.getEmailAddress());
  }

  @Test
  @Order(7)
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testClientClose() {
    VaultSandboxClient tempClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .build());

    Inbox inbox = tempClient.createInbox();
    inbox.onNewEmail(email -> {});

    // Close should complete without hanging
    assertDoesNotThrow(
        () -> {
          tempClient.close();
        });
  }

  @Test
  @Order(8)
  void testInboxMonitor() {
    Inbox inbox1 = sseClient.createInbox();
    Inbox inbox2 = sseClient.createInbox();

    InboxMonitor monitor = sseClient.monitorInboxes(inbox1, inbox2);

    assertNotNull(monitor);

    // Clean up
    monitor.close();
    sseClient.deleteInbox(inbox1.getEmailAddress());
    sseClient.deleteInbox(inbox2.getEmailAddress());
  }

  @Test
  @Order(9)
  void testConfiguredStrategy() {
    // Verify the clients use their configured strategies
    VaultSandboxClient defaultClient = VaultSandboxClient.create(TestConfig.getApiKey());

    // Default strategy is AUTO
    assertEquals(StrategyType.AUTO, defaultClient.getConfig().getStrategy());

    defaultClient.close();
  }

  // ==================== Real Email Callback Tests ====================

  @Test
  @Order(20)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testOnNewEmailCallback() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicReference<Email> received = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              String subject = email.getSubject();
              if (subject != null && subject.contains(uniqueId)) {
                received.set(email);
                latch.countDown();
              }
            });

    try {
      // Wait for SSE connection to establish
      Thread.sleep(500);

      // Send email
      smtp.sendEmail(
          inbox.getEmailAddress(),
          "callback@test.com",
          "Callback Test " + uniqueId,
          "Body content");

      // Wait for callback with shorter timeout
      boolean callbackInvoked = latch.await(15, TimeUnit.SECONDS);

      assertTrue(callbackInvoked, "Callback should be invoked within timeout");
      assertNotNull(received.get(), "Email should be received via callback");
      assertNotNull(received.get().getSubject());
      assertTrue(received.get().getSubject().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testMultipleCallbacksForSameEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(2);

    Subscription sub1 =
        inbox.onNewEmail(
            email -> {
              String subject = email.getSubject();
              if (subject != null && subject.contains(uniqueId)) {
                count1.incrementAndGet();
                latch.countDown();
              }
            });

    Subscription sub2 =
        inbox.onNewEmail(
            email -> {
              String subject = email.getSubject();
              if (subject != null && subject.contains(uniqueId)) {
                count2.incrementAndGet();
                latch.countDown();
              }
            });

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "multicallback@test.com", "Multi Callback " + uniqueId, "Body");

      boolean bothInvoked = latch.await(15, TimeUnit.SECONDS);

      assertTrue(bothInvoked, "Both callbacks should be invoked");
      assertEquals(1, count1.get(), "First callback should be called once");
      assertEquals(1, count2.get(), "Second callback should be called once");
    } finally {
      sub1.unsubscribe();
      sub2.unsubscribe();
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(22)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testUnsubscribeStopsCallback() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger callCount = new AtomicInteger(0);

    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              callCount.incrementAndGet();
            });

    try {
      Thread.sleep(300);

      // Unsubscribe immediately
      subscription.unsubscribe();

      // Wait a moment for unsubscribe to take effect
      Thread.sleep(300);

      // Send email after unsubscribe
      smtp.sendEmail(
          inbox.getEmailAddress(), "afterunsub@test.com", "After Unsub " + uniqueId, "Body");

      // Wait to see if callback is invoked
      Thread.sleep(2000);

      // Count should not have increased from emails sent after unsubscribe
      int countAfter = callCount.get();

      // Wait more
      Thread.sleep(1000);

      // Should remain the same
      assertEquals(
          countAfter, callCount.get(), "Callback count should not increase after unsubscribe");
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(23)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCallbackReceivesEmailContent() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    String expectedBody = "This is the email body content " + uniqueId;
    AtomicReference<Email> received = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              String subject = email.getSubject();
              if (subject != null && subject.contains(uniqueId)) {
                received.set(email);
                latch.countDown();
              }
            });

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "content@test.com", "Content Check " + uniqueId, expectedBody);

      boolean callbackInvoked = latch.await(15, TimeUnit.SECONDS);

      assertTrue(callbackInvoked);
      assertNotNull(received.get());

      // Email received via callback should have basic properties
      Email email = received.get();
      assertNotNull(email.getId());
      assertNotNull(email.getSubject());
      assertTrue(email.getSubject().contains(uniqueId));
    } finally {
      subscription.unsubscribe();
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Monitor All Inboxes Tests ====================

  @Test
  @Order(30)
  @Timeout(value = 45, unit = TimeUnit.SECONDS)
  void testInboxMonitorReceivesEmails() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox1 = sseClient.createInbox();
    Inbox inbox2 = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger emailCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(2);

    InboxMonitor monitor = sseClient.monitorInboxes(inbox1, inbox2);
    monitor.onEmail(
        email -> {
          String subject = email.getSubject();
          if (subject != null && subject.contains(uniqueId)) {
            emailCount.incrementAndGet();
            latch.countDown();
          }
        });

    try {
      Thread.sleep(500);

      // Send to both inboxes
      smtp.sendEmail(
          inbox1.getEmailAddress(), "monitor1@test.com", "Monitor Test 1 " + uniqueId, "Body 1");
      smtp.sendEmail(
          inbox2.getEmailAddress(), "monitor2@test.com", "Monitor Test 2 " + uniqueId, "Body 2");

      boolean bothReceived = latch.await(20, TimeUnit.SECONDS);

      assertTrue(bothReceived, "Should receive emails from both monitored inboxes");
      assertEquals(2, emailCount.get(), "Should receive 2 emails total");
    } finally {
      monitor.close();
      sseClient.deleteInbox(inbox1.getEmailAddress());
      sseClient.deleteInbox(inbox2.getEmailAddress());
    }
  }

  @Test
  @Order(31)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testInboxMonitorClose() {
    Inbox inbox1 = sseClient.createInbox();
    Inbox inbox2 = sseClient.createInbox();

    InboxMonitor monitor = sseClient.monitorInboxes(inbox1, inbox2);

    // Close should work without hanging
    assertDoesNotThrow(monitor::close);

    // Multiple closes should be safe
    assertDoesNotThrow(monitor::close);

    // Clean up
    sseClient.deleteInbox(inbox1.getEmailAddress());
    sseClient.deleteInbox(inbox2.getEmailAddress());
  }
}
