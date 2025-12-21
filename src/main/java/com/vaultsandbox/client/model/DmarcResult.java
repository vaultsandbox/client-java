package com.vaultsandbox.client.model;

public class DmarcResult {
  private String result; // pass, fail, none
  private String domain;
  private String policy; // none, quarantine, reject

  public DmarcResult() {}

  public String getResult() {
    return result;
  }

  /**
   * Alias for getResult() - returns the DMARC check status.
   *
   * @return the DMARC status (pass, fail, none)
   */
  public String getStatus() {
    return result;
  }

  public String getDomain() {
    return domain;
  }

  public String getPolicy() {
    return policy;
  }
}
