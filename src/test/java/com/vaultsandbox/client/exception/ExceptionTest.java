package com.vaultsandbox.client.exception;

import static org.junit.jupiter.api.Assertions.*;

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
}
