package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.crypto.CryptoProvider;
import com.vaultsandbox.client.exception.InvalidImportDataException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExportedInboxTest {

  // Raw ML-KEM-768 key sizes
  private static final int PUBLIC_KEY_SIZE = 1184;
  private static final int SECRET_KEY_SIZE = 2400;
  // In raw ML-KEM-768 format, public key is at bytes 1152-2336 in secret key
  private static final int PK_OFFSET_IN_SK = 1152;

  @BeforeAll
  static void setUp() {
    CryptoProvider.ensureInitialized();
  }

  /**
   * Create a valid ExportedInbox with raw ML-KEM-768 format keys. The public key is embedded in the
   * secret key at the expected offset.
   */
  private ExportedInbox createValidExportedInboxWithRawKeys() {
    // Generate random secret key
    byte[] sk = new byte[SECRET_KEY_SIZE];
    new SecureRandom().nextBytes(sk);

    // Extract public key from secret key (as per ML-KEM spec)
    byte[] pk = Arrays.copyOfRange(sk, PK_OFFSET_IN_SK, PK_OFFSET_IN_SK + PUBLIC_KEY_SIZE);

    ExportedInbox exported = new ExportedInbox();
    exported.setEmailAddress("test@vaultsandbox.com");
    exported.setExpiresAt(Instant.now().plus(Duration.ofHours(1)).toString());
    exported.setInboxHash("hash123");
    exported.setServerSigPk(Base64Url.encode(new byte[1952]));
    exported.setPublicKeyB64(Base64Url.encode(pk));
    exported.setSecretKeyB64(Base64Url.encode(sk));
    exported.setExportedAt(Instant.now().toString());
    return exported;
  }

  // ==================== Valid Export Tests ====================

  @Test
  void testValidExport() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();

    assertDoesNotThrow(exported::validate);
  }

  @Test
  void testValidExportWithMinimalData() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setExportedAt(null); // exportedAt is optional

    assertDoesNotThrow(exported::validate);
  }

  // ==================== Required Field Tests ====================

  @Test
  void testMissingEmailAddress() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setEmailAddress(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("emailAddress is required"));
  }

  @Test
  void testBlankEmailAddress() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setEmailAddress("   ");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("emailAddress is required"));
  }

  @Test
  void testMissingExpiresAt() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setExpiresAt(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("expiresAt is required"));
  }

  @Test
  void testMissingInboxHash() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setInboxHash(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("inboxHash is required"));
  }

  @Test
  void testMissingServerSigPk() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setServerSigPk(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("serverSigPk is required"));
  }

  @Test
  void testMissingPublicKey() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setPublicKeyB64(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("publicKeyB64 is required"));
  }

  @Test
  void testMissingSecretKey() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setSecretKeyB64(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("secretKeyB64 is required"));
  }

  // ==================== Timestamp Validation Tests ====================

  @Test
  void testInvalidExpiresAtTimestamp() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setExpiresAt("not-a-timestamp");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("ISO 8601"));
  }

  @Test
  void testInvalidExportedAtTimestamp() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setExportedAt("invalid-date");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("ISO 8601"));
  }

  @Test
  void testExpiredInbox() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setExpiresAt(Instant.now().minus(Duration.ofHours(1)).toString());

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("expired"));
  }

  @Test
  void testInboxExpiringNow() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    // Set expiration to slightly in the past
    exported.setExpiresAt(Instant.now().minusSeconds(1).toString());

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("expired"));
  }

  // ==================== Key Validation Tests ====================

  @Test
  void testInvalidPublicKeyLength() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setPublicKeyB64(Base64Url.encode(new byte[100]));

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("1184 bytes"));
  }

  @Test
  void testInvalidSecretKeyLength() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setSecretKeyB64(Base64Url.encode(new byte[100]));

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("2400 bytes"));
  }

  @Test
  void testInvalidPublicKeyBase64() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setPublicKeyB64("not-valid-base64!");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("base64url"));
  }

  @Test
  void testInvalidSecretKeyBase64() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();
    exported.setSecretKeyB64("not-valid-base64!");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("base64url"));
  }

  @Test
  void testKeyMismatch() {
    ExportedInbox exported = createValidExportedInboxWithRawKeys();

    // Generate a different public key
    byte[] differentPk = new byte[PUBLIC_KEY_SIZE];
    new SecureRandom().nextBytes(differentPk);
    exported.setPublicKeyB64(Base64Url.encode(differentPk));

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("does not match"));
  }

  @Test
  void testKeyMismatchWithValidLengths() {
    // Create two different raw keypairs
    byte[] sk1 = new byte[SECRET_KEY_SIZE];
    byte[] sk2 = new byte[SECRET_KEY_SIZE];
    new SecureRandom().nextBytes(sk1);
    new SecureRandom().nextBytes(sk2);

    byte[] pk1 = Arrays.copyOfRange(sk1, PK_OFFSET_IN_SK, PK_OFFSET_IN_SK + PUBLIC_KEY_SIZE);

    ExportedInbox exported = new ExportedInbox();
    exported.setEmailAddress("test@vaultsandbox.com");
    exported.setExpiresAt(Instant.now().plus(Duration.ofHours(1)).toString());
    exported.setInboxHash("hash123");
    exported.setServerSigPk(Base64Url.encode(new byte[1952]));
    exported.setPublicKeyB64(Base64Url.encode(pk1)); // pk from sk1
    exported.setSecretKeyB64(Base64Url.encode(sk2)); // sk2, different

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("does not match"));
  }

  // ==================== Multiple Errors Tests ====================

  @Test
  void testMultipleErrors() {
    ExportedInbox exported = new ExportedInbox();
    // Leave everything null

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    // Should have multiple errors
    assertTrue(ex.getErrors().size() > 1);
    assertTrue(ex.getErrors().contains("emailAddress is required"));
    assertTrue(ex.getErrors().contains("expiresAt is required"));
    assertTrue(ex.getErrors().contains("inboxHash is required"));
    assertTrue(ex.getErrors().contains("serverSigPk is required"));
    assertTrue(ex.getErrors().contains("publicKeyB64 is required"));
    assertTrue(ex.getErrors().contains("secretKeyB64 is required"));
  }

  @Test
  void testErrorMessageFormatting() {
    ExportedInbox exported = new ExportedInbox();

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    // With multiple errors, message should contain "Invalid import data:"
    String message = ex.getMessage();
    assertTrue(message.contains("Invalid import data:") || ex.getErrors().size() == 1);
  }

  // ==================== Getter/Setter Tests ====================

  @Test
  void testGettersSetters() {
    ExportedInbox exported = new ExportedInbox();

    String emailAddress = "test@example.com";
    String expiresAt = Instant.now().toString();
    String inboxHash = "hash123";
    String serverSigPk = "serverKey";
    String publicKeyB64 = "publicKey";
    String secretKeyB64 = "secretKey";
    String exportedAt = Instant.now().toString();

    exported.setEmailAddress(emailAddress);
    exported.setExpiresAt(expiresAt);
    exported.setInboxHash(inboxHash);
    exported.setServerSigPk(serverSigPk);
    exported.setPublicKeyB64(publicKeyB64);
    exported.setSecretKeyB64(secretKeyB64);
    exported.setExportedAt(exportedAt);

    assertEquals(emailAddress, exported.getEmailAddress());
    assertEquals(expiresAt, exported.getExpiresAt());
    assertEquals(inboxHash, exported.getInboxHash());
    assertEquals(serverSigPk, exported.getServerSigPk());
    assertEquals(publicKeyB64, exported.getPublicKeyB64());
    assertEquals(secretKeyB64, exported.getSecretKeyB64());
    assertEquals(exportedAt, exported.getExportedAt());
  }
}
