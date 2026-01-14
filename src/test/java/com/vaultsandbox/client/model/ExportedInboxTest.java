package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

/**
 * Tests for ExportedInbox validation per VaultSandbox spec §9 and §10.
 *
 * <p>Export format per spec §9.2:
 *
 * <pre>{@code
 * {
 *   "version": 1,
 *   "emailAddress": "example@vaultsandbox.com",
 *   "expiresAt": "2024-01-20T15:30:00Z",
 *   "inboxHash": "abc123...",
 *   "serverSigPk": "<base64url>",
 *   "secretKey": "<base64url>",
 *   "exportedAt": "2024-01-13T10:00:00Z"
 * }
 * }</pre>
 *
 * <p>Note: Per spec §9.4, publicKey is NOT included (derived from secretKey).
 */
class ExportedInboxTest {

  // Raw ML-KEM-768 key sizes per spec Appendix B
  private static final int PUBLIC_KEY_SIZE = 1184;
  private static final int SECRET_KEY_SIZE = 2400;
  private static final int PK_OFFSET_IN_SK = 1152;
  private static final int MLDSA_PUBLIC_KEY_SIZE = 1952;

  @BeforeAll
  static void setUp() {
    CryptoProvider.ensureInitialized();
  }

  /**
   * Create a valid ExportedInbox per spec §9.2. Public key is derived from secret key, not stored
   * separately.
   */
  private ExportedInbox createValidExportedInbox() {
    // Generate random secret key
    byte[] sk = new byte[SECRET_KEY_SIZE];
    new SecureRandom().nextBytes(sk);

    ExportedInbox exported = new ExportedInbox();
    exported.setVersion(1);
    exported.setEmailAddress("test@vaultsandbox.com");
    exported.setExpiresAt(Instant.now().plus(Duration.ofHours(1)).toString());
    exported.setInboxHash("hash123");
    exported.setServerSigPk(Base64Url.encode(new byte[MLDSA_PUBLIC_KEY_SIZE]));
    // Per spec §9.4: public key is NOT included
    exported.setSecretKey(Base64Url.encode(sk));
    exported.setExportedAt(Instant.now().toString());
    return exported;
  }

  // ==================== Valid Export Tests ====================

  @Test
  void testValidExport() {
    ExportedInbox exported = createValidExportedInbox();
    assertDoesNotThrow(exported::validate);
  }

  @Test
  void testValidExportWithMinimalData() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExportedAt(null); // exportedAt is optional
    assertDoesNotThrow(exported::validate);
  }

  // ==================== Version Validation Tests (spec §10.1 step 2) ====================

  @Test
  void testInvalidVersion() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setVersion(2);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("version"));
  }

  @Test
  void testVersionZeroInvalid() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setVersion(0);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("version"));
  }

  // ==================== Required Field Tests (spec §10.1 step 3) ====================

  @Test
  void testMissingEmailAddress() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setEmailAddress(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("emailAddress is required"));
  }

  @Test
  void testBlankEmailAddress() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setEmailAddress("   ");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("emailAddress is required"));
  }

  @Test
  void testMissingExpiresAt() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExpiresAt(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("expiresAt is required"));
  }

  @Test
  void testMissingInboxHash() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setInboxHash(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("inboxHash is required"));
  }

  @Test
  void testMissingServerSigPk() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setServerSigPk(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("serverSigPk is required for encrypted inboxes"));
  }

  @Test
  void testMissingSecretKey() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setSecretKey(null);

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getErrors().contains("secretKey is required for encrypted inboxes"));
  }

  // ==================== Email @ Validation (spec §10.1 step 4) ====================

  @Test
  void testEmailWithoutAtSign() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setEmailAddress("testvaultsandbox.com");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("@"));
  }

  @Test
  void testEmailWithMultipleAtSigns() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setEmailAddress("test@vault@sandbox.com");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("@"));
  }

  // ==================== Timestamp Validation Tests (spec §10.1 step 8) ====================

  @Test
  void testInvalidExpiresAtTimestamp() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExpiresAt("not-a-timestamp");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("ISO 8601"));
  }

  @Test
  void testInvalidExportedAtTimestamp() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExportedAt("invalid-date");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("ISO 8601"));
  }

  @Test
  void testExpiredInbox() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExpiresAt(Instant.now().minus(Duration.ofHours(1)).toString());

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("expired"));
  }

  @Test
  void testInboxExpiringNow() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setExpiresAt(Instant.now().minusSeconds(1).toString());

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("expired"));
  }

  // ==================== Key Validation Tests (spec §10.1 steps 6-7) ====================

  @Test
  void testInvalidSecretKeyLength() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setSecretKey(Base64Url.encode(new byte[100]));

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("2400 bytes"));
  }

  @Test
  void testInvalidServerSigPkLength() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setServerSigPk(Base64Url.encode(new byte[100]));

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("1952 bytes"));
  }

  @Test
  void testInvalidSecretKeyBase64() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setSecretKey("not+valid/base64!");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("base64url"));
  }

  @Test
  void testInvalidServerSigPkBase64() {
    ExportedInbox exported = createValidExportedInbox();
    exported.setServerSigPk("not+valid/base64!");

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    assertTrue(ex.getMessage().contains("base64url"));
  }

  // ==================== Public Key Derivation Tests (spec §10.2) ====================

  @Test
  void testDerivePublicKey() {
    // Generate a secret key
    byte[] sk = new byte[SECRET_KEY_SIZE];
    new SecureRandom().nextBytes(sk);

    ExportedInbox exported = createValidExportedInbox();
    exported.setSecretKey(Base64Url.encode(sk));

    // Derive public key
    byte[] derivedPk = exported.derivePublicKey();

    assertNotNull(derivedPk);
    assertEquals(PUBLIC_KEY_SIZE, derivedPk.length);

    // Verify it matches expected bytes from secret key
    byte[] expectedPk = Arrays.copyOfRange(sk, PK_OFFSET_IN_SK, PK_OFFSET_IN_SK + PUBLIC_KEY_SIZE);
    assertArrayEquals(expectedPk, derivedPk);
  }

  // ==================== Multiple Errors Tests ====================

  @Test
  void testMultipleErrors() {
    ExportedInbox exported = new ExportedInbox();
    // Leave everything null (except version which defaults to 1)

    InvalidImportDataException ex =
        assertThrows(InvalidImportDataException.class, exported::validate);

    // Should have multiple errors
    assertTrue(ex.getErrors().size() > 1);
    assertTrue(ex.getErrors().contains("emailAddress is required"));
    assertTrue(ex.getErrors().contains("expiresAt is required"));
    assertTrue(ex.getErrors().contains("inboxHash is required"));
    // serverSigPk and secretKey are only required for encrypted inboxes (default)
    assertTrue(ex.getErrors().contains("serverSigPk is required for encrypted inboxes"));
    assertTrue(ex.getErrors().contains("secretKey is required for encrypted inboxes"));
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

    int version = 1;
    String emailAddress = "test@example.com";
    String expiresAt = Instant.now().toString();
    String inboxHash = "hash123";
    String serverSigPk = "serverKey";
    String secretKey = "secretKey";
    String exportedAt = Instant.now().toString();

    exported.setVersion(version);
    exported.setEmailAddress(emailAddress);
    exported.setExpiresAt(expiresAt);
    exported.setInboxHash(inboxHash);
    exported.setServerSigPk(serverSigPk);
    exported.setSecretKey(secretKey);
    exported.setExportedAt(exportedAt);

    assertEquals(version, exported.getVersion());
    assertEquals(emailAddress, exported.getEmailAddress());
    assertEquals(expiresAt, exported.getExpiresAt());
    assertEquals(inboxHash, exported.getInboxHash());
    assertEquals(serverSigPk, exported.getServerSigPk());
    assertEquals(secretKey, exported.getSecretKey());
    assertEquals(exportedAt, exported.getExportedAt());
  }
}
