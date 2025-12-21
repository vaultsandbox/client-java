package com.vaultsandbox.client.model;

public class ReverseDnsResult {
  private String hostname;
  private boolean valid;

  public ReverseDnsResult() {}

  public String getHostname() {
    return hostname;
  }

  public boolean isValid() {
    return valid;
  }
}
