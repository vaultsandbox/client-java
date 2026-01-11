package com.vaultsandbox.client.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.crypto.CryptoUtils;
import com.vaultsandbox.client.exception.SseException;
import com.vaultsandbox.client.exception.TimeoutException;
import com.vaultsandbox.client.model.EmailMetadata;
import com.vaultsandbox.client.model.SyncStatus;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Delivery strategy using Server-Sent Events for real-time email notifications. */
public class SseStrategy implements DeliveryStrategy {
  private static final Logger log = LoggerFactory.getLogger(SseStrategy.class);
  private final OkHttpClient client;
  private final String baseUrl;
  private final String apiKey;
  private final Gson gson;

  private final Map<String, Set<Consumer<Email>>> subscriptions = new ConcurrentHashMap<>();
  private final Map<String, Inbox> inboxesByHash = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> seenEmailsByInbox = new ConcurrentHashMap<>();

  private volatile EventSource eventSource;
  private final Object connectionLock = new Object();

  // Reconnection config
  private final Duration reconnectInterval;
  private final int maxReconnectAttempts;
  private final double backoffMultiplier;

  private volatile int reconnectAttempts = 0;
  private ScheduledExecutorService reconnectScheduler;

  // Track pending futures for waitForEmail calls so we can complete them on failure
  private final Set<CompletableFuture<Email>> pendingFutures = ConcurrentHashMap.newKeySet();

  // Failure state
  private volatile boolean permanentlyFailed = false;

  public SseStrategy(OkHttpClient client, ClientConfig config) {
    this.client = client;
    this.baseUrl = config.getBaseUrl();
    this.apiKey = config.getApiKey();
    this.reconnectInterval = config.getSseReconnectInterval();
    this.maxReconnectAttempts = config.getSseMaxReconnectAttempts();
    this.backoffMultiplier = 2.0;
    this.gson = new GsonBuilder().create();
  }

  @Override
  public Email waitForEmail(Inbox inbox, EmailFilter filter, Duration timeout)
      throws TimeoutException {
    // Check if SSE has permanently failed
    if (permanentlyFailed) {
      throw new SseException("SSE connection permanently failed after max reconnect attempts");
    }

    // First check existing emails - they might have arrived before we started waiting
    List<Email> existingEmails = inbox.listEmails();
    for (Email email : existingEmails) {
      if (filter.matches(email)) {
        // Fetch full email with content
        return inbox.getEmail(email.getId());
      }
    }

    // Track seen email IDs to avoid duplicates
    Set<String> seenIds = ConcurrentHashMap.newKeySet();
    for (Email email : existingEmails) {
      seenIds.add(email.getId());
    }

    CompletableFuture<Email> future = new CompletableFuture<>();
    pendingFutures.add(future);

    Subscription sub =
        subscribe(
            inbox,
            email -> {
              if (!seenIds.contains(email.getId()) && filter.matches(email)) {
                seenIds.add(email.getId());
                future.complete(email);
              }
            });

    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new TimeoutException("Email not received within timeout");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TimeoutException("Wait interrupted", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SseException) {
        throw (SseException) cause;
      }
      throw new SseException("Error waiting for email", cause);
    } finally {
      pendingFutures.remove(future);
      sub.unsubscribe();
    }
  }

  @Override
  public Subscription subscribe(Inbox inbox, Consumer<Email> callback) {
    String hash = inbox.getHash();
    log.debug("Subscribing to inbox: {}", inbox.getEmailAddress());
    inboxesByHash.put(hash, inbox);
    subscriptions.computeIfAbsent(hash, k -> ConcurrentHashMap.newKeySet()).add(callback);

    // Initialize seen emails from current state (metadata only for efficiency)
    List<EmailMetadata> existingEmails = inbox.listEmailsMetadataOnly();
    Set<String> seen = seenEmailsByInbox.computeIfAbsent(hash, k -> ConcurrentHashMap.newKeySet());
    for (EmailMetadata email : existingEmails) {
      seen.add(email.getId());
    }

    reconnect();

    return () -> unsubscribe(hash, callback);
  }

  private void unsubscribe(String hash, Consumer<Email> callback) {
    Set<Consumer<Email>> callbacks = subscriptions.get(hash);
    if (callbacks != null) {
      callbacks.remove(callback);
      if (callbacks.isEmpty()) {
        log.debug("Unsubscribing from inbox hash: {}", hash);
        subscriptions.remove(hash);
        inboxesByHash.remove(hash);
        seenEmailsByInbox.remove(hash);
        reconnect(); // Reconnect with updated inbox list
      }
    }
  }

  private void reconnect() {
    synchronized (connectionLock) {
      if (eventSource != null) {
        eventSource.cancel();
        eventSource = null;
      }

      if (subscriptions.isEmpty()) {
        return;
      }

      connect();
    }
  }

  private void syncAllInboxes() {
    for (Map.Entry<String, Inbox> entry : inboxesByHash.entrySet()) {
      String hash = entry.getKey();
      Inbox inbox = entry.getValue();
      try {
        syncInbox(hash, inbox);
      } catch (Exception e) {
        log.warn("Sync failed for {}: {}", inbox.getEmailAddress(), e.getMessage());
      }
    }
  }

  private void syncInbox(String inboxHash, Inbox inbox) {
    // Step 1: Get metadata only (lightweight, no content)
    List<EmailMetadata> emailsMetadata = inbox.listEmailsMetadataOnly();
    List<String> emailIds =
        emailsMetadata.stream().map(EmailMetadata::getId).collect(Collectors.toList());

    // Step 2: Compute local hash from IDs
    String localHash = CryptoUtils.computeEmailsHash(emailIds);

    // Step 3: Get server hash
    SyncStatus status = inbox.getSyncStatus();

    // Step 4: Compare hashes
    if (Objects.equals(localHash, status.getEmailsHash())) {
      log.debug("Hashes match for {}, no sync needed", inbox.getEmailAddress());
      return; // No changes, skip sync
    }

    log.info("Hash mismatch for {}, syncing", inbox.getEmailAddress());

    // Step 5: Track seen emails
    Set<String> seen =
        seenEmailsByInbox.computeIfAbsent(inboxHash, k -> ConcurrentHashMap.newKeySet());
    Set<String> serverIds = emailIds.stream().collect(Collectors.toSet());

    // Step 6: Remove deleted emails from seen set
    seen.retainAll(serverIds);

    // Step 7: Find new emails and notify subscribers
    Set<Consumer<Email>> callbacks = subscriptions.get(inboxHash);
    if (callbacks != null) {
      for (EmailMetadata metadata : emailsMetadata) {
        if (seen.add(metadata.getId())) {
          // Only fetch full email content for NEW emails
          try {
            Email email = inbox.getEmail(metadata.getId());
            log.debug("New email from sync: {}", email.getId());
            for (Consumer<Email> callback : callbacks) {
              try {
                callback.accept(email);
              } catch (Exception e) {
                log.warn("Callback error: {}", e.getMessage());
              }
            }
          } catch (Exception e) {
            log.warn("Failed to fetch email {}: {}", metadata.getId(), e.getMessage());
          }
        }
      }
    }
  }

  private void connect() {
    String hashes = String.join(",", subscriptions.keySet());
    log.info("Opening SSE connection for {} inbox(es)", subscriptions.size());
    Request request =
        new Request.Builder()
            .url(baseUrl + "/api/events?inboxes=" + hashes)
            .header("X-API-Key", apiKey)
            .header("Accept", "text/event-stream")
            .build();

    eventSource =
        EventSources.createFactory(client)
            .newEventSource(
                request,
                new EventSourceListener() {
                  @Override
                  public void onOpen(EventSource es, Response response) {
                    log.info("SSE connection established");
                    reconnectAttempts = 0; // Reset on successful connection
                    // Trigger sync for all inboxes after reconnect
                    syncAllInboxes();
                  }

                  @Override
                  public void onEvent(EventSource es, String id, String type, String data) {
                    log.debug("SSE event received: type={}", type);
                    handleEvent(type, data);
                  }

                  @Override
                  public void onFailure(EventSource es, Throwable t, Response response) {
                    int code = response != null ? response.code() : 0;
                    log.warn(
                        "SSE connection failed: {} (code={})",
                        t != null ? t.getMessage() : "unknown",
                        code);
                    scheduleReconnect();
                  }

                  @Override
                  public void onClosed(EventSource es) {
                    log.info("SSE connection closed");
                    scheduleReconnect();
                  }
                });
  }

  private void handleEvent(String type, String data) {
    // Accept events with type "email", "message" (SSE default), or null (no event type specified)
    if (type != null && !"email".equals(type) && !"message".equals(type)) {
      log.debug("Ignoring SSE event with type: {}", type);
      return;
    }

    if (data == null || data.isBlank()) {
      log.debug("Ignoring SSE event with empty data");
      return;
    }

    log.debug("SSE event data: {}", data);

    try {
      SseEmailEvent event = gson.fromJson(data, SseEmailEvent.class);

      // Validate that this is actually an email event
      if (event.inboxHash == null || event.emailId == null) {
        log.debug(
            "Ignoring SSE event without inboxHash/emailId (parsed: inboxHash={}, emailId={})",
            event.inboxHash,
            event.emailId);
        return;
      }

      String inboxHash = event.inboxHash;

      Inbox inbox = inboxesByHash.get(inboxHash);
      if (inbox == null) {
        return;
      }

      // Track as seen - skip if already seen
      Set<String> seen =
          seenEmailsByInbox.computeIfAbsent(inboxHash, k -> ConcurrentHashMap.newKeySet());
      if (!seen.add(event.emailId)) {
        log.debug("Email {} already seen, skipping", event.emailId);
        return;
      }

      // Fetch and decrypt the email
      Email email = inbox.getEmail(event.emailId);

      // Notify all subscribers for this inbox
      Set<Consumer<Email>> callbacks = subscriptions.get(inboxHash);
      if (callbacks != null) {
        log.debug("Notifying {} subscriber(s) for email {}", callbacks.size(), event.emailId);
        for (Consumer<Email> callback : callbacks) {
          try {
            callback.accept(email);
          } catch (Exception e) {
            log.warn("Callback error for email {}: {}", event.emailId, e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.error("Error processing SSE event: {}", e.getMessage());
    }
  }

  private void scheduleReconnect() {
    if (subscriptions.isEmpty()) {
      return;
    }

    if (reconnectAttempts >= maxReconnectAttempts) {
      log.error("Max reconnection attempts ({}) reached, giving up", maxReconnectAttempts);
      handlePermanentFailure();
      return;
    }

    long delay =
        (long) (reconnectInterval.toMillis() * Math.pow(backoffMultiplier, reconnectAttempts));
    reconnectAttempts++;
    log.warn(
        "Scheduling SSE reconnect (attempt {}/{}) in {}ms",
        reconnectAttempts,
        maxReconnectAttempts,
        delay);

    synchronized (connectionLock) {
      if (reconnectScheduler == null) {
        reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(
                r -> {
                  Thread t = new Thread(r, "sse-reconnect");
                  t.setDaemon(true);
                  return t;
                });
      }
    }

    reconnectScheduler.schedule(this::reconnect, delay, TimeUnit.MILLISECONDS);
  }

  private void handlePermanentFailure() {
    permanentlyFailed = true;
    SseException failure =
        new SseException(
            "SSE connection permanently failed after "
                + maxReconnectAttempts
                + " reconnect attempts");

    // Complete all pending futures exceptionally
    for (CompletableFuture<Email> future : pendingFutures) {
      future.completeExceptionally(failure);
    }
    pendingFutures.clear();
  }

  @Override
  public void close() {
    log.info("Closing SSE strategy");
    synchronized (connectionLock) {
      if (eventSource != null) {
        eventSource.cancel();
        eventSource = null;
      }
      if (reconnectScheduler != null) {
        reconnectScheduler.shutdown();
        try {
          if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            reconnectScheduler.shutdownNow();
          }
        } catch (InterruptedException e) {
          reconnectScheduler.shutdownNow();
          Thread.currentThread().interrupt();
        }
        reconnectScheduler = null;
      }
      subscriptions.clear();
      inboxesByHash.clear();
      seenEmailsByInbox.clear();
    }
  }

  /** SSE event payload for email notifications. */
  private static final class SseEmailEvent {
    // Support multiple possible field names from server
    @com.google.gson.annotations.SerializedName(
        value = "inboxId",
        alternate = {"inbox_id", "inbox_hash", "inboxHash"})
    String inboxHash;

    @com.google.gson.annotations.SerializedName(
        value = "emailId",
        alternate = {"email_id"})
    String emailId;
  }
}
