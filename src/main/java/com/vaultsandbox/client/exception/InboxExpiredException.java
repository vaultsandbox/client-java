package com.vaultsandbox.client.exception;

import java.time.Instant;

/**
 * Thrown when an operation is attempted on an inbox that has expired.
 *
 * <p>Inboxes have a limited lifetime defined at creation time. Once expired, the inbox and all its
 * emails are no longer accessible. This exception is thrown client-side before making API calls to
 * provide immediate feedback.
 */
public class InboxExpiredException extends VaultSandboxException {
  private final String emailAddress;
  private final Instant expiredAt;

  /**
   * Creates a new InboxExpiredException.
   *
   * @param emailAddress the email address of the expired inbox
   * @param expiredAt when the inbox expired
   */
  public InboxExpiredException(String emailAddress, Instant expiredAt) {
    super(String.format("Inbox %s expired at %s", emailAddress, expiredAt));
    this.emailAddress = emailAddress;
    this.expiredAt = expiredAt;
  }

  /**
   * Returns the email address of the expired inbox.
   *
   * @return the inbox email address
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Returns when the inbox expired.
   *
   * @return the expiration time
   */
  public Instant getExpiredAt() {
    return expiredAt;
  }
}
