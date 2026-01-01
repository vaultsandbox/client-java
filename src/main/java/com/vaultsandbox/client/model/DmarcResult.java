package com.vaultsandbox.client.model;

public class DmarcResult {
  private String result; // pass, fail, none
  private String policy; // none, quarantine, reject
  private Boolean aligned;
  private String domain;

  public DmarcResult() {}

  public String getResult() {
    return result;
  }

  public String getPolicy() {
    return policy;
  }

  public Boolean getAligned() {
    return aligned;
  }

  public boolean isAligned() {
    return aligned != null && aligned;
  }

  public String getDomain() {
    return domain;
  }
}
