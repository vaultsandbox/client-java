package com.vaultsandbox.client.model;

public class InboxData {
  private String emailAddress;
  private String expiresAt;
  private String inboxHash;
  private String serverSigPk;
  private Boolean emailAuth;
  private Boolean encrypted;

  public InboxData() {}

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public String getInboxHash() {
    return inboxHash;
  }

  public String getServerSigPk() {
    return serverSigPk;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void setInboxHash(String inboxHash) {
    this.inboxHash = inboxHash;
  }

  public void setServerSigPk(String serverSigPk) {
    this.serverSigPk = serverSigPk;
  }

  public Boolean getEmailAuth() {
    return emailAuth;
  }

  public void setEmailAuth(Boolean emailAuth) {
    this.emailAuth = emailAuth;
  }

  /**
   * Returns whether this inbox is encrypted.
   *
   * <p>When {@code true}, emails are end-to-end encrypted with ML-KEM and AES-256-GCM. When {@code
   * false}, emails are stored in plain text (Base64 encoded).
   *
   * @return {@code true} if encrypted, {@code false} if plain, {@code null} if not specified
   *     (defaults to {@code true} for backward compatibility)
   */
  public Boolean getEncrypted() {
    return encrypted;
  }

  public void setEncrypted(Boolean encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Returns whether this inbox uses encryption.
   *
   * <p>This is a convenience method that defaults to {@code true} for backward compatibility when
   * the encrypted field is not set.
   *
   * @return {@code true} if encrypted or not specified, {@code false} if explicitly plain
   */
  public boolean isEncrypted() {
    // Default to encrypted for backward compatibility
    return encrypted == null || encrypted;
  }
}
