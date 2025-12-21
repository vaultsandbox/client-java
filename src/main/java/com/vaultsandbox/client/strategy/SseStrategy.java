package com.vaultsandbox.client.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.exception.SseException;
import com.vaultsandbox.client.exception.TimeoutException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

  // Failure state and callback for fallback support
  private volatile boolean permanentlyFailed = false;
  private volatile Consumer<SseException> failureCallback;

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

    // Notify failure callback (used by AutoStrategy for fallback)
    Consumer<SseException> callback = this.failureCallback;
    if (callback != null) {
      try {
        callback.accept(failure);
      } catch (Exception e) {
        log.warn("Failure callback error: {}", e.getMessage());
      }
    }
  }

  /**
   * Sets a callback to be invoked when SSE permanently fails.
   *
   * <p>This is used by AutoStrategy to trigger fallback to polling.
   *
   * @param callback the failure callback
   */
  public void setFailureCallback(Consumer<SseException> callback) {
    this.failureCallback = callback;
  }

  /**
   * Returns whether SSE has permanently failed.
   *
   * @return true if SSE failed after max reconnect attempts
   */
  public boolean isPermanentlyFailed() {
    return permanentlyFailed;
  }

  @Override
  public boolean isSupported() {
    // If we've already permanently failed, SSE is not supported
    if (permanentlyFailed) {
      return false;
    }

    // Test SSE connectivity with a quick HEAD request to the events endpoint
    Request request =
        new Request.Builder()
            .url(baseUrl + "/api/events")
            .header("X-API-Key", apiKey)
            .head()
            .build();

    try (Response response = client.newCall(request).execute()) {
      int code = response.code();
      // Accept 2xx, 3xx, and 405 (Method Not Allowed - means endpoint exists but HEAD not allowed)
      // Reject 4xx (except 405) and 5xx as indicators that SSE won't work
      if (code >= 200 && code < 400) {
        return true;
      }
      if (code == 405) {
        // HEAD not allowed, but endpoint exists - SSE likely supported
        return true;
      }
      log.debug("SSE endpoint returned {}, falling back to polling", code);
      return false;
    } catch (IOException e) {
      log.debug("SSE connectivity check failed: {}, falling back to polling", e.getMessage());
      return false;
    }
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
