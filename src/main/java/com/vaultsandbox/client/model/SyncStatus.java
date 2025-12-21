package com.vaultsandbox.client.model;

public class SyncStatus {
  private String emailsHash;
  private int emailCount;
  private String lastUpdated;

  public SyncStatus() {}

  public String getEmailsHash() {
    return emailsHash;
  }

  public int getEmailCount() {
    return emailCount;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }
}
