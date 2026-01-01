package com.vaultsandbox.client.model;

public class ReverseDnsResult {
  private boolean verified;
  private String ip;
  private String hostname;

  public ReverseDnsResult() {}

  public boolean isVerified() {
    return verified;
  }

  public String getIp() {
    return ip;
  }

  public String getHostname() {
    return hostname;
  }
}
