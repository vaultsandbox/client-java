package com.vaultsandbox.client.model;

public class DkimResult {
  private String result; // pass, fail, none
  private String domain;
  private String selector;
  private String signature;

  public DkimResult() {}

  public String getResult() {
    return result;
  }

  public String getDomain() {
    return domain;
  }

  public String getSelector() {
    return selector;
  }

  public String getSignature() {
    return signature;
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
