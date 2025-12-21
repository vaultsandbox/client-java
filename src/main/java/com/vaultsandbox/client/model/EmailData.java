package com.vaultsandbox.client.model;

import java.util.List;

public class EmailData {
  private String id;
  private String inboxId;
  private String receivedAt;
  private boolean isRead;
  private EncryptedPayload encryptedMetadata;
  private EncryptedPayload encryptedParsed;

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
}
