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

  /**
   * Convenience method that returns true if the result is 'pass'.
   *
   * @return true if result equals "pass" (case-insensitive)
   */
  public boolean isVerified() {
    return "pass".equalsIgnoreCase(result);
  }
}
