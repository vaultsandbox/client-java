package com.vaultsandbox.client.exception;

public class VaultSandboxException extends RuntimeException {
  public VaultSandboxException(String message) {
    super(message);
  }

  public VaultSandboxException(String message, Throwable cause) {
    super(message, cause);
  }
}
