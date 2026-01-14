package com.vaultsandbox.client.model;

public class SpfResult {
  private String result; // pass, fail, softfail, neutral, none, temperror, permerror
  private String domain;
  private String ip;
  private String details;

  public SpfResult() {}

  public String getResult() {
    return result;
  }

  public String getDomain() {
    return domain;
  }

  public String getIp() {
    return ip;
  }

  public String getDetails() {
    return details;
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
