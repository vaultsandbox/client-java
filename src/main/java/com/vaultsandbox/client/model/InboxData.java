package com.vaultsandbox.client.model;

public class InboxData {
  private String emailAddress;
  private String expiresAt;
  private String inboxHash;
  private String serverSigPk;

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
}
