package com.vaultsandbox.client.exception;

public class StrategyException extends VaultSandboxException {
  public StrategyException(String message) {
    super(message);
  }

  public StrategyException(String message, Throwable cause) {
    super(message, cause);
  }
}
