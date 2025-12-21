package com.vaultsandbox.client.exception;

public class NetworkException extends VaultSandboxException {
  public NetworkException(String message) {
    super(message);
  }

  public NetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
