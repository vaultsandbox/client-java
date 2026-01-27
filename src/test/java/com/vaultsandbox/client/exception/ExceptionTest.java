package com.vaultsandbox.client.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExceptionTest {

  // ==================== SseException Tests ====================

  @Test
  void testSseExceptionWithMessage() {
    SseException ex = new SseException("SSE connection failed");
    assertEquals("SSE connection failed", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testSseExceptionWithMessageAndCause() {
    Throwable cause = new RuntimeException("Connection reset");
    SseException ex = new SseException("SSE connection failed", cause);
    assertEquals("SSE connection failed", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ==================== StrategyException Tests ====================

  @Test
  void testStrategyExceptionWithMessage() {
    StrategyException ex = new StrategyException("Strategy failed");
    assertEquals("Strategy failed", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testStrategyExceptionWithMessageAndCause() {
    Throwable cause = new IllegalStateException("Invalid state");
    StrategyException ex = new StrategyException("Strategy failed", cause);
    assertEquals("Strategy failed", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ==================== ApiException Tests ====================

  @Test
  void testApiExceptionWithMessageAndStatusCode() {
    ApiException ex = new ApiException("Not found", 404);
    assertEquals("Not found", ex.getMessage());
    assertEquals(404, ex.getStatusCode());
    assertNull(ex.getCause());
  }

  @Test
  void testApiExceptionWithMessageStatusCodeAndCause() {
    Throwable cause = new RuntimeException("HTTP error");
    ApiException ex = new ApiException("Server error", 500, cause);
    assertEquals("Server error", ex.getMessage());
    assertEquals(500, ex.getStatusCode());
    assertSame(cause, ex.getCause());
  }

  // ==================== TimeoutException Tests ====================

  @Test
  void testTimeoutExceptionWithMessage() {
    TimeoutException ex = new TimeoutException("Operation timed out");
    assertEquals("Operation timed out", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testTimeoutExceptionWithMessageAndCause() {
    Throwable cause = new InterruptedException("Interrupted");
    TimeoutException ex = new TimeoutException("Operation timed out", cause);
    assertEquals("Operation timed out", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ==================== SignatureVerificationException Tests ====================

  @Test
  void testSignatureVerificationExceptionDefault() {
    SignatureVerificationException ex = new SignatureVerificationException();
    assertEquals("SIGNATURE VERIFICATION FAILED - Data may be tampered!", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testSignatureVerificationExceptionWithMessage() {
    SignatureVerificationException ex = new SignatureVerificationException("Custom message");
    assertEquals("Custom message", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testSignatureVerificationExceptionWithMessageAndCause() {
    Throwable cause = new SecurityException("Invalid signature");
    SignatureVerificationException ex =
        new SignatureVerificationException("Verification failed", cause);
    assertEquals("Verification failed", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ==================== NetworkException Tests ====================

  @Test
  void testNetworkExceptionWithMessage() {
    NetworkException ex = new NetworkException("Connection refused");
    assertEquals("Connection refused", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testNetworkExceptionWithMessageAndCause() {
    Throwable cause = new java.net.ConnectException("Connection refused");
    NetworkException ex = new NetworkException("Network error", cause);
    assertEquals("Network error", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ==================== Inheritance Tests ====================

  @Test
  void testSseExceptionExtendsVaultSandboxException() {
    SseException ex = new SseException("test");
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  @Test
  void testStrategyExceptionExtendsVaultSandboxException() {
    StrategyException ex = new StrategyException("test");
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  @Test
  void testApiExceptionExtendsVaultSandboxException() {
    ApiException ex = new ApiException("test", 400);
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  @Test
  void testTimeoutExceptionExtendsVaultSandboxException() {
    TimeoutException ex = new TimeoutException("test");
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  @Test
  void testNetworkExceptionExtendsVaultSandboxException() {
    NetworkException ex = new NetworkException("test");
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  @Test
  void testSignatureVerificationExceptionExtendsVaultSandboxException() {
    SignatureVerificationException ex = new SignatureVerificationException();
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  // ==================== InboxExpiredException Tests ====================

  @Test
  void testInboxExpiredExceptionWithEmailAndTime() {
    Instant expiredAt = Instant.parse("2026-01-26T10:00:00Z");
    InboxExpiredException ex = new InboxExpiredException("test@example.com", expiredAt);

    assertTrue(ex.getMessage().contains("test@example.com"));
    assertTrue(ex.getMessage().contains(expiredAt.toString()));
    assertEquals("test@example.com", ex.getEmailAddress());
    assertEquals(expiredAt, ex.getExpiredAt());
  }

  @Test
  void testInboxExpiredExceptionExtendsVaultSandboxException() {
    InboxExpiredException ex = new InboxExpiredException("test@example.com", Instant.now());
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  // ==================== RateLimitedException Tests ====================

  @Test
  void testRateLimitedExceptionWithoutRetryAfter() {
    RateLimitedException ex = new RateLimitedException("Rate limited");

    assertEquals("Rate limited", ex.getMessage());
    assertEquals(429, ex.getStatusCode());
    assertTrue(ex.getRetryAfter().isEmpty());
    assertEquals(-1, ex.getRetryAfterSeconds());
  }

  @Test
  void testRateLimitedExceptionWithRetryAfter() {
    RateLimitedException ex = new RateLimitedException("Rate limited", 60);

    assertEquals("Rate limited", ex.getMessage());
    assertEquals(429, ex.getStatusCode());
    assertTrue(ex.getRetryAfter().isPresent());
    assertEquals(Duration.ofSeconds(60), ex.getRetryAfter().get());
    assertEquals(60, ex.getRetryAfterSeconds());
  }

  @Test
  void testRateLimitedExceptionExtendsApiException() {
    RateLimitedException ex = new RateLimitedException("Rate limited");
    assertInstanceOf(ApiException.class, ex);
    assertInstanceOf(VaultSandboxException.class, ex);
  }

  // ==================== RequestInterruptedException Tests ====================

  @Test
  void testRequestInterruptedExceptionWithMessage() {
    RequestInterruptedException ex = new RequestInterruptedException("Request interrupted");

    assertEquals("Request interrupted", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testRequestInterruptedExceptionWithMessageAndCause() {
    InterruptedException cause = new InterruptedException("Thread interrupted");
    RequestInterruptedException ex =
        new RequestInterruptedException("Request interrupted during backoff", cause);

    assertEquals("Request interrupted during backoff", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  void testRequestInterruptedExceptionExtendsVaultSandboxException() {
    RequestInterruptedException ex = new RequestInterruptedException("test");
    assertInstanceOf(VaultSandboxException.class, ex);
  }
}
