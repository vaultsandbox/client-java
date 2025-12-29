package com.vaultsandbox.client.model;

public class ReverseDnsResult {
  private String status; // pass, fail, none
  private String ip;
  private String hostname;
  private String info;

  public ReverseDnsResult() {}

  /**
   * Returns the reverse DNS verification result.
   *
   * @return the status (pass, fail, none)
   */
  public String getStatus() {
    return status;
  }

  /**
   * Returns the IP address being validated.
   *
   * @return the IP address
   */
  public String getIp() {
    return ip;
  }

  /**
   * Returns the hostname from PTR record.
   *
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Returns additional information about the reverse DNS check.
   *
   * @return additional info string
   */
  public String getInfo() {
    return info;
  }

  /**
   * Convenience method to check if reverse DNS passed.
   *
   * @return true if status is "pass"
   */
  public boolean isValid() {
    return "pass".equalsIgnoreCase(status);
  }
}
