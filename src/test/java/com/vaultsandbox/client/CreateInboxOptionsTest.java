package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Unit tests for CreateInboxOptions builder functionality. */
class CreateInboxOptionsTest {

  // ==================== Default Configuration Tests ====================

  @Test
  void testDefaultOptions() {
    CreateInboxOptions options = CreateInboxOptions.defaults();

    assertNull(options.getEmailAddress());
    assertNull(options.getTtl());
    assertNull(options.getEmailAuth());
    assertNull(options.getEncryption());
    assertNull(options.getSpamAnalysis());
  }

  @Test
  void testEmptyBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().build();

    assertNull(options.getEmailAddress());
    assertNull(options.getTtl());
    assertNull(options.getEmailAuth());
    assertNull(options.getEncryption());
    assertNull(options.getSpamAnalysis());
  }

  // ==================== Builder Methods Tests ====================

  @Test
  void testEmailAddressBuilder() {
    CreateInboxOptions options =
        CreateInboxOptions.builder().emailAddress("custom@example.com").build();

    assertEquals("custom@example.com", options.getEmailAddress());
  }

  @Test
  void testTtlBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().ttl(Duration.ofHours(2)).build();

    assertEquals(Duration.ofHours(2), options.getTtl());
  }

  @Test
  void testEmailAuthEnabledBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().emailAuth(true).build();

    assertEquals(Boolean.TRUE, options.getEmailAuth());
  }

  @Test
  void testEmailAuthDisabledBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().emailAuth(false).build();

    assertEquals(Boolean.FALSE, options.getEmailAuth());
  }

  @Test
  void testEmailAuthNullBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().emailAuth(null).build();

    assertNull(options.getEmailAuth());
  }

  // ==================== Builder Chaining Tests ====================

  @Test
  void testBuilderChaining() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .emailAddress("test@example.com")
            .ttl(Duration.ofMinutes(30))
            .emailAuth(true)
            .build();

    assertNotNull(options);
    assertEquals("test@example.com", options.getEmailAddress());
    assertEquals(Duration.ofMinutes(30), options.getTtl());
    assertEquals(Boolean.TRUE, options.getEmailAuth());
  }

  @Test
  void testBuilderWithAllFields() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .emailAddress("inbox@domain.com")
            .ttl(Duration.ofHours(1))
            .emailAuth(false)
            .build();

    assertEquals("inbox@domain.com", options.getEmailAddress());
    assertEquals(Duration.ofHours(1), options.getTtl());
    assertEquals(Boolean.FALSE, options.getEmailAuth());
  }

  // ==================== Encryption Builder Tests ====================

  @Test
  void testPlainBuilderMethod() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().build();

    assertEquals("plain", options.getEncryption());
  }

  @Test
  void testEncryptedBuilderMethod() {
    CreateInboxOptions options = CreateInboxOptions.builder().encrypted().build();

    assertEquals("encrypted", options.getEncryption());
  }

  @Test
  void testEncryptionStringBuilderPlain() {
    CreateInboxOptions options = CreateInboxOptions.builder().encryption("plain").build();

    assertEquals("plain", options.getEncryption());
  }

  @Test
  void testEncryptionStringBuilderEncrypted() {
    CreateInboxOptions options = CreateInboxOptions.builder().encryption("encrypted").build();

    assertEquals("encrypted", options.getEncryption());
  }

  @Test
  void testEncryptionStringBuilderNull() {
    CreateInboxOptions options = CreateInboxOptions.builder().encryption(null).build();

    assertNull(options.getEncryption());
  }

  @Test
  void testPlainWithOtherOptions() {
    CreateInboxOptions options =
        CreateInboxOptions.builder().plain().ttl(Duration.ofMinutes(45)).emailAuth(true).build();

    assertEquals("plain", options.getEncryption());
    assertEquals(Duration.ofMinutes(45), options.getTtl());
    assertEquals(Boolean.TRUE, options.getEmailAuth());
  }

  @Test
  void testEncryptedWithOtherOptions() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .encrypted()
            .emailAddress("test@example.com")
            .ttl(Duration.ofHours(2))
            .build();

    assertEquals("encrypted", options.getEncryption());
    assertEquals("test@example.com", options.getEmailAddress());
    assertEquals(Duration.ofHours(2), options.getTtl());
  }

  @Test
  void testBuilderWithAllFieldsIncludingEncryption() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .emailAddress("full@example.com")
            .ttl(Duration.ofHours(3))
            .emailAuth(false)
            .plain()
            .build();

    assertEquals("full@example.com", options.getEmailAddress());
    assertEquals(Duration.ofHours(3), options.getTtl());
    assertEquals(Boolean.FALSE, options.getEmailAuth());
    assertEquals("plain", options.getEncryption());
  }

  // ==================== Spam Analysis Builder Tests ====================

  @Test
  void testSpamAnalysisEnabledBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().spamAnalysis(true).build();

    assertEquals(Boolean.TRUE, options.getSpamAnalysis());
  }

  @Test
  void testSpamAnalysisDisabledBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().spamAnalysis(false).build();

    assertEquals(Boolean.FALSE, options.getSpamAnalysis());
  }

  @Test
  void testSpamAnalysisNullBuilder() {
    CreateInboxOptions options = CreateInboxOptions.builder().spamAnalysis(null).build();

    assertNull(options.getSpamAnalysis());
  }

  @Test
  void testSpamAnalysisWithOtherOptions() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .emailAddress("spam-test@example.com")
            .ttl(Duration.ofMinutes(30))
            .emailAuth(true)
            .spamAnalysis(true)
            .build();

    assertEquals("spam-test@example.com", options.getEmailAddress());
    assertEquals(Duration.ofMinutes(30), options.getTtl());
    assertEquals(Boolean.TRUE, options.getEmailAuth());
    assertEquals(Boolean.TRUE, options.getSpamAnalysis());
  }

  @Test
  void testBuilderWithAllFieldsIncludingSpamAnalysis() {
    CreateInboxOptions options =
        CreateInboxOptions.builder()
            .emailAddress("complete@example.com")
            .ttl(Duration.ofHours(2))
            .emailAuth(true)
            .encrypted()
            .spamAnalysis(true)
            .build();

    assertEquals("complete@example.com", options.getEmailAddress());
    assertEquals(Duration.ofHours(2), options.getTtl());
    assertEquals(Boolean.TRUE, options.getEmailAuth());
    assertEquals("encrypted", options.getEncryption());
    assertEquals(Boolean.TRUE, options.getSpamAnalysis());
  }

  @Test
  void testPlainInboxWithSpamAnalysisDisabled() {
    CreateInboxOptions options = CreateInboxOptions.builder().plain().spamAnalysis(false).build();

    assertEquals("plain", options.getEncryption());
    assertEquals(Boolean.FALSE, options.getSpamAnalysis());
  }
}
