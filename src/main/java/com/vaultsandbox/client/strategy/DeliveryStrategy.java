package com.vaultsandbox.client.strategy;

import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.exception.TimeoutException;
import java.io.Closeable;
import java.time.Duration;
import java.util.function.Consumer;

/** Strategy for waiting for and subscribing to emails. */
public interface DeliveryStrategy extends Closeable {
  /**
   * Wait for an email matching the filter within timeout.
   *
   * @param inbox the inbox to wait for emails in
   * @param filter filter to match emails
   * @param timeout maximum time to wait
   * @return the matching email
   * @throws TimeoutException if no matching email arrives within timeout
   */
  Email waitForEmail(Inbox inbox, EmailFilter filter, Duration timeout) throws TimeoutException;

  /**
   * Wait for an email matching the filter within timeout with custom poll interval.
   *
   * @param inbox the inbox to wait for emails in
   * @param filter filter to match emails
   * @param timeout maximum time to wait
   * @param pollInterval custom poll interval (for polling strategies)
   * @return the matching email
   * @throws TimeoutException if no matching email arrives within timeout
   */
  default Email waitForEmail(
      Inbox inbox, EmailFilter filter, Duration timeout, Duration pollInterval)
      throws TimeoutException {
    return waitForEmail(inbox, filter, timeout);
  }

  /**
   * Subscribe to new emails for an inbox.
   *
   * @param inbox the inbox to subscribe to
   * @param callback called when new emails arrive
   * @return subscription handle to unsubscribe
   */
  Subscription subscribe(Inbox inbox, Consumer<Email> callback);

  /**
   * Check if this strategy supports the current environment.
   *
   * @return true if strategy is available
   */
  boolean isSupported();

  @Override
  void close();
}
