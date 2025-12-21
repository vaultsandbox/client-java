package com.vaultsandbox.client.exception;

public class InboxAlreadyExistsException extends VaultSandboxException {
  public InboxAlreadyExistsException(String emailAddress) {
    super("Inbox already exists: " + emailAddress);
  }
}
