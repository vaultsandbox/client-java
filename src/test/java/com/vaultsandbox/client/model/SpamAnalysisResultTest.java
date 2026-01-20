package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for SpamAnalysisResult and related spam analysis classes. */
class SpamAnalysisResultTest {

  private static final Gson GSON = new GsonBuilder().create();

  // ==================== SpamAnalysisStatus Enum Tests ====================

  @Test
  void testSpamAnalysisStatusDeserialization() {
    assertEquals(
        SpamAnalysisStatus.ANALYZED, GSON.fromJson("\"analyzed\"", SpamAnalysisStatus.class));
    assertEquals(
        SpamAnalysisStatus.SKIPPED, GSON.fromJson("\"skipped\"", SpamAnalysisStatus.class));
    assertEquals(SpamAnalysisStatus.ERROR, GSON.fromJson("\"error\"", SpamAnalysisStatus.class));
  }

  // ==================== SpamAction Enum Tests ====================

  @Test
  void testSpamActionDeserialization() {
    assertEquals(SpamAction.NO_ACTION, GSON.fromJson("\"no action\"", SpamAction.class));
    assertEquals(SpamAction.GREYLIST, GSON.fromJson("\"greylist\"", SpamAction.class));
    assertEquals(SpamAction.ADD_HEADER, GSON.fromJson("\"add header\"", SpamAction.class));
    assertEquals(
        SpamAction.REWRITE_SUBJECT, GSON.fromJson("\"rewrite subject\"", SpamAction.class));
    assertEquals(SpamAction.SOFT_REJECT, GSON.fromJson("\"soft reject\"", SpamAction.class));
    assertEquals(SpamAction.REJECT, GSON.fromJson("\"reject\"", SpamAction.class));
  }

  // ==================== SpamSymbol Tests ====================

  @Test
  void testSpamSymbolDeserialization() {
    String json =
        """
        {
          "name": "DKIM_SIGNED",
          "score": -0.1,
          "description": "Message has a valid DKIM signature"
        }
        """;

    SpamSymbol symbol = GSON.fromJson(json, SpamSymbol.class);

    assertEquals("DKIM_SIGNED", symbol.getName());
    assertEquals(-0.1, symbol.getScore(), 0.001);
    assertEquals("Message has a valid DKIM signature", symbol.getDescription());
    assertTrue(symbol.getOptions().isEmpty());
  }

  @Test
  void testSpamSymbolWithOptions() {
    String json =
        """
        {
          "name": "SUSPICIOUS_URL",
          "score": 2.5,
          "description": "Contains suspicious URL",
          "options": ["http://malicious-site.example", "http://another-bad.site"]
        }
        """;

    SpamSymbol symbol = GSON.fromJson(json, SpamSymbol.class);

    assertEquals("SUSPICIOUS_URL", symbol.getName());
    assertEquals(2.5, symbol.getScore(), 0.001);
    assertEquals(2, symbol.getOptions().size());
    assertTrue(symbol.getOptions().contains("http://malicious-site.example"));
  }

  @Test
  void testSpamSymbolWithoutDescription() {
    String json =
        """
        {
          "name": "MISSING_HEADERS",
          "score": 2.0
        }
        """;

    SpamSymbol symbol = GSON.fromJson(json, SpamSymbol.class);

    assertEquals("MISSING_HEADERS", symbol.getName());
    assertEquals(2.0, symbol.getScore(), 0.001);
    assertNull(symbol.getDescription());
    assertTrue(symbol.getOptions().isEmpty());
  }

  // ==================== SpamAnalysisResult - Analyzed Status Tests ====================

  @Test
  void testAnalyzedCleanEmail() {
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

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.ANALYZED, result.getStatus());
    assertTrue(result.isAnalyzed());
    assertEquals(0.5, result.getScore(), 0.001);
    assertEquals(6.0, result.getRequiredScore(), 0.001);
    assertEquals(SpamAction.NO_ACTION, result.getAction());
    assertFalse(result.isSpam());
    assertEquals(2, result.getSymbols().size());
    assertEquals(32L, result.getProcessingTimeMs());
    assertNull(result.getInfo());
  }

  @Test
  void testAnalyzedSpamEmail() {
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

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.ANALYZED, result.getStatus());
    assertTrue(result.isAnalyzed());
    assertEquals(12.7, result.getScore(), 0.001);
    assertTrue(result.isSpam());
    assertEquals(SpamAction.REJECT, result.getAction());
    assertEquals(3, result.getSymbols().size());
  }

  @Test
  void testAnalyzedSuspiciousEmail() {
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

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.ANALYZED, result.getStatus());
    assertFalse(result.isSpam());
    assertEquals(SpamAction.ADD_HEADER, result.getAction());
  }

  // ==================== SpamAnalysisResult - Skipped Status Tests ====================

  @Test
  void testSkippedStatus() {
    String json =
        """
        {
          "status": "skipped",
          "info": "Spam analysis disabled for this inbox"
        }
        """;

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.SKIPPED, result.getStatus());
    assertFalse(result.isAnalyzed());
    assertNull(result.getScore());
    assertNull(result.getRequiredScore());
    assertNull(result.getAction());
    assertNull(result.isSpam());
    assertTrue(result.getSymbols().isEmpty());
    assertEquals("Spam analysis disabled for this inbox", result.getInfo());
  }

  // ==================== SpamAnalysisResult - Error Status Tests ====================

  @Test
  void testErrorStatus() {
    String json =
        """
        {
          "status": "error",
          "processingTimeMs": 5023,
          "info": "Rspamd request timed out after 5000ms"
        }
        """;

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.ERROR, result.getStatus());
    assertFalse(result.isAnalyzed());
    assertNull(result.getScore());
    assertNull(result.isSpam());
    assertEquals(5023L, result.getProcessingTimeMs());
    assertEquals("Rspamd request timed out after 5000ms", result.getInfo());
  }

  // ==================== SpamAnalysisResult Helper Methods Tests ====================

  @Test
  void testGetSpamIndicators() {
    String json =
        """
        {
          "status": "analyzed",
          "score": 8.5,
          "isSpam": true,
          "symbols": [
            {"name": "DKIM_SIGNED", "score": -0.1},
            {"name": "SPF_ALLOW", "score": -0.2},
            {"name": "RCVD_IN_SBL", "score": 6.5},
            {"name": "FORGED_SENDER", "score": 3.0},
            {"name": "NEUTRAL_SYMBOL", "score": 0.0}
          ]
        }
        """;

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);
    List<SpamSymbol> spamIndicators = result.getSpamIndicators();

    assertEquals(2, spamIndicators.size());
    assertEquals("RCVD_IN_SBL", spamIndicators.get(0).getName()); // Highest score first
    assertEquals("FORGED_SENDER", spamIndicators.get(1).getName());
  }

  @Test
  void testGetLegitimacyIndicators() {
    String json =
        """
        {
          "status": "analyzed",
          "score": 1.0,
          "isSpam": false,
          "symbols": [
            {"name": "DKIM_SIGNED", "score": -0.1},
            {"name": "DMARC_POLICY_ALLOW", "score": -0.5},
            {"name": "SPF_ALLOW", "score": -0.2},
            {"name": "MISSING_HEADERS", "score": 2.0}
          ]
        }
        """;

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);
    List<SpamSymbol> legitimacyIndicators = result.getLegitimacyIndicators();

    assertEquals(3, legitimacyIndicators.size());
    assertEquals(
        "DMARC_POLICY_ALLOW", legitimacyIndicators.get(0).getName()); // Most negative first
    assertEquals("SPF_ALLOW", legitimacyIndicators.get(1).getName());
    assertEquals("DKIM_SIGNED", legitimacyIndicators.get(2).getName());
  }

  @Test
  void testGetSpamIndicatorsWithNoSymbols() {
    SpamAnalysisResult result = new SpamAnalysisResult();
    assertTrue(result.getSpamIndicators().isEmpty());
    assertTrue(result.getLegitimacyIndicators().isEmpty());
  }

  // ==================== SpamAnalysisResult Null Safety Tests ====================

  @Test
  void testEmptyResult() {
    SpamAnalysisResult result = new SpamAnalysisResult();

    assertNull(result.getStatus());
    assertNull(result.getScore());
    assertNull(result.getRequiredScore());
    assertNull(result.getAction());
    assertNull(result.isSpam());
    assertNull(result.getProcessingTimeMs());
    assertNull(result.getInfo());
    assertNotNull(result.getSymbols());
    assertTrue(result.getSymbols().isEmpty());
  }

  @Test
  void testIsAnalyzedWithNullStatus() {
    SpamAnalysisResult result = new SpamAnalysisResult();
    assertFalse(result.isAnalyzed());
  }

  // ==================== ServerInfo spamAnalysisEnabled Tests ====================

  @Test
  void testServerInfoSpamAnalysisEnabled() {
    String json =
        """
        {
          "serverSigPk": "testKey",
          "maxTtl": 3600,
          "defaultTtl": 1800,
          "sseConsole": true,
          "spamAnalysisEnabled": true
        }
        """;

    ServerInfo serverInfo = GSON.fromJson(json, ServerInfo.class);
    assertTrue(serverInfo.isSpamAnalysisEnabled());
  }

  @Test
  void testServerInfoSpamAnalysisDisabled() {
    String json =
        """
        {
          "serverSigPk": "testKey",
          "maxTtl": 3600,
          "defaultTtl": 1800,
          "sseConsole": true,
          "spamAnalysisEnabled": false
        }
        """;

    ServerInfo serverInfo = GSON.fromJson(json, ServerInfo.class);
    assertFalse(serverInfo.isSpamAnalysisEnabled());
  }

  @Test
  void testServerInfoSpamAnalysisDefaultFalse() {
    String json =
        """
        {
          "serverSigPk": "testKey",
          "maxTtl": 3600
        }
        """;

    ServerInfo serverInfo = GSON.fromJson(json, ServerInfo.class);
    // Default boolean value is false
    assertFalse(serverInfo.isSpamAnalysisEnabled());
  }

  // ==================== Full JSON Response Examples ====================

  @Test
  void testFullAnalyzedResponseFromApiDocs() {
    // Example from the API documentation
    String json =
        """
        {
          "status": "analyzed",
          "score": 2.3,
          "requiredScore": 6.0,
          "action": "no action",
          "isSpam": false,
          "symbols": [
            {
              "name": "DKIM_SIGNED",
              "score": -0.1,
              "description": "Message has a valid DKIM signature"
            },
            {
              "name": "SPF_ALLOW",
              "score": -0.2,
              "description": "SPF check passed"
            },
            {
              "name": "MISSING_MID",
              "score": 2.5,
              "description": "Missing Message-ID header"
            }
          ],
          "processingTimeMs": 45
        }
        """;

    SpamAnalysisResult result = GSON.fromJson(json, SpamAnalysisResult.class);

    assertEquals(SpamAnalysisStatus.ANALYZED, result.getStatus());
    assertEquals(2.3, result.getScore(), 0.001);
    assertEquals(6.0, result.getRequiredScore(), 0.001);
    assertEquals(SpamAction.NO_ACTION, result.getAction());
    assertFalse(result.isSpam());
    assertEquals(3, result.getSymbols().size());
    assertEquals(45L, result.getProcessingTimeMs());

    // Check spam indicators
    List<SpamSymbol> spamIndicators = result.getSpamIndicators();
    assertEquals(1, spamIndicators.size());
    assertEquals("MISSING_MID", spamIndicators.get(0).getName());

    // Check legitimacy indicators
    List<SpamSymbol> legitimacyIndicators = result.getLegitimacyIndicators();
    assertEquals(2, legitimacyIndicators.size());
  }
}
