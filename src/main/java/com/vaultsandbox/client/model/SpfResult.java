package com.vaultsandbox.client.model;

public class SpfResult {
  private String result; // pass, fail, softfail, neutral, none, temperror, permerror
  private String domain;

  public SpfResult() {}

  public String getResult() {
    return result;
  }

  /**
   * Alias for getResult() - returns the SPF check status.
   *
   * @return the SPF status (pass, fail, softfail, neutral, none, temperror, permerror)
   */
  public String getStatus() {
    return result;
  }

  public String getDomain() {
    return domain;
  }
}
