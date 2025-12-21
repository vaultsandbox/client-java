package com.vaultsandbox.client.exception;

public class EmailNotFoundException extends ApiException {
  public EmailNotFoundException(String emailId) {
    super("Email not found: " + emailId, 404);
  }
}
