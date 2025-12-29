package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class DkimResult {
  @SerializedName("status")
  private String result; // pass, fail, none

  private String domain;
  private String selector;
  private String info;

  public DkimResult() {}

  public String getResult() {
    return result;
  }

  /**
   * Alias for getResult() - returns the DKIM verification status.
   *
   * @return the DKIM status (pass, fail, none)
   */
  public String getStatus() {
    return result;
  }

  public String getDomain() {
    return domain;
  }

  /**
   * Returns the DKIM selector (identifies the public key in DNS).
   *
   * @return the DKIM selector
   */
  public String getSelector() {
    return selector;
  }

  /**
   * Returns additional information about the DKIM verification.
   *
   * @return additional info string
   */
  public String getInfo() {
    return info;
  }
}
