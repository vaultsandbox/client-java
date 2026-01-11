package com.vaultsandbox.client.strategy;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.exception.TimeoutException;
import com.vaultsandbox.client.model.SyncStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Delivery strategy that polls for new emails at regular intervals. */
public class PollingStrategy implements DeliveryStrategy {
  private static final Logger log = LoggerFactory.getLogger(PollingStrategy.class);
  private final Duration initialInterval;
  private final Duration maxBackoff;
  private final double backoffMultiplier;
  private final double jitterFactor;

  private final Map<String, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> seenEmailIds = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;

  public PollingStrategy(ClientConfig config) {
    this.initialInterval = config.getPollInterval();
    this.maxBackoff = config.getMaxBackoff();
    this.backoffMultiplier = config.getBackoffMultiplier();
    this.jitterFactor = config.getJitterFactor();
    this.scheduler =
        Executors.newScheduledThreadPool(
            2,
            r -> {
              Thread t = new Thread(r, "polling-strategy");
              t.setDaemon(true);
              return t;
            });
  }

  @Override
  public Email waitForEmail(Inbox inbox, EmailFilter filter, Duration timeout)
      throws TimeoutException {
    return waitForEmail(inbox, filter, timeout, null);
  }

  @Override
  public Email waitForEmail(
      Inbox inbox, EmailFilter filter, Duration timeout, Duration pollInterval)
      throws TimeoutException {
    Duration effectiveInterval = pollInterval != null ? pollInterval : initialInterval;
    log.debug(
        "Waiting for email in inbox {} (timeout={}, pollInterval={})",
        inbox.getEmailAddress(),
        timeout,
        effectiveInterval);
    Instant deadline = Instant.now().plus(timeout);
    String lastHash = null;
    long currentBackoff = effectiveInterval.toMillis();
    Set<String> seenIds = new HashSet<>();

    // First, check existing emails - they might have arrived before we started waiting
    List<Email> existingEmails = inbox.listEmails();
    for (Email email : existingEmails) {
      if (filter.matches(email)) {
        log.debug("Found matching email in existing emails: {}", email.getId());
        // Fetch full email with content (listEmails returns metadata only)
        return inbox.getEmail(email.getId());
      }
      seenIds.add(email.getId());
    }

    while (Instant.now().isBefore(deadline)) {
      SyncStatus status = inbox.getSyncStatus();

      if (!Objects.equals(lastHash, status.getEmailsHash())) {
        // Hash changed - new emails may have arrived
        log.debug("Hash changed, checking for new emails");
        lastHash = status.getEmailsHash();
        currentBackoff = effectiveInterval.toMillis(); // Reset backoff

        List<Email> emails = inbox.listEmails();
        for (Email email : emails) {
          if (!seenIds.contains(email.getId())) {
            seenIds.add(email.getId());
            log.debug("New email found: {}", email.getId());
            if (filter.matches(email)) {
              log.debug("Email {} matches filter", email.getId());
              // Fetch full email with content (listEmails returns metadata only)
              return inbox.getEmail(email.getId());
            }
          }
        }
      } else {
        // No change - increase backoff
        currentBackoff =
            Math.min((long) (currentBackoff * backoffMultiplier), maxBackoff.toMillis());
      }

      // Add jitter to prevent thundering herd
      long jitter = (long) (Math.random() * jitterFactor * currentBackoff);
      long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
      long sleepTime = Math.min(currentBackoff + jitter, remainingMs);

      if (sleepTime > 0) {
        sleep(sleepTime);
      }
    }

    log.warn("Timeout waiting for email in inbox {}", inbox.getEmailAddress());
    throw new TimeoutException("Email not received within timeout");
  }

  @Override
  public Subscription subscribe(Inbox inbox, Consumer<Email> callback) {
    log.info(
        "Subscribing to inbox {} via polling (interval={})",
        inbox.getEmailAddress(),
        initialInterval);
    String key = inbox.getHash();
    AtomicReference<String> lastHash = new AtomicReference<>();
    Set<String> seen = seenEmailIds.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());

    // Initialize with existing emails
    List<Email> existingEmails = inbox.listEmails();
    for (Email email : existingEmails) {
      seen.add(email.getId());
    }
    if (!existingEmails.isEmpty()) {
      lastHash.set(inbox.getSyncStatus().getEmailsHash());
    }

    ScheduledFuture<?> task =
        scheduler.scheduleWithFixedDelay(
            () -> {
              try {
                SyncStatus status = inbox.getSyncStatus();
                if (!Objects.equals(lastHash.get(), status.getEmailsHash())) {
                  lastHash.set(status.getEmailsHash());
                  List<Email> emails = inbox.listEmails();
                  for (Email email : emails) {
                    if (seen.add(email.getId())) {
                      log.debug("New email detected via polling: {}", email.getId());
                      try {
                        callback.accept(email);
                      } catch (Exception e) {
                        log.warn("Callback error for email {}: {}", email.getId(), e.getMessage());
                      }
                    }
                  }
                }
              } catch (Exception e) {
                log.warn("Polling error: {}", e.getMessage());
              }
            },
            0,
            initialInterval.toMillis(),
            TimeUnit.MILLISECONDS);

    pollingTasks.put(key, task);

    return () -> {
      ScheduledFuture<?> t = pollingTasks.remove(key);
      if (t != null) {
        t.cancel(false);
      }
      seenEmailIds.remove(key);
    };
  }

  @Override
  public void close() {
    log.info("Closing polling strategy");
    pollingTasks.values().forEach(t -> t.cancel(false));
    pollingTasks.clear();
    seenEmailIds.clear();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
