package com.vaultsandbox.client.model;

public class ReverseDnsResult {
  private String result;
  private String ip;
  private String hostname;

  public ReverseDnsResult() {}

  /**
   * Returns the reverse DNS check result.
   *
   * @return the result status: "pass", "fail", "none", or "skipped"
   */
  public String getResult() {
    return result;
  }

  public String getIp() {
    return ip;
  }

  public String getHostname() {
    return hostname;
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
