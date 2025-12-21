package com.vaultsandbox.client;

import com.vaultsandbox.client.strategy.DeliveryStrategy;
import com.vaultsandbox.client.strategy.Subscription;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors multiple inboxes for incoming emails.
 *
 * <p>An InboxMonitor allows you to register callbacks that will be invoked when emails arrive in
 * any of the monitored inboxes. This is useful for testing scenarios involving multiple recipients.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (InboxMonitor monitor = client.monitorInboxes(inbox1, inbox2, inbox3)) {
 *     monitor.onEmail(email -> {
 *         System.out.println("Email to: " + email.getTo());
 *         System.out.println("Subject: " + email.getSubject());
 *     });
 *
 *     // Trigger emails to be sent...
 *     // Callbacks will be invoked as emails arrive
 * }
 * }</pre>
 *
 * @see VaultSandboxClient#monitorInboxes(Inbox...)
 */
public class InboxMonitor implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(InboxMonitor.class);
  private final List<Inbox> inboxes;
  private final DeliveryStrategy strategy;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private final Set<Consumer<Email>> callbacks = ConcurrentHashMap.newKeySet();

  InboxMonitor(List<Inbox> inboxes, DeliveryStrategy strategy) {
    this.inboxes = List.copyOf(inboxes);
    this.strategy = strategy;
  }

  /**
   * Registers a callback to be invoked when emails arrive.
   *
   * <p>The callback will be called for every email received by any of the monitored inboxes.
   * Exceptions thrown by the callback are caught and logged but do not affect other callbacks.
   *
   * @param callback the callback to invoke with received emails
   */
  public void onEmail(Consumer<Email> callback) {
    callbacks.add(callback);

    // Subscribe to all inboxes if not already
    if (subscriptions.isEmpty()) {
      synchronized (subscriptions) {
        if (subscriptions.isEmpty()) {
          for (Inbox inbox : inboxes) {
            Subscription sub =
                strategy.subscribe(
                    inbox,
                    email -> {
                      for (Consumer<Email> cb : callbacks) {
                        try {
                          cb.accept(email);
                        } catch (Exception e) {
                          log.warn("Email callback threw exception", e);
                        }
                      }
                    });
            subscriptions.add(sub);
          }
        }
      }
    }
  }

  /**
   * Removes a previously registered callback.
   *
   * @param callback the callback to remove
   */
  public void removeCallback(Consumer<Email> callback) {
    callbacks.remove(callback);
  }

  /**
   * Returns the list of monitored inboxes.
   *
   * @return an immutable list of monitored inboxes
   */
  public List<Inbox> getInboxes() {
    return List.copyOf(inboxes);
  }

  /**
   * Closes this monitor and releases resources.
   *
   * <p>Unsubscribes from all inbox notifications and removes all registered callbacks.
   */
  @Override
  public void close() {
    synchronized (subscriptions) {
      for (Subscription sub : subscriptions) {
        sub.unsubscribe();
      }
      subscriptions.clear();
    }
    callbacks.clear();
  }
}
