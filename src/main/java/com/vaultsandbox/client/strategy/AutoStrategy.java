package com.vaultsandbox.client.strategy;

import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.exception.SseException;
import com.vaultsandbox.client.exception.TimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivery strategy that automatically falls back from SSE to polling.
 *
 * <p>This strategy attempts to use SSE for real-time email notifications, but transparently falls
 * back to polling if:
 *
 * <ul>
 *   <li>SSE is not supported (detected at initialization via connectivity check)
 *   <li>SSE connection permanently fails at runtime (after max reconnect attempts)
 * </ul>
 *
 * <p>Active subscriptions are automatically migrated to polling when fallback occurs.
 */
public class AutoStrategy implements DeliveryStrategy {
  private static final Logger log = LoggerFactory.getLogger(AutoStrategy.class);

  private final SseStrategy sseStrategy;
  private final PollingStrategy pollingStrategy;

  private volatile DeliveryStrategy activeStrategy;
  private final Object fallbackLock = new Object();

  // Track active subscriptions for migration during fallback
  private final Map<SubscriptionKey, SubscriptionInfo> activeSubscriptions =
      new ConcurrentHashMap<>();

  public AutoStrategy(SseStrategy sseStrategy, PollingStrategy pollingStrategy) {
    this.sseStrategy = sseStrategy;
    this.pollingStrategy = pollingStrategy;

    // Choose initial strategy based on SSE support
    if (sseStrategy.isSupported()) {
      log.info("SSE supported, using SSE strategy");
      this.activeStrategy = sseStrategy;

      // Set up failure callback for runtime fallback
      sseStrategy.setFailureCallback(this::handleSseFailure);
    } else {
      log.info("SSE not supported, using polling strategy");
      this.activeStrategy = pollingStrategy;
    }
  }

  private void handleSseFailure(SseException failure) {
    log.warn("SSE permanently failed, falling back to polling: {}", failure.getMessage());
    synchronized (fallbackLock) {
      if (activeStrategy == pollingStrategy) {
        return; // Already fell back
      }
      activeStrategy = pollingStrategy;

      // Migrate active subscriptions to polling
      for (Map.Entry<SubscriptionKey, SubscriptionInfo> entry : activeSubscriptions.entrySet()) {
        SubscriptionInfo info = entry.getValue();
        if (info.sseSubscription != null) {
          // Unsubscribe from SSE (already failed, but clean up)
          try {
            info.sseSubscription.unsubscribe();
          } catch (Exception e) {
            // Ignore cleanup errors
          }

          // Subscribe via polling
          info.pollingSubscription = pollingStrategy.subscribe(info.inbox, info.callback);
          log.debug("Migrated subscription for {} to polling", info.inbox.getEmailAddress());
        }
      }
    }
  }

  @Override
  public Email waitForEmail(Inbox inbox, EmailFilter filter, Duration timeout)
      throws TimeoutException {
    try {
      return activeStrategy.waitForEmail(inbox, filter, timeout);
    } catch (SseException e) {
      // SSE failed during wait - if we haven't already fallen back, do so now
      if (activeStrategy == sseStrategy && sseStrategy.isPermanentlyFailed()) {
        synchronized (fallbackLock) {
          if (activeStrategy == sseStrategy) {
            log.warn("SSE failed during waitForEmail, retrying with polling");
            activeStrategy = pollingStrategy;
          }
        }
        // Retry with polling
        return pollingStrategy.waitForEmail(inbox, filter, timeout);
      }
      throw e;
    }
  }

  @Override
  public Email waitForEmail(
      Inbox inbox, EmailFilter filter, Duration timeout, Duration pollInterval)
      throws TimeoutException {
    try {
      return activeStrategy.waitForEmail(inbox, filter, timeout, pollInterval);
    } catch (SseException e) {
      if (activeStrategy == sseStrategy && sseStrategy.isPermanentlyFailed()) {
        synchronized (fallbackLock) {
          if (activeStrategy == sseStrategy) {
            log.warn("SSE failed during waitForEmail, retrying with polling");
            activeStrategy = pollingStrategy;
          }
        }
        return pollingStrategy.waitForEmail(inbox, filter, timeout, pollInterval);
      }
      throw e;
    }
  }

  @Override
  public Subscription subscribe(Inbox inbox, Consumer<Email> callback) {
    SubscriptionKey key = new SubscriptionKey(inbox.getHash(), callback);
    SubscriptionInfo info = new SubscriptionInfo(inbox, callback);

    synchronized (fallbackLock) {
      if (activeStrategy == sseStrategy) {
        info.sseSubscription = sseStrategy.subscribe(inbox, callback);
      } else {
        info.pollingSubscription = pollingStrategy.subscribe(inbox, callback);
      }
      activeSubscriptions.put(key, info);
    }

    return () -> {
      SubscriptionInfo removed = activeSubscriptions.remove(key);
      if (removed != null) {
        if (removed.sseSubscription != null) {
          removed.sseSubscription.unsubscribe();
        }
        if (removed.pollingSubscription != null) {
          removed.pollingSubscription.unsubscribe();
        }
      }
    };
  }

  @Override
  public boolean isSupported() {
    return true; // AutoStrategy always works (falls back to polling)
  }

  @Override
  public void close() {
    log.info("Closing auto strategy");
    activeSubscriptions.clear();
    sseStrategy.close();
    pollingStrategy.close();
  }

  /**
   * Returns whether the strategy is currently using SSE or has fallen back to polling.
   *
   * @return true if using SSE, false if using polling
   */
  public boolean isUsingSse() {
    return activeStrategy == sseStrategy;
  }

  private static class SubscriptionKey {
    private final String inboxHash;
    private final Consumer<Email> callback;

    SubscriptionKey(String inboxHash, Consumer<Email> callback) {
      this.inboxHash = inboxHash;
      this.callback = callback;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SubscriptionKey)) {
        return false;
      }
      SubscriptionKey that = (SubscriptionKey) o;
      return inboxHash.equals(that.inboxHash) && callback == that.callback;
    }

    @Override
    public int hashCode() {
      return 31 * inboxHash.hashCode() + System.identityHashCode(callback);
    }
  }

  private static class SubscriptionInfo {
    final Inbox inbox;
    final Consumer<Email> callback;
    volatile Subscription sseSubscription;
    volatile Subscription pollingSubscription;

    SubscriptionInfo(Inbox inbox, Consumer<Email> callback) {
      this.inbox = inbox;
      this.callback = callback;
    }
  }
}
