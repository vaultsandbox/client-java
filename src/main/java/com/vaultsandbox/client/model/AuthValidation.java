package com.vaultsandbox.client.model;

import java.util.List;

public class AuthValidation {
  private final List<String> passed;
  private final List<String> failed;

  public AuthValidation(List<String> passed, List<String> failed) {
    this.passed = List.copyOf(passed);
    this.failed = List.copyOf(failed);
  }

  public List<String> getPassed() {
    return passed;
  }

  public List<String> getFailed() {
    return failed;
  }

  /**
   * Alias for getFailed() - returns the list of failed authentication checks.
   *
   * @return list of failure reasons
   */
  public List<String> getFailures() {
    return failed;
  }

  public boolean isFullyAuthenticated() {
    return failed.isEmpty() && !passed.isEmpty();
  }

  /**
   * Returns whether all authentication checks passed (alias for isFullyAuthenticated).
   *
   * @return true if all checks passed
   */
  public boolean isPassed() {
    return isFullyAuthenticated();
  }

  public boolean hasSpf() {
    return passed.contains("SPF");
  }

  public boolean hasDkim() {
    return passed.contains("DKIM");
  }

  public boolean hasDmarc() {
    return passed.contains("DMARC");
  }

  /**
   * Returns whether the reverse DNS check passed.
   *
   * @return true if reverse DNS verification passed
   */
  public boolean hasReverseDns() {
    return passed.contains("ReverseDNS");
  }
}
