package com.vaultsandbox.client.exception;

import java.time.Duration;
import java.util.Optional;

/**
 * Thrown when the API rate limit is exceeded (HTTP 429).
 *
 * <p>This exception provides access to the Retry-After header value if present, which indicates how
 * long to wait before retrying the request.
 */
public class RateLimitedException extends ApiException {
  private final Duration retryAfter;

  /**
   * Creates a new RateLimitedException without retry information.
   *
   * @param message the error message
   */
  public RateLimitedException(String message) {
    super(message, 429);
    this.retryAfter = null;
  }

  /**
   * Creates a new RateLimitedException with retry information.
   *
   * @param message the error message
   * @param retryAfterSeconds the number of seconds to wait before retrying (from Retry-After
   *     header)
   */
  public RateLimitedException(String message, long retryAfterSeconds) {
    super(message, 429);
    this.retryAfter = Duration.ofSeconds(retryAfterSeconds);
  }

  /**
   * Returns the recommended wait duration before retrying.
   *
   * <p>This value comes from the Retry-After HTTP header. If the header was not present or could
   * not be parsed, this returns empty.
   *
   * @return the retry-after duration, or empty if not available
   */
  public Optional<Duration> getRetryAfter() {
    return Optional.ofNullable(retryAfter);
  }

  /**
   * Returns the recommended wait time in seconds.
   *
   * @return the retry-after value in seconds, or -1 if not available
   */
  public long getRetryAfterSeconds() {
    return retryAfter != null ? retryAfter.getSeconds() : -1;
  }
}
