package com.vaultsandbox.client.exception;

/**
 * Thrown when an API request is interrupted.
 *
 * <p>This exception is thrown when a thread is interrupted while waiting for an API response or
 * during retry backoff. Unlike other API exceptions, this does not represent a server error and the
 * thread's interrupt status will have been restored.
 */
public class RequestInterruptedException extends VaultSandboxException {
  /**
   * Creates a new RequestInterruptedException.
   *
   * @param message the error message
   */
  public RequestInterruptedException(String message) {
    super(message);
  }

  /**
   * Creates a new RequestInterruptedException with a cause.
   *
   * @param message the error message
   * @param cause the underlying InterruptedException
   */
  public RequestInterruptedException(String message, InterruptedException cause) {
    super(message, cause);
  }
}
