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
}
