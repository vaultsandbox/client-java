package com.vaultsandbox.client.exception;

public class DecryptionException extends VaultSandboxException {
  public DecryptionException(String message) {
    super(message);
  }

  public DecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
