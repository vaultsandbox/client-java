package com.vaultsandbox.client.exception;

public class SseException extends VaultSandboxException {
  public SseException(String message) {
    super(message);
  }

  public SseException(String message, Throwable cause) {
    super(message, cause);
  }
}
