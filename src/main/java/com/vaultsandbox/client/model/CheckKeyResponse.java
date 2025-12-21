package com.vaultsandbox.client.model;

public class CheckKeyResponse {
  private boolean valid;
  private String message;

  public CheckKeyResponse() {}

  public boolean isValid() {
    return valid;
  }

  public String getMessage() {
    return message;
  }
}
