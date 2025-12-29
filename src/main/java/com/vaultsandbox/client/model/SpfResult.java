package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class SpfResult {
  @SerializedName("status")
  private String result; // pass, fail, softfail, neutral, none, temperror, permerror

  private String domain;
  private String ip;
  private String info;

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

  /**
   * Returns the IP address of the sending server.
   *
   * @return the IP address being validated
   */
  public String getIp() {
    return ip;
  }

  /**
   * Returns additional information or explanation about the SPF check.
   *
   * @return additional info string
   */
  public String getInfo() {
    return info;
  }
}
