package com.vaultsandbox.client.exception;

import java.util.List;

public class InvalidImportDataException extends VaultSandboxException {
  private final List<String> errors;

  public InvalidImportDataException(String message) {
    super(message);
    this.errors = List.of(message);
  }

  public InvalidImportDataException(List<String> errors) {
    super(formatErrors(errors));
    this.errors = List.copyOf(errors);
  }

  public InvalidImportDataException(String message, Throwable cause) {
    super(message, cause);
    this.errors = List.of(message);
  }

  public List<String> getErrors() {
    return errors;
  }

  private static String formatErrors(List<String> errors) {
    if (errors.isEmpty()) {
      return "Invalid import data";
    }
    if (errors.size() == 1) {
      return errors.get(0);
    }
    return "Invalid import data: " + String.join("; ", errors);
  }
}
