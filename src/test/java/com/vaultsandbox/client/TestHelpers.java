package com.vaultsandbox.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.crypto.Keypair;
import com.vaultsandbox.client.crypto.KeypairGenerator;
import com.vaultsandbox.client.model.AuthResults;
import com.vaultsandbox.client.model.DecryptedEmailContent;
import com.vaultsandbox.client.model.EmailData;
import com.vaultsandbox.client.model.ExportedInbox;
import com.vaultsandbox.client.model.SpamAnalysisResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Test utilities for creating mock objects and test data. */
public final class TestHelpers {
  private TestHelpers() {}

  private static final KeypairGenerator KEYPAIR_GENERATOR = new KeypairGenerator();
  private static final Gson GSON = new GsonBuilder().create();

  /**
   * Create a valid ExportedInbox for testing. Per VaultSandbox spec §9.4, public key is NOT
   * included (derived from secret key).
   */
  public static ExportedInbox createValidExportedInbox() {
    Keypair kp = KEYPAIR_GENERATOR.generate();
    ExportedInbox exported = new ExportedInbox();
    exported.setVersion(1);
    exported.setEmailAddress("test@vaultsandbox.com");
    exported.setExpiresAt(Instant.now().plus(Duration.ofHours(1)).toString());
    exported.setInboxHash("hash123");
    exported.setServerSigPk(Base64Url.encode(new byte[1952]));
    // Per spec §9.4: public key is NOT included (derived from secret key)
    exported.setSecretKey(Base64Url.encode(kp.getSecretKey()));
    exported.setExportedAt(Instant.now().toString());
    return exported;
  }

  /**
   * Create a test Email object with basic data.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @return a test Email object
   */
  public static Email createTestEmail(String from, String subject) {
    return createTestEmail(from, subject, "Test body content");
  }

  /**
   * Create a test Email object with specified content.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param body the email body text
   * @return a test Email object
   */
  public static Email createTestEmail(String from, String subject, String body) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content = createDecryptedContent(body != null ? body : "", null);
    // Pass null for metadata - Email will fall back to legacy fields in EmailData
    return new Email(emailData, null, content, null);
  }

  /**
   * Create a test Email object with HTML content.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param text the plain text body
   * @param html the HTML body
   * @return a test Email object
   */
  public static Email createTestEmailWithHtml(
      String from, String subject, String text, String html) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content = createDecryptedContent(text, html);
    // Pass null for metadata - Email will fall back to legacy fields in EmailData
    return new Email(emailData, null, content, null);
  }

  /** Create EmailData for testing. */
  public static EmailData createEmailData(String id, String from, String subject) {
    // Build map manually to handle nulls properly (using camelCase for new API format)
    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
    map.put("id", id);
    map.put("from", from); // Can be null
    map.put("to", List.of("test@vaultsandbox.com"));
    map.put("subject", subject); // Can be null
    map.put("receivedAt", Instant.now().toString()); // camelCase
    map.put("isRead", false); // camelCase

    String json = GSON.toJson(map);
    return GSON.fromJson(json, EmailData.class);
  }

  /** Create DecryptedEmailContent for testing. */
  public static DecryptedEmailContent createDecryptedContent(String text, String html) {
    String json =
        GSON.toJson(
            Map.of(
                "text", text != null ? text : "",
                "html", html != null ? html : "",
                "headers", Collections.emptyMap(),
                "attachments", Collections.emptyList(),
                "links", Collections.emptyList()));
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  /**
   * Create DecryptedEmailContent with links for testing.
   *
   * @param text the plain text body
   * @param html the HTML body
   * @param links the list of URLs found in the email
   * @return DecryptedEmailContent with specified links
   */
  public static DecryptedEmailContent createDecryptedContentWithLinks(
      String text, String html, List<String> links) {
    String json =
        GSON.toJson(
            Map.of(
                "text", text != null ? text : "",
                "html", html != null ? html : "",
                "headers", Collections.emptyMap(),
                "attachments", Collections.emptyList(),
                "links", links != null ? links : Collections.emptyList()));
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  /**
   * Create DecryptedEmailContent with attachments for testing.
   *
   * @param text the plain text body
   * @param html the HTML body
   * @param attachments the list of attachment data maps
   * @return DecryptedEmailContent with specified attachments
   */
  public static DecryptedEmailContent createDecryptedContentWithAttachments(
      String text, String html, List<Map<String, Object>> attachments) {
    String json =
        GSON.toJson(
            Map.of(
                "text", text != null ? text : "",
                "html", html != null ? html : "",
                "headers", Collections.emptyMap(),
                "attachments", attachments != null ? attachments : Collections.emptyList(),
                "links", Collections.emptyList()));
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  /**
   * Create DecryptedEmailContent with auth results for testing.
   *
   * @param text the plain text body
   * @param html the HTML body
   * @param authResults the authentication results
   * @return DecryptedEmailContent with specified auth results
   */
  public static DecryptedEmailContent createDecryptedContentWithAuth(
      String text, String html, AuthResults authResults) {
    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
    map.put("text", text != null ? text : "");
    map.put("html", html != null ? html : "");
    map.put("headers", Collections.emptyMap());
    map.put("attachments", Collections.emptyList());
    map.put("links", Collections.emptyList());
    if (authResults != null) {
      // Serialize authResults separately
      String authJson = GSON.toJson(authResults);
      map.put("authResults", GSON.fromJson(authJson, Map.class));
    }
    String json = GSON.toJson(map);
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  /**
   * Create a test Email with links.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param links the list of URLs
   * @return a test Email object with links
   */
  public static Email createTestEmailWithLinks(String from, String subject, List<String> links) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content =
        createDecryptedContentWithLinks("Email body with links", null, links);
    return new Email(emailData, null, content, null);
  }

  /**
   * Create a test Email with attachments.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param attachments the list of attachment data
   * @return a test Email object with attachments
   */
  public static Email createTestEmailWithAttachments(
      String from, String subject, List<Map<String, Object>> attachments) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content =
        createDecryptedContentWithAttachments("Email body", null, attachments);
    return new Email(emailData, null, content, null);
  }

  /**
   * Create a test Email with authentication results.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param authResults the auth results
   * @return a test Email object with auth results
   */
  public static Email createTestEmailWithAuth(
      String from, String subject, AuthResults authResults) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content = createDecryptedContentWithAuth("Email body", null, authResults);
    return new Email(emailData, null, content, null);
  }

  /**
   * Create attachment data map for testing.
   *
   * @param filename the attachment filename
   * @param contentType the MIME type
   * @param content the file content
   * @return a map representing attachment data
   */
  public static Map<String, Object> createAttachmentData(
      String filename, String contentType, byte[] content) {
    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
    map.put("filename", filename);
    map.put("content_type", contentType); // snake_case for Gson @SerializedName
    map.put("size", content.length);
    map.put("content_b64", java.util.Base64.getEncoder().encodeToString(content)); // snake_case
    return map;
  }

  /** Create AuthResults for testing with passing results. */
  public static AuthResults createPassingAuthResults() {
    String json =
        """
        {
          "spf": {
            "result": "pass", "domain": "example.com",
            "ip": "192.0.2.1", "details": "sender SPF authorized"
          },
          "dkim": [{
            "result": "pass", "domain": "example.com",
            "selector": "default", "signature": "dkim=pass (test)"
          }],
          "dmarc": {
            "result": "pass", "domain": "example.com",
            "policy": "reject", "aligned": true
          },
          "reverseDns": {"result": "pass", "ip": "192.0.2.1", "hostname": "mail.example.com"}
        }
        """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults for testing with failing SPF. */
  public static AuthResults createFailingSpfAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "fail", "domain": "example.com", "ip": "192.0.2.1"},
                    "dkim": [{"result": "pass", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "pass", "domain": "example.com", "policy": "reject", "aligned": true},
                    "reverseDns": {"result": "pass", "ip": "192.0.2.1", "hostname": "mail.example.com"}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults for testing with failing DKIM. */
  public static AuthResults createFailingDkimAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "pass", "domain": "example.com", "ip": "192.0.2.1"},
                    "dkim": [{"result": "fail", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "pass", "domain": "example.com", "policy": "reject", "aligned": true},
                    "reverseDns": {"result": "pass", "ip": "192.0.2.1", "hostname": "mail.example.com"}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults for testing with failing DMARC. */
  public static AuthResults createFailingDmarcAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "pass", "domain": "example.com", "ip": "192.0.2.1"},
                    "dkim": [{"result": "pass", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "fail", "domain": "example.com", "policy": "reject"},
                    "reverseDns": {"result": "pass", "ip": "192.0.2.1", "hostname": "mail.example.com"}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults for testing with invalid reverse DNS. */
  public static AuthResults createInvalidReverseDnsAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "pass", "domain": "example.com", "ip": "192.0.2.1"},
                    "dkim": [{"result": "pass", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "pass", "domain": "example.com", "policy": "reject", "aligned": true},
                    "reverseDns": {"result": "fail", "ip": "192.0.2.1", "hostname": null}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults with all checks missing. */
  public static AuthResults createEmptyAuthResults() {
    return new AuthResults();
  }

  /** Create AuthResults where all statuses are "none" (neutral - not passing but not failing). */
  public static AuthResults createNoneStatusAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "none", "domain": "example.com"},
                    "dkim": [{"result": "none", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "none", "domain": "example.com"},
                    "reverseDns": {"result": "none", "ip": "192.0.2.1", "hostname": null}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /** Create AuthResults with SPF softfail (should be treated as neutral per spec). */
  public static AuthResults createSoftfailSpfAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "softfail", "domain": "example.com", "ip": "192.0.2.1"},
                    "dkim": [{"result": "pass", "domain": "example.com", "selector": "default"}],
                    "dmarc": {"result": "pass", "domain": "example.com", "policy": "reject", "aligned": true},
                    "reverseDns": {"result": "pass", "ip": "192.0.2.1", "hostname": "mail.example.com"}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  /**
   * Create AuthResults where all statuses are "skipped" (when email auth is disabled for inbox).
   */
  public static AuthResults createSkippedAuthResults() {
    String json =
        """
                {
                    "spf": {"result": "skipped", "domain": "example.com", "details": "SPF check disabled"},
                    "dkim": [{"result": "skipped", "info": "DKIM check disabled"}],
                    "dmarc": {"result": "skipped", "info": "DMARC check disabled"},
                    "reverseDns": {"result": "skipped", "ip": "192.0.2.1", "hostname": ""}
                }
                """;
    return GSON.fromJson(json, AuthResults.class);
  }

  // ==================== Spam Analysis Helpers ====================

  /** Create SpamAnalysisResult for a clean (non-spam) email. */
  public static SpamAnalysisResult createCleanSpamAnalysis() {
    String json =
        """
        {
          "status": "analyzed",
          "score": 0.5,
          "requiredScore": 6.0,
          "action": "no action",
          "isSpam": false,
          "symbols": [
            {"name": "DKIM_SIGNED", "score": -0.1, "description": "Valid DKIM signature"},
            {"name": "SPF_ALLOW", "score": -0.2, "description": "SPF passed"}
          ],
          "processingTimeMs": 32
        }
        """;
    return GSON.fromJson(json, SpamAnalysisResult.class);
  }

  /** Create SpamAnalysisResult for a spam email. */
  public static SpamAnalysisResult createSpamAnalysis() {
    String json =
        """
        {
          "status": "analyzed",
          "score": 12.7,
          "requiredScore": 6.0,
          "action": "reject",
          "isSpam": true,
          "symbols": [
            {"name": "RCVD_IN_SBL", "score": 6.5, "description": "Sender IP in Spamhaus blocklist"},
            {"name": "FORGED_SENDER", "score": 3.0, "description": "Sender address appears forged"},
            {"name": "SUSPICIOUS_URL", "score": 2.5, "options": ["http://malicious.example"]}
          ],
          "processingTimeMs": 78
        }
        """;
    return GSON.fromJson(json, SpamAnalysisResult.class);
  }

  /** Create SpamAnalysisResult for a suspicious (but not spam) email. */
  public static SpamAnalysisResult createSuspiciousSpamAnalysis() {
    String json =
        """
        {
          "status": "analyzed",
          "score": 4.5,
          "requiredScore": 6.0,
          "action": "add header",
          "isSpam": false,
          "symbols": [
            {"name": "MISSING_HEADERS", "score": 2.0},
            {"name": "SUSPICIOUS_LINK", "score": 2.5}
          ],
          "processingTimeMs": 45
        }
        """;
    return GSON.fromJson(json, SpamAnalysisResult.class);
  }

  /** Create SpamAnalysisResult with skipped status. */
  public static SpamAnalysisResult createSkippedSpamAnalysis() {
    String json =
        """
        {
          "status": "skipped",
          "info": "Spam analysis disabled for this inbox"
        }
        """;
    return GSON.fromJson(json, SpamAnalysisResult.class);
  }

  /** Create SpamAnalysisResult with error status. */
  public static SpamAnalysisResult createErrorSpamAnalysis() {
    String json =
        """
        {
          "status": "error",
          "processingTimeMs": 5023,
          "info": "Rspamd request timed out after 5000ms"
        }
        """;
    return GSON.fromJson(json, SpamAnalysisResult.class);
  }

  /**
   * Create DecryptedEmailContent with spam analysis results.
   *
   * @param text the plain text body
   * @param html the HTML body
   * @param spamAnalysis the spam analysis result
   * @return DecryptedEmailContent with specified spam analysis
   */
  public static DecryptedEmailContent createDecryptedContentWithSpamAnalysis(
      String text, String html, SpamAnalysisResult spamAnalysis) {
    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
    map.put("text", text != null ? text : "");
    map.put("html", html != null ? html : "");
    map.put("headers", Collections.emptyMap());
    map.put("attachments", Collections.emptyList());
    map.put("links", Collections.emptyList());
    if (spamAnalysis != null) {
      String spamJson = GSON.toJson(spamAnalysis);
      map.put("spamAnalysis", GSON.fromJson(spamJson, Map.class));
    }
    String json = GSON.toJson(map);
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  /**
   * Create a test Email with spam analysis results.
   *
   * @param from the sender email address
   * @param subject the email subject
   * @param spamAnalysis the spam analysis result
   * @return a test Email object with spam analysis
   */
  public static Email createTestEmailWithSpamAnalysis(
      String from, String subject, SpamAnalysisResult spamAnalysis) {
    EmailData emailData = createEmailData("email-" + System.nanoTime(), from, subject);
    DecryptedEmailContent content =
        createDecryptedContentWithSpamAnalysis("Email body", null, spamAnalysis);
    return new Email(emailData, null, content, null);
  }
}
