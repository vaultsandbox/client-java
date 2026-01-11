package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTest {

  private static final Gson GSON = new GsonBuilder().create();

  // ==================== DeleteAllResponse Tests ====================

  @Test
  void testDeleteAllResponseGetDeletedCount() {
    String json = "{\"deleted\": 5}";
    DeleteAllResponse response = GSON.fromJson(json, DeleteAllResponse.class);
    assertEquals(5, response.getDeletedCount());
  }

  @Test
  void testDeleteAllResponseZeroDeleted() {
    String json = "{\"deleted\": 0}";
    DeleteAllResponse response = GSON.fromJson(json, DeleteAllResponse.class);
    assertEquals(0, response.getDeletedCount());
  }

  // ==================== ServerInfo Tests ====================

  @Test
  void testServerInfoGetters() {
    String json =
        """
        {
          "serverSigPk": "abc123",
          "context": "test-context",
          "maxTtl": 86400,
          "defaultTtl": 3600,
          "sseConsole": true,
          "allowedDomains": ["example.com", "test.com"],
          "version": "1.0.0",
          "domain": "vaultsandbox.com"
        }
        """;
    ServerInfo info = GSON.fromJson(json, ServerInfo.class);

    assertEquals("abc123", info.getServerSigPk());
    assertEquals("test-context", info.getContext());
    assertEquals(86400, info.getMaxTtl());
    assertEquals(3600, info.getDefaultTtl());
    assertTrue(info.isSseConsole());
    assertEquals(List.of("example.com", "test.com"), info.getAllowedDomains());
    assertEquals("1.0.0", info.getVersion());
    assertEquals("vaultsandbox.com", info.getDomain());
  }

  @Test
  void testServerInfoAlgorithmsFromNewApi() {
    String json =
        """
        {
          "algs": {
            "aead": "AES-256-GCM",
            "kdf": "HKDF-SHA-512",
            "kem": "ML-KEM-768",
            "sig": "ML-DSA-65"
          }
        }
        """;
    ServerInfo info = GSON.fromJson(json, ServerInfo.class);
    Algorithms algs = info.getAlgorithms();

    assertNotNull(algs);
    assertEquals("AES-256-GCM", algs.getAead());
    assertEquals("HKDF-SHA-512", algs.getKdf());
    assertEquals("ML-KEM-768", algs.getKem());
    assertEquals("ML-DSA-65", algs.getSig());
  }

  @Test
  void testServerInfoAlgorithmsFromLegacyApi() {
    String json =
        """
        {
          "algorithms": {
            "aead": "AES-256-GCM",
            "kdf": "HKDF-SHA-512"
          }
        }
        """;
    ServerInfo info = GSON.fromJson(json, ServerInfo.class);
    Algorithms algs = info.getAlgorithms();

    assertNotNull(algs);
    assertEquals("AES-256-GCM", algs.getAead());
    assertEquals("HKDF-SHA-512", algs.getKdf());
  }

  @Test
  void testServerInfoLimits() {
    String json =
        """
        {
          "limits": {
            "maxInboxes": 100,
            "maxEmailsPerInbox": 50,
            "inboxTtlSeconds": 86400
          }
        }
        """;
    ServerInfo info = GSON.fromJson(json, ServerInfo.class);
    ServerInfo.Limits limits = info.getLimits();

    assertNotNull(limits);
    assertEquals(100, limits.getMaxInboxes());
    assertEquals(50, limits.getMaxEmailsPerInbox());
    assertEquals(86400, limits.getInboxTtlSeconds());
  }

  // ==================== EmailMetadata Tests ====================

  @Test
  void testEmailMetadataGetters() {
    EmailMetadata metadata =
        new EmailMetadata(
            "id-123", "sender@example.com", "Test Subject", "2024-01-15T10:30:00Z", false);

    assertEquals("id-123", metadata.getId());
    assertEquals("sender@example.com", metadata.getFrom());
    assertEquals("Test Subject", metadata.getSubject());
    assertEquals("2024-01-15T10:30:00Z", metadata.getReceivedAt());
    assertFalse(metadata.isRead());
  }

  @Test
  void testEmailMetadataIsRead() {
    EmailMetadata metadata =
        new EmailMetadata("id-456", "sender@test.com", "Read Email", "2024-01-15T11:00:00Z", true);

    assertTrue(metadata.isRead());
  }

  // ==================== EmailData Tests ====================

  @Test
  void testEmailDataGetters() {
    String json =
        """
        {
          "id": "email-123",
          "inboxId": "inbox-456",
          "receivedAt": "2024-01-15T10:00:00Z",
          "isRead": true,
          "from": "sender@example.com",
          "to": ["recipient@example.com"],
          "subject": "Test Email"
        }
        """;
    EmailData emailData = GSON.fromJson(json, EmailData.class);

    assertEquals("email-123", emailData.getId());
    assertEquals("inbox-456", emailData.getInboxId());
    assertEquals("2024-01-15T10:00:00Z", emailData.getReceivedAt());
    assertTrue(emailData.isRead());
    assertEquals("sender@example.com", emailData.getFrom());
    assertEquals(List.of("recipient@example.com"), emailData.getTo());
    assertEquals("Test Email", emailData.getSubject());
  }

  @Test
  void testEmailDataEncryptedPayloadNewApi() {
    String json =
        """
        {
          "id": "email-123",
          "encryptedParsed": {
            "ciphertext": "abc123",
            "nonce": "nonce123",
            "eph_pk": "ephpk123"
          }
        }
        """;
    EmailData emailData = GSON.fromJson(json, EmailData.class);

    assertNotNull(emailData.getEncrypted());
    assertNotNull(emailData.getEncryptedParsed());
  }

  @Test
  void testEmailDataEncryptedPayloadLegacyApi() {
    String json =
        """
        {
          "id": "email-123",
          "encrypted": {
            "ciphertext": "abc123",
            "nonce": "nonce123",
            "eph_pk": "ephpk123"
          }
        }
        """;
    EmailData emailData = GSON.fromJson(json, EmailData.class);

    assertNotNull(emailData.getEncrypted());
  }

  @Test
  void testEmailDataEncryptedMetadata() {
    String json =
        """
        {
          "id": "email-123",
          "encryptedMetadata": {
            "ciphertext": "metadata123",
            "nonce": "nonce456",
            "eph_pk": "ephpk456"
          }
        }
        """;
    EmailData emailData = GSON.fromJson(json, EmailData.class);

    assertNotNull(emailData.getEncryptedMetadata());
  }

  // ==================== SyncStatus Tests ====================

  @Test
  void testSyncStatusGetters() {
    String json =
        """
        {
          "emailsHash": "hash123abc",
          "emailCount": 42,
          "lastUpdated": "2024-01-15T12:00:00Z"
        }
        """;
    SyncStatus status = GSON.fromJson(json, SyncStatus.class);

    assertEquals("hash123abc", status.getEmailsHash());
    assertEquals(42, status.getEmailCount());
    assertEquals("2024-01-15T12:00:00Z", status.getLastUpdated());
  }

  // ==================== RawEmailData Tests ====================

  @Test
  void testRawEmailDataGetters() {
    String json =
        """
        {
          "id": "raw-email-123",
          "raw_content": "From: sender@example.com\\nTo: recipient@example.com\\n\\nBody"
        }
        """;
    RawEmailData rawData = GSON.fromJson(json, RawEmailData.class);

    assertEquals("raw-email-123", rawData.getId());
    assertEquals(
        "From: sender@example.com\nTo: recipient@example.com\n\nBody", rawData.getRawContent());
  }

  @Test
  void testRawEmailDataEncryptedRaw() {
    String json =
        """
        {
          "id": "raw-email-456",
          "encryptedRaw": {
            "ciphertext": "encrypted123",
            "nonce": "nonce789",
            "eph_pk": "ephpk789"
          }
        }
        """;
    RawEmailData rawData = GSON.fromJson(json, RawEmailData.class);

    assertEquals("raw-email-456", rawData.getId());
    assertNotNull(rawData.getEncryptedRaw());
  }
}
