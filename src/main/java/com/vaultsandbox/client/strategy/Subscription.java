package com.vaultsandbox.client.strategy;

/** Handle for managing an email subscription. */
@FunctionalInterface
public interface Subscription {
  /** Unsubscribe from email notifications. */
  void unsubscribe();
}
