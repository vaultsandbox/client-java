package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.SmtpTestHelper;
import com.vaultsandbox.client.StrategyType;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.strategy.EmailFilter;
import com.vaultsandbox.client.strategy.Subscription;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests for polling strategy behavior. Tests timeout behavior, custom intervals,
 * concurrent polling, and subscription management.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PollingStrategyIT {

  private static VaultSandboxClient pollingClient;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    pollingClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.POLLING)
                .pollInterval(Duration.ofMillis(500))
                .build());
  }

  @AfterAll
  static void tearDown() {
    if (pollingClient != null) {
      pollingClient.close();
    }
  }

  // ==================== Configuration Tests ====================

  @Test
  @Order(1)
  void testPollingClientCreation() {
    assertNotNull(pollingClient);
    assertEquals(StrategyType.POLLING, pollingClient.getConfig().getStrategy());
  }

  @Test
  @Order(2)
  void testCustomPollIntervalConfiguration() {
    VaultSandboxClient customClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.POLLING)
                .pollInterval(Duration.ofSeconds(2))
                .build());

    try {
      assertNotNull(customClient);
      assertEquals(Duration.ofSeconds(2), customClient.getConfig().getPollInterval());
    } finally {
      customClient.close();
    }
  }

  // ==================== Timeout Behavior Tests ====================

  @Test
  @Order(10)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testPollingTimeout() {
    Inbox inbox = pollingClient.createInbox();

    try {
      long start = System.currentTimeMillis();

      // Wait for email that won't arrive
      Email email =
          inbox.awaitEmail(
              EmailFilter.builder().subject("nonexistent-xyz-never-arrives").build(),
              Duration.ofSeconds(3));

      long elapsed = System.currentTimeMillis() - start;

      assertNull(email, "Should return null on timeout");
      assertTrue(elapsed >= 2500, "Should wait approximately 3 seconds (was " + elapsed + "ms)");
      assertTrue(elapsed <= 5000, "Should not wait too long past timeout (was " + elapsed + "ms)");
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(11)
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testPollingTimeoutWithShortDuration() {
    Inbox inbox = pollingClient.createInbox();

    try {
      long start = System.currentTimeMillis();

      Email email =
          inbox.awaitEmail(
              EmailFilter.builder().subject("short-timeout").build(), Duration.ofSeconds(1));

      long elapsed = System.currentTimeMillis() - start;

      assertNull(email);
      assertTrue(elapsed >= 900, "Should wait at least 1 second");
      assertTrue(elapsed <= 3000, "Should not wait too long");
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Concurrent Polling Tests ====================

  @Test
  @Order(20)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testConcurrentPolling() throws Exception {
    int numInboxes = 3;
    List<Inbox> inboxes = new ArrayList<>();
    List<CompletableFuture<Email>> futures = new ArrayList<>();

    try {
      // Create multiple inboxes
      for (int i = 0; i < numInboxes; i++) {
        inboxes.add(pollingClient.createInbox());
      }

      // Start polling all concurrently
      for (Inbox inbox : inboxes) {
        CompletableFuture<Email> future =
            CompletableFuture.supplyAsync(
                () ->
                    inbox.awaitEmail(
                        EmailFilter.builder().subject("concurrent-never-arrives").build(),
                        Duration.ofSeconds(3)));
        futures.add(future);
      }

      // Wait for all to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // All should return null (timeout)
      for (CompletableFuture<Email> future : futures) {
        assertNull(future.get(), "All concurrent polls should timeout");
      }
    } finally {
      // Clean up
      for (Inbox inbox : inboxes) {
        try {
          pollingClient.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }
  }

  @Test
  @Order(21)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testConcurrentPollingWithRealEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox1 = pollingClient.createInbox();
    Inbox inbox2 = pollingClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    try {
      // Start polling both inboxes concurrently
      CompletableFuture<Email> future1 =
          CompletableFuture.supplyAsync(
              () ->
                  inbox1.awaitEmail(
                      EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(15)));

      CompletableFuture<Email> future2 =
          CompletableFuture.supplyAsync(
              () ->
                  inbox2.awaitEmail(
                      EmailFilter.builder().subject("never-arrives").build(),
                      Duration.ofSeconds(5)));

      // Wait a moment, then send email to first inbox only
      Thread.sleep(1000);
      smtp.sendEmail(
          inbox1.getEmailAddress(), "concurrent@test.com", "Concurrent " + uniqueId, "Body");

      // First should receive, second should timeout
      Email email1 = future1.get(20, TimeUnit.SECONDS);
      Email email2 = future2.get(10, TimeUnit.SECONDS);

      assertNotNull(email1, "First inbox should receive email");
      assertNull(email2, "Second inbox should timeout");
    } finally {
      pollingClient.deleteInbox(inbox1.getEmailAddress());
      pollingClient.deleteInbox(inbox2.getEmailAddress());
    }
  }

  // ==================== Subscription Management Tests ====================

  @Test
  @Order(30)
  void testPollingSubscription() {
    Inbox inbox = pollingClient.createInbox();
    AtomicInteger callCount = new AtomicInteger(0);

    try {
      Subscription subscription = inbox.onNewEmail(email -> callCount.incrementAndGet());

      assertNotNull(subscription, "Subscription should be created");

      // Unsubscribe
      subscription.unsubscribe();
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(31)
  void testPollingUnsubscribeIdempotent() {
    Inbox inbox = pollingClient.createInbox();

    try {
      Subscription subscription = inbox.onNewEmail(email -> {});

      // Multiple unsubscribes should not throw
      assertDoesNotThrow(
          () -> {
            subscription.unsubscribe();
            subscription.unsubscribe();
            subscription.unsubscribe();
          });
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(32)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testPollingClientClose() {
    VaultSandboxClient tempClient =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .strategy(StrategyType.POLLING)
                .build());

    Inbox inbox = tempClient.createInbox();
    inbox.onNewEmail(email -> {});

    // Close should complete without hanging
    assertDoesNotThrow(tempClient::close);
  }

  // ==================== Real Email with Polling Tests ====================

  @Test
  @Order(40)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testPollingReceivesRealEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = pollingClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

    try {
      // Send email
      smtp.sendEmail(
          inbox.getEmailAddress(), "polling@test.com", "Polling Test " + uniqueId, "Body");

      // Use polling to receive
      Email email =
          inbox.awaitEmail(EmailFilter.builder().subject(uniqueId).build(), Duration.ofSeconds(20));

      assertNotNull(email, "Should receive email via polling");
      assertNotNull(email.getSubject());
      assertTrue(email.getSubject().contains(uniqueId));
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testPollingCallbackWithRealEmail() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = pollingClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger callCount = new AtomicInteger(0);

    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              String subject = email.getSubject();
              if (subject != null && subject.contains(uniqueId)) {
                callCount.incrementAndGet();
                latch.countDown();
              }
            });

    try {
      // Small delay to ensure subscription is active
      Thread.sleep(300);

      // Send email
      smtp.sendEmail(
          inbox.getEmailAddress(), "callback@test.com", "Callback Test " + uniqueId, "Body");

      // Wait for callback
      boolean received = latch.await(15, TimeUnit.SECONDS);

      assertTrue(received, "Callback should be invoked");
      assertEquals(1, callCount.get(), "Callback should be called exactly once");
    } finally {
      subscription.unsubscribe();
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(42)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testUnsubscribeStopsCallback() throws Exception {
    assumeTrue(SmtpTestHelper.isAvailable(), "SMTP not available");

    Inbox inbox = pollingClient.createInbox();
    SmtpTestHelper smtp = new SmtpTestHelper();

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger callCount = new AtomicInteger(0);

    Subscription subscription =
        inbox.onNewEmail(
            email -> {
              callCount.incrementAndGet();
            });

    try {
      // Unsubscribe immediately
      subscription.unsubscribe();

      // Wait a moment
      Thread.sleep(300);

      // Send email after unsubscribe
      smtp.sendEmail(inbox.getEmailAddress(), "unsub@test.com", "After Unsub " + uniqueId, "Body");

      // Wait to ensure polling would have picked it up if still active
      Thread.sleep(1500);

      // Callback should not have been called (or only for emails before unsubscribe)
      int countAfterUnsub = callCount.get();

      // Wait more and verify count doesn't increase
      Thread.sleep(1000);
      assertEquals(countAfterUnsub, callCount.get(), "Count should not increase after unsubscribe");
    } finally {
      pollingClient.deleteInbox(inbox.getEmailAddress());
    }
  }
}
