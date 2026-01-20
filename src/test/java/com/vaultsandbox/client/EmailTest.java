package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.model.DecryptedEmailContent;
import com.vaultsandbox.client.model.EmailData;
import com.vaultsandbox.client.model.SpamAction;
import com.vaultsandbox.client.model.SpamAnalysisResult;
import com.vaultsandbox.client.model.SpamAnalysisStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTest {

  private static final Gson GSON = new GsonBuilder().create();

  @Test
  void testMetadataFromContent() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithMetadata();

    Email email = new Email(emailData, null, content, null);

    assertNotNull(email.getMetadata());
    assertEquals(2, email.getMetadata().size());
    assertEquals("value1", email.getMetadata().get("key1"));
    assertEquals(123.0, email.getMetadata().get("key2"));
  }

  @Test
  void testMetadataEmptyWhenContentNull() {
    EmailData emailData = createEmailData();

    Email email = new Email(emailData, null, null, null);

    assertNotNull(email.getMetadata());
    assertTrue(email.getMetadata().isEmpty());
  }

  @Test
  void testMetadataEmptyWhenNotPresent() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithoutMetadata();

    Email email = new Email(emailData, null, content, null);

    assertNotNull(email.getMetadata());
    assertTrue(email.getMetadata().isEmpty());
  }

  @Test
  void testMetadataImmutability() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithMetadata();

    Email email = new Email(emailData, null, content, null);

    assertThrows(
        UnsupportedOperationException.class, () -> email.getMetadata().put("new", "value"));
  }

  private EmailData createEmailData() {
    String json =
        GSON.toJson(
            Map.of(
                "id",
                "test-id",
                "from",
                "sender@example.com",
                "to",
                List.of("recipient@example.com"),
                "subject",
                "Test Subject",
                "receivedAt",
                Instant.now().toString(),
                "isRead",
                false));
    return GSON.fromJson(json, EmailData.class);
  }

  private DecryptedEmailContent createContentWithMetadata() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "metadata": {
                "key1": "value1",
                "key2": 123
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  private DecryptedEmailContent createContentWithoutMetadata() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": []
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  // ==================== Spam Analysis Tests ====================

  @Test
  void testSpamAnalysisFromContent() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertNotNull(email.getSpamAnalysis());
    assertEquals(SpamAnalysisStatus.ANALYZED, email.getSpamAnalysis().getStatus());
    assertEquals(12.7, email.getSpamAnalysis().getScore(), 0.001);
    assertTrue(email.getSpamAnalysis().isSpam());
    assertEquals(SpamAction.REJECT, email.getSpamAnalysis().getAction());
  }

  @Test
  void testIsSpamHelperMethod() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertEquals(Boolean.TRUE, email.isSpam());
  }

  @Test
  void testIsSpamForCleanEmail() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithCleanSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertEquals(Boolean.FALSE, email.isSpam());
    assertEquals(0.5, email.getSpamScore(), 0.001);
  }

  @Test
  void testGetSpamScoreHelperMethod() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertEquals(12.7, email.getSpamScore(), 0.001);
  }

  @Test
  void testIsSpamReturnsNullWhenContentNull() {
    EmailData emailData = createEmailData();

    Email email = new Email(emailData, null, null, null);

    assertNull(email.isSpam());
    assertNull(email.getSpamScore());
    assertNull(email.getSpamAnalysis());
  }

  @Test
  void testIsSpamReturnsNullWhenSkipped() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithSkippedSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertNull(email.isSpam());
    assertNull(email.getSpamScore());
    assertNotNull(email.getSpamAnalysis());
    assertEquals(SpamAnalysisStatus.SKIPPED, email.getSpamAnalysis().getStatus());
  }

  @Test
  void testIsSpamReturnsNullWhenError() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithErrorSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    assertNull(email.isSpam());
    assertNull(email.getSpamScore());
    assertNotNull(email.getSpamAnalysis());
    assertEquals(SpamAnalysisStatus.ERROR, email.getSpamAnalysis().getStatus());
    assertNotNull(email.getSpamAnalysis().getInfo());
  }

  @Test
  void testSpamAnalysisWithSymbols() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithSpamAnalysis();

    Email email = new Email(emailData, null, content, null);

    SpamAnalysisResult spam = email.getSpamAnalysis();
    assertNotNull(spam.getSymbols());
    assertFalse(spam.getSymbols().isEmpty());
    assertEquals(3, spam.getSymbols().size());
  }

  private DecryptedEmailContent createContentWithSpamAnalysis() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "spamAnalysis": {
                "status": "analyzed",
                "score": 12.7,
                "requiredScore": 6.0,
                "action": "reject",
                "isSpam": true,
                "symbols": [
                    {"name": "RCVD_IN_SBL", "score": 6.5},
                    {"name": "FORGED_SENDER", "score": 3.0},
                    {"name": "SUSPICIOUS_URL", "score": 2.5}
                ],
                "processingTimeMs": 78
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  private DecryptedEmailContent createContentWithCleanSpamAnalysis() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "spamAnalysis": {
                "status": "analyzed",
                "score": 0.5,
                "requiredScore": 6.0,
                "action": "no action",
                "isSpam": false,
                "symbols": [
                    {"name": "DKIM_SIGNED", "score": -0.1},
                    {"name": "SPF_ALLOW", "score": -0.2}
                ],
                "processingTimeMs": 32
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  private DecryptedEmailContent createContentWithSkippedSpamAnalysis() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "spamAnalysis": {
                "status": "skipped",
                "info": "Spam analysis disabled"
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  private DecryptedEmailContent createContentWithErrorSpamAnalysis() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "spamAnalysis": {
                "status": "error",
                "processingTimeMs": 5001,
                "info": "Rspamd request timed out"
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }
}
