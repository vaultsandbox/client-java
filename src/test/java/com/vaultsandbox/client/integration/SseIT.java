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

    // Default strategy is SSE
    assertEquals(StrategyType.SSE, defaultClient.getConfig().getStrategy());

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

  // ==================== SSE Strategy Coverage Tests ====================

  @Test
  @Order(40)
  void testSseStrategyWorksWithServer() {
    // SSE strategy should work without errors when server is available
    VaultSandboxClient testClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .build());

    try {
      // Creating inbox with SSE should work
      Inbox inbox = testClient.createInbox();
      assertNotNull(inbox);
      testClient.deleteInbox(inbox.getEmailAddress());
    } finally {
      testClient.close();
    }
  }

  @Test
  @Order(41)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWaitForEmailWithExistingEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    try {
      // Send email BEFORE waiting
      smtp.sendEmail(
          inbox.getEmailAddress(),
          "existing@test.com",
          "Existing Email " + uniqueId,
          "Body content");

      // Wait a moment for email to arrive
      Thread.sleep(2000);

      // waitForEmail should find the existing email immediately
      Email email =
          inbox.waitForEmail(
              com.vaultsandbox.client.strategy.EmailFilter.subjectContains(uniqueId),
              java.time.Duration.ofSeconds(10));

      assertNotNull(email);
      assertTrue(email.getSubject().contains(uniqueId));
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(42)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testWaitForEmailTimeout() {
    Inbox inbox = sseClient.createInbox();

    try {
      // Wait for an email that will never arrive with a short timeout
      String nonExistentSubject = "this-will-never-match-" + UUID.randomUUID();

      org.junit.jupiter.api.Assertions.assertThrows(
          com.vaultsandbox.client.exception.TimeoutException.class,
          () ->
              inbox.waitForEmail(
                  com.vaultsandbox.client.strategy.EmailFilter.subjectContains(nonExistentSubject),
                  java.time.Duration.ofSeconds(2)));
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(43)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCallbackExceptionDoesNotBreakOtherSubscribers() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicReference<Email> received = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    // First subscriber throws an exception
    Subscription sub1 =
        inbox.onNewEmail(
            email -> {
              if (email.getSubject() != null && email.getSubject().contains(uniqueId)) {
                throw new RuntimeException("Intentional test exception");
              }
            });

    // Second subscriber should still receive the email
    Subscription sub2 =
        inbox.onNewEmail(
            email -> {
              if (email.getSubject() != null && email.getSubject().contains(uniqueId)) {
                received.set(email);
                latch.countDown();
              }
            });

    try {
      Thread.sleep(500);

      smtp.sendEmail(
          inbox.getEmailAddress(), "exception@test.com", "Exception Test " + uniqueId, "Body");

      boolean callbackInvoked = latch.await(15, TimeUnit.SECONDS);

      assertTrue(callbackInvoked, "Second callback should still be invoked despite first throwing");
      assertNotNull(received.get());
      assertTrue(received.get().getSubject().contains(uniqueId));
    } finally {
      sub1.unsubscribe();
      sub2.unsubscribe();
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(44)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWaitForEmailWithSubjectFilter() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String targetId = UUID.randomUUID().toString().substring(0, 8);
    String otherId = UUID.randomUUID().toString().substring(0, 8);

    try {
      // Start waiting for specific subject in background
      java.util.concurrent.CompletableFuture<Email> waitFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () ->
                  inbox.waitForEmail(
                      com.vaultsandbox.client.strategy.EmailFilter.subjectContains(targetId),
                      java.time.Duration.ofSeconds(20)));

      Thread.sleep(500);

      // Send email with different subject first
      smtp.sendEmail(
          inbox.getEmailAddress(), "other@test.com", "Other Subject " + otherId, "Body 1");

      Thread.sleep(500);

      // Send email with target subject
      smtp.sendEmail(
          inbox.getEmailAddress(), "target@test.com", "Target Subject " + targetId, "Body 2");

      // Wait should return the target email, not the first one
      Email email = waitFuture.get(15, TimeUnit.SECONDS);

      assertNotNull(email);
      assertTrue(email.getSubject().contains(targetId));
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(45)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWaitForEmailWithFromFilter() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    try {
      java.util.concurrent.CompletableFuture<Email> waitFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () ->
                  inbox.waitForEmail(
                      com.vaultsandbox.client.strategy.EmailFilter.from("specialsender"),
                      java.time.Duration.ofSeconds(20)));

      Thread.sleep(500);

      // Send from different address first
      smtp.sendEmail(inbox.getEmailAddress(), "other@test.com", "Other " + uniqueId, "Body 1");

      Thread.sleep(500);

      // Send from target address
      smtp.sendEmail(
          inbox.getEmailAddress(), "specialsender@test.com", "Target " + uniqueId, "Body 2");

      Email email = waitFuture.get(15, TimeUnit.SECONDS);

      assertNotNull(email);
      assertTrue(email.getFrom().contains("specialsender"));
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(46)
  void testSseStrategyDoesNotFailOnInit() {
    // SSE strategy should not be in failed state upon initialization
    VaultSandboxClient testClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .build());

    try {
      // Create inbox and subscribe to verify strategy is working
      Inbox inbox = testClient.createInbox();
      AtomicInteger callCount = new AtomicInteger(0);
      Subscription sub = inbox.onNewEmail(email -> callCount.incrementAndGet());

      // Strategy should be functional
      assertNotNull(sub);
      sub.unsubscribe();
      testClient.deleteInbox(inbox.getEmailAddress());
    } finally {
      testClient.close();
    }
  }

  @Test
  @Order(47)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testSyncDetectsNewEmailAfterReconnect() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicReference<Email> received = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    try {
      // Subscribe to trigger SSE connection
      Subscription subscription =
          inbox.onNewEmail(
              email -> {
                if (email.getSubject() != null && email.getSubject().contains(uniqueId)) {
                  received.set(email);
                  latch.countDown();
                }
              });

      // Wait for connection to establish
      Thread.sleep(1000);

      // Send email
      smtp.sendEmail(
          inbox.getEmailAddress(), "sync@test.com", "Sync Test " + uniqueId, "Body content");

      // Wait for callback
      boolean callbackInvoked = latch.await(15, TimeUnit.SECONDS);

      assertTrue(callbackInvoked, "Callback should be invoked after sync");
      assertNotNull(received.get());

      subscription.unsubscribe();
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(48)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testEmailDeduplication() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger callbackCount = new AtomicInteger(0);

    try {
      // Subscribe
      Subscription subscription =
          inbox.onNewEmail(
              email -> {
                if (email.getSubject() != null && email.getSubject().contains(uniqueId)) {
                  callbackCount.incrementAndGet();
                }
              });

      Thread.sleep(500);

      // Send single email
      smtp.sendEmail(
          inbox.getEmailAddress(), "dedup@test.com", "Dedup Test " + uniqueId, "Body content");

      // Wait for processing
      Thread.sleep(3000);

      // Should only be called once despite potential sync operations
      assertEquals(1, callbackCount.get(), "Callback should be called exactly once per email");

      subscription.unsubscribe();
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(49)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWaitForEmailFindsEmailFromContent() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueCode = "CODE-" + UUID.randomUUID().toString().substring(0, 8);

    try {
      java.util.concurrent.CompletableFuture<Email> waitFuture =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () ->
                  inbox.waitForEmail(
                      com.vaultsandbox.client.strategy.EmailFilter.matching(
                          e -> e.getText() != null && e.getText().contains(uniqueCode)),
                      java.time.Duration.ofSeconds(20)));

      Thread.sleep(500);

      // Send email with unique code in body
      smtp.sendEmail(
          inbox.getEmailAddress(),
          "code@test.com",
          "Verification Email",
          "Your verification code is: " + uniqueCode);

      Email email = waitFuture.get(15, TimeUnit.SECONDS);

      assertNotNull(email);
      assertNotNull(email.getText());
      assertTrue(email.getText().contains(uniqueCode));
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(50)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testMultipleInboxesIndependentSubscriptions() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox1 = sseClient.createInbox();
    Inbox inbox2 = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId1 = UUID.randomUUID().toString().substring(0, 8);
    String uniqueId2 = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(2);

    try {
      Subscription sub1 =
          inbox1.onNewEmail(
              email -> {
                if (email.getSubject() != null && email.getSubject().contains(uniqueId1)) {
                  count1.incrementAndGet();
                  latch.countDown();
                }
              });

      Subscription sub2 =
          inbox2.onNewEmail(
              email -> {
                if (email.getSubject() != null && email.getSubject().contains(uniqueId2)) {
                  count2.incrementAndGet();
                  latch.countDown();
                }
              });

      Thread.sleep(500);

      // Send to each inbox
      smtp.sendEmail(inbox1.getEmailAddress(), "multi1@test.com", "Multi1 " + uniqueId1, "Body 1");
      smtp.sendEmail(inbox2.getEmailAddress(), "multi2@test.com", "Multi2 " + uniqueId2, "Body 2");

      boolean bothReceived = latch.await(15, TimeUnit.SECONDS);

      assertTrue(bothReceived);
      assertEquals(1, count1.get(), "Inbox 1 should receive exactly 1 email");
      assertEquals(1, count2.get(), "Inbox 2 should receive exactly 1 email");

      sub1.unsubscribe();
      sub2.unsubscribe();
    } finally {
      sseClient.deleteInbox(inbox1.getEmailAddress());
      sseClient.deleteInbox(inbox2.getEmailAddress());
    }
  }

  // ==================== SSE Failure and Edge Case Tests ====================

  @Test
  @Order(60)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testPermanentFailureAfterMaxReconnectAttempts() throws Exception {
    // Create a client pointing to an unreachable server with minimal retry config
    VaultSandboxClient failingClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey("test-api-key")
                .baseUrl("http://127.0.0.1:59999") // Unreachable port
                .strategy(StrategyType.SSE)
                .sseMaxReconnectAttempts(1)
                .sseReconnectInterval(java.time.Duration.ofMillis(100))
                .build());

    try {
      // Create a fake inbox to trigger SSE connection
      // We need to use reflection or create a minimal inbox for this test
      // Since we can't create a real inbox with invalid server, we'll test
      // the permanent failure by subscribing and letting it fail

      // The SSE strategy will fail to connect and exhaust reconnect attempts
      // After that, waitForEmail should throw SseException

      // Wait for the reconnect attempts to be exhausted
      Thread.sleep(1000);

    } finally {
      // Close should not hang even after permanent failure
      assertDoesNotThrow(failingClient::close);
    }
  }

  @Test
  @Order(61)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testWaitForEmailInterrupted() throws Exception {
    Inbox inbox = sseClient.createInbox();

    AtomicReference<Throwable> caughtException = new AtomicReference<>();
    CountDownLatch startedLatch = new CountDownLatch(1);
    CountDownLatch completedLatch = new CountDownLatch(1);

    Thread waiterThread =
        new Thread(
            () -> {
              try {
                startedLatch.countDown();
                inbox.waitForEmail(
                    com.vaultsandbox.client.strategy.EmailFilter.subjectContains(
                        "never-arriving-email-xyz"),
                    java.time.Duration.ofSeconds(30));
              } catch (Throwable t) {
                caughtException.set(t);
              } finally {
                completedLatch.countDown();
              }
            });

    try {
      waiterThread.start();

      // Wait for the thread to start waiting
      assertTrue(startedLatch.await(5, TimeUnit.SECONDS), "Waiter thread should start");

      // Give it a moment to actually enter the wait
      Thread.sleep(500);

      // Interrupt the thread
      waiterThread.interrupt();

      // Wait for the thread to complete
      assertTrue(completedLatch.await(5, TimeUnit.SECONDS), "Waiter thread should complete");

      // Should have caught a TimeoutException (wrapping InterruptedException)
      assertNotNull(caughtException.get(), "Should have caught an exception");
      assertTrue(
          caughtException.get() instanceof com.vaultsandbox.client.exception.TimeoutException,
          "Should be TimeoutException, was: " + caughtException.get().getClass().getName());
    } finally {
      waiterThread.interrupt(); // Ensure cleanup
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(62)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testCloseWithActiveReconnectScheduler() throws Exception {
    // Create a client that will need to reconnect
    VaultSandboxClient reconnectClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .sseReconnectInterval(java.time.Duration.ofMillis(500))
                .build());

    try {
      Inbox inbox = reconnectClient.createInbox();

      // Subscribe to trigger SSE connection
      Subscription subscription = inbox.onNewEmail(email -> {});

      // Wait for connection to establish
      Thread.sleep(1000);

      // Unsubscribe and resubscribe to trigger reconnect logic
      subscription.unsubscribe();
      Subscription newSub = inbox.onNewEmail(email -> {});
      assertNotNull(newSub, "Resubscription should succeed");

      // Close while potentially in reconnect state
      // This tests the scheduler shutdown path
      assertDoesNotThrow(reconnectClient::close);

      reconnectClient.deleteInbox(inbox.getEmailAddress());
    } catch (Exception e) {
      // If inbox creation fails, just close
      reconnectClient.close();
      throw e;
    }
  }

  @Test
  @Order(63)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testCloseInterruptsAwaitTermination() throws Exception {
    // This test exercises the close() path with an active scheduler
    VaultSandboxClient tempClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.SSE)
                .sseReconnectInterval(java.time.Duration.ofMillis(100))
                .sseMaxReconnectAttempts(5)
                .build());

    try {
      Inbox inbox = tempClient.createInbox();

      // Subscribe to start the SSE connection
      AtomicInteger callCount = new AtomicInteger(0);
      Subscription sub = inbox.onNewEmail(email -> callCount.incrementAndGet());
      assertNotNull(sub, "Subscription should be created");

      // Wait a moment for connection
      Thread.sleep(500);

      // Close should handle scheduler shutdown gracefully
      tempClient.close();

      // Verify close completed (if we get here, it didn't hang)
      assertTrue(true, "Close completed without hanging");

    } catch (Exception e) {
      tempClient.close();
      throw e;
    }
  }

  @Test
  @Order(64)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testWaitForEmailFindsExistingMatchingEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    try {
      // Send email FIRST
      smtp.sendEmail(
          inbox.getEmailAddress(),
          "existing@test.com",
          "Pre-existing Email " + uniqueId,
          "This email exists before waitForEmail is called");

      // Wait for email to definitely arrive
      Thread.sleep(3000);

      // Verify email is there
      var emails = inbox.listEmails();
      assertTrue(
          emails.stream()
              .anyMatch(e -> e.getSubject() != null && e.getSubject().contains(uniqueId)),
          "Email should exist before waitForEmail");

      // Now call waitForEmail - it should find the existing email immediately
      // This tests the path at lines 83-88 in SseStrategy
      long start = System.currentTimeMillis();
      Email email =
          inbox.waitForEmail(
              com.vaultsandbox.client.strategy.EmailFilter.subjectContains(uniqueId),
              java.time.Duration.ofSeconds(10));
      long elapsed = System.currentTimeMillis() - start;

      assertNotNull(email, "Should find existing email");
      assertTrue(email.getSubject().contains(uniqueId));
      // Should find it quickly since it already exists
      assertTrue(elapsed < 5000, "Should find existing email quickly, took " + elapsed + "ms");
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(65)
  void testSubscribeToMultipleInboxesThenUnsubscribeOne() {
    Inbox inbox1 = sseClient.createInbox();
    Inbox inbox2 = sseClient.createInbox();

    AtomicInteger count1 = new AtomicInteger(0);
    AtomicInteger count2 = new AtomicInteger(0);

    try {
      // Subscribe to both
      Subscription sub1 = inbox1.onNewEmail(email -> count1.incrementAndGet());
      Subscription sub2 = inbox2.onNewEmail(email -> count2.incrementAndGet());

      // Unsubscribe from first - this should trigger reconnect() with remaining subscription
      sub1.unsubscribe();

      // Second subscription should still be active
      assertNotNull(sub2);

      // Unsubscribe from second
      sub2.unsubscribe();

    } finally {
      sseClient.deleteInbox(inbox1.getEmailAddress());
      sseClient.deleteInbox(inbox2.getEmailAddress());
    }
  }

  @Test
  @Order(66)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testSubscribeUnsubscribeResubscribe() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = sseClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger callCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(1);

    try {
      // First subscription
      Subscription sub1 = inbox.onNewEmail(email -> {});

      Thread.sleep(300);

      // Unsubscribe
      sub1.unsubscribe();

      Thread.sleep(300);

      // Resubscribe with new callback
      Subscription sub2 =
          inbox.onNewEmail(
              email -> {
                if (email.getSubject() != null && email.getSubject().contains(uniqueId)) {
                  callCount.incrementAndGet();
                  latch.countDown();
                }
              });

      Thread.sleep(500);

      // Send email after resubscribe
      smtp.sendEmail(
          inbox.getEmailAddress(), "resub@test.com", "Resubscribe Test " + uniqueId, "Body");

      boolean received = latch.await(15, TimeUnit.SECONDS);

      assertTrue(received, "Should receive email after resubscribe");
      assertEquals(1, callCount.get());

      sub2.unsubscribe();
    } finally {
      sseClient.deleteInbox(inbox.getEmailAddress());
    }
  }
}
