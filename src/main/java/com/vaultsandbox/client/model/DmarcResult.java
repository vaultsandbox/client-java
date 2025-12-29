package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class DmarcResult {
  @SerializedName("status")
  private String result; // pass, fail, none

  private String domain;
  private String policy; // none, quarantine, reject
  private Boolean aligned;
  private String info;

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

  /**
   * Returns whether SPF/DKIM results align with the From header domain.
   *
   * @return true if aligned, false if not, null if not available
   */
  public Boolean getAligned() {
    return aligned;
  }

  /**
   * Returns whether SPF/DKIM results align with the From header domain.
   *
   * @return true if aligned, false otherwise (defaults to false if null)
   */
  public boolean isAligned() {
    return aligned != null && aligned;
  }

  /**
   * Returns additional information about the DMARC check.
   *
   * @return additional info string
   */
  public String getInfo() {
    return info;
  }
}
