package com.vaultsandbox.client.model;

import java.util.List;

public class EmailData {
  private String id;
  private String inboxId;
  private String receivedAt;
  private boolean isRead;
  private EncryptedPayload encryptedMetadata;
  private EncryptedPayload encryptedParsed;

  // Plain (unencrypted) inbox fields - Base64-encoded JSON
  private String metadata;
  private String parsed;

  // Legacy fields for backward compatibility with test data
  private String from;
  private List<String> to;
  private String subject;
  private EncryptedPayload encrypted;

  public EmailData() {}

  public String getId() {
    return id;
  }

  public String getInboxId() {
    return inboxId;
  }

  public String getFrom() {
    return from;
  }

  public List<String> getTo() {
    return to;
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

  public EncryptedPayload getEncrypted() {
    // Return encryptedParsed first (new API), fall back to encrypted (legacy)
    return encryptedParsed != null ? encryptedParsed : encrypted;
  }

  public EncryptedPayload getEncryptedMetadata() {
    return encryptedMetadata;
  }

  public EncryptedPayload getEncryptedParsed() {
    return encryptedParsed;
  }

  /**
   * Returns the plain (Base64-encoded JSON) metadata for plain inboxes.
   *
   * @return the Base64-encoded metadata JSON, or {@code null} if this is an encrypted email
   */
  public String getMetadata() {
    return metadata;
  }

  /**
   * Returns the plain (Base64-encoded JSON) parsed content for plain inboxes.
   *
   * @return the Base64-encoded parsed content JSON, or {@code null} if this is an encrypted email
   */
  public String getParsed() {
    return parsed;
  }

  /**
   * Returns whether this email is from an encrypted inbox.
   *
   * <p>Use field presence to discriminate: encrypted emails have {@code encryptedMetadata}, while
   * plain emails have {@code metadata}.
   *
   * @return {@code true} if encrypted, {@code false} if plain
   */
  public boolean isEncrypted() {
    return encryptedMetadata != null;
  }
}
