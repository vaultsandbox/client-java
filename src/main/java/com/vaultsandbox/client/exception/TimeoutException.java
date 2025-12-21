package com.vaultsandbox.client.exception;

public class TimeoutException extends VaultSandboxException {
  public TimeoutException(String message) {
    super(message);
  }

  public TimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
