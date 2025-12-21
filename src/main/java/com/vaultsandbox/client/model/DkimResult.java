package com.vaultsandbox.client.model;

public class DkimResult {
  private String result; // pass, fail, none, etc.
  private String domain;
  private String selector;

  public DkimResult() {}

  public String getResult() {
    return result;
  }

  public String getDomain() {
    return domain;
  }

  public String getSelector() {
    return selector;
  }
}
