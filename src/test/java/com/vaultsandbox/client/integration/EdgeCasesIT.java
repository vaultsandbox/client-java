package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.InboxMonitor;
import com.vaultsandbox.client.StrategyType;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

/** Integration tests for edge cases and error handling. */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EdgeCasesIT {

  private static VaultSandboxClient client;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    client =
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
    if (client != null) {
      try {
        client.deleteAllInboxes();
      } catch (Exception ignored) {
      }
      client.close();
    }
  }

  // ==================== Zero Timeout Tests ====================

  @Test
  @Order(1)
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testZeroTimeout() {
    Inbox inbox = client.createInbox();

    try {
      // Waiting with Duration.ZERO should return immediately (or nearly so)
      long start = System.currentTimeMillis();

      Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ZERO);

      long elapsed = System.currentTimeMillis() - start;

      // Should return null immediately without finding an email
      assertNull(email, "Should return null for zero timeout on empty inbox");
      assertTrue(elapsed < 2000, "Zero timeout should complete quickly (was " + elapsed + "ms)");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(2)
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testVeryShortTimeout() {
    Inbox inbox = client.createInbox();

    try {
      // Very short timeout (100ms)
      long start = System.currentTimeMillis();

      Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ofMillis(100));

      long elapsed = System.currentTimeMillis() - start;

      assertNull(email, "Should return null for very short timeout");
      assertTrue(elapsed < 3000, "Should complete within reasonable time");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Deleted Inbox Tests ====================

  @Test
  @Order(10)
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testDeletedInboxDuringWait() throws Exception {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    // Start waiting for email in background
    CompletableFuture<Email> future =
        CompletableFuture.supplyAsync(
            () -> inbox.awaitEmail(EmailFilter.any(), Duration.ofSeconds(15)));

    // Wait a moment, then delete the inbox
    Thread.sleep(1000);
    client.deleteInbox(email);

    // The wait should either:
    // 1. Throw an exception because the inbox was deleted
    // 2. Return null because the inbox no longer exists
    // 3. Complete with timeout
    try {
      Email result = future.get(10, TimeUnit.SECONDS);
      // If we get here without exception, result should be null
      assertNull(result, "Should return null after inbox deletion");
    } catch (ExecutionException e) {
      // Expected - operation failed due to deleted inbox
      assertNotNull(e.getCause(), "Should have a cause for the execution exception");
    } catch (TimeoutException e) {
      // Also acceptable - the future might still be running
      future.cancel(true);
    }
  }

  @Test
  @Order(11)
  void testOperationsOnDeletedInbox() {
    Inbox inbox = client.createInbox();
    String email = inbox.getEmailAddress();

    // Delete the inbox
    client.deleteInbox(email);

    // Subsequent operations should fail gracefully
    assertThrows(
        Exception.class, inbox::listEmails, "Should throw when listing emails on deleted inbox");
  }

  // ==================== Empty Inbox Monitor Tests ====================

  @Test
  @Order(20)
  void testMonitorNoInboxes() {
    // Monitoring zero inboxes should work (even if it does nothing)
    InboxMonitor monitor = client.monitorInboxes();

    assertNotNull(monitor, "Monitor should be created even with no inboxes");

    // Close should work without error
    assertDoesNotThrow(monitor::close);
  }

  @Test
  @Order(21)
  void testMonitorCloseIdempotent() {
    Inbox inbox = client.createInbox();

    try {
      InboxMonitor monitor = client.monitorInboxes(inbox);

      // Multiple closes should be safe
      assertDoesNotThrow(
          () -> {
            monitor.close();
            monitor.close();
            monitor.close();
          });
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  // ==================== Concurrent Operations Tests ====================

  @Test
  @Order(30)
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testConcurrentInboxCreationAndDeletion() throws Exception {
    int numInboxes = 5;
    CompletableFuture<?>[] createFutures = new CompletableFuture[numInboxes];

    // Create inboxes concurrently
    for (int i = 0; i < numInboxes; i++) {
      createFutures[i] = CompletableFuture.supplyAsync(() -> client.createInbox());
    }

    CompletableFuture.allOf(createFutures).join();

    // Get all created inboxes
    Inbox[] inboxes = new Inbox[numInboxes];
    for (int i = 0; i < numInboxes; i++) {
      inboxes[i] = (Inbox) ((CompletableFuture<?>) createFutures[i]).get();
      assertNotNull(inboxes[i]);
    }

    // Delete all concurrently
    CompletableFuture<?>[] deleteFutures = new CompletableFuture[numInboxes];
    for (int i = 0; i < numInboxes; i++) {
      final Inbox inbox = inboxes[i];
      deleteFutures[i] =
          CompletableFuture.runAsync(() -> client.deleteInbox(inbox.getEmailAddress()));
    }

    CompletableFuture.allOf(deleteFutures).join();
  }

  // ==================== Boundary Value Tests ====================

  @Test
  @Order(40)
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testNegativeTimeoutTreatedAsZero() {
    Inbox inbox = client.createInbox();

    try {
      // Negative duration should be handled gracefully
      // Implementation might treat it as zero or throw
      long start = System.currentTimeMillis();

      try {
        Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ofMillis(-100));
        long elapsed = System.currentTimeMillis() - start;
        // If no exception, should return quickly
        assertNull(email);
        assertTrue(elapsed < 5000, "Negative timeout should complete quickly");
      } catch (IllegalArgumentException e) {
        // Also acceptable - rejecting negative timeout
      }
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  void testEmptyEmailFilter() {
    Inbox inbox = client.createInbox();

    try {
      // Using EmailFilter.any() on empty inbox
      Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ofMillis(500));

      assertNull(email, "Empty inbox should return null even with any() filter");
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }
}
