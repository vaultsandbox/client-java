package com.vaultsandbox.client.exception;

public class InboxNotFoundException extends ApiException {
  public InboxNotFoundException(String emailAddress) {
    super("Inbox not found: " + emailAddress, 404);
  }
}
