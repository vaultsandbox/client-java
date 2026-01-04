package com.vaultsandbox.client.model;

/**
 * Lightweight representation of an email containing only metadata (no body content).
 *
 * <p>This class is returned by {@link com.vaultsandbox.client.Inbox#listEmailsMetadataOnly()} and
 * is useful when you only need basic email information without fetching the full content.
 */
public class EmailMetadata {
  private final String id;
  private final String from;
  private final String subject;
  private final String receivedAt;
  private final boolean isRead;

  public EmailMetadata(String id, String from, String subject, String receivedAt, boolean isRead) {
    this.id = id;
    this.from = from;
    this.subject = subject;
    this.receivedAt = receivedAt;
    this.isRead = isRead;
  }

  public String getId() {
    return id;
  }

  public String getFrom() {
    return from;
  }

  public String getSubject() {
    return subject;
  }

  public String getReceivedAt() {
    return receivedAt;
  }

  public boolean isRead() {
    return isRead;
  }
}
