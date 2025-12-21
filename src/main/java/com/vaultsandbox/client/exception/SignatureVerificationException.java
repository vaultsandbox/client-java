package com.vaultsandbox.client.exception;

public class SignatureVerificationException extends VaultSandboxException {
  public SignatureVerificationException() {
    super("SIGNATURE VERIFICATION FAILED - Data may be tampered!");
  }

  public SignatureVerificationException(String message) {
    super(message);
  }

  public SignatureVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
