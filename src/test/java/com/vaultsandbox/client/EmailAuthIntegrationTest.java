package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.model.AuthResults;
import com.vaultsandbox.client.model.AuthValidation;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

/**
 * End-to-end integration tests for email authentication (SPF, DKIM, DMARC, Reverse DNS).
 *
 * <p>These tests use the test email API (POST /api/test/emails) to create test emails with
 * controlled authentication results, then verify the SDK correctly parses those results.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>Server running with VSB_DEVELOPMENT=true
 *   <li>VAULTSANDBOX_API_KEY environment variable set
 *   <li>VAULTSANDBOX_URL environment variable (defaults to http://localhost:3000)
 * </ul>
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class EmailAuthIntegrationTest {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final Gson GSON = new GsonBuilder().create();

  private VaultSandboxClient client;
  private OkHttpClient httpClient;
  private String baseUrl;
  private String apiKey;

  @BeforeAll
  void setUpClass() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for E2E auth tests");

    apiKey = TestConfig.getApiKey();
    baseUrl = TestConfig.getBaseUrl();

    client =
        VaultSandboxClient.create(ClientConfig.builder().apiKey(apiKey).baseUrl(baseUrl).build());

    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(30))
            .build();
  }

  @AfterAll
  void tearDownClass() {
    if (client != null) {
      client.close();
    }
  }

  /**
   * Send a test email using the test email API.
   *
   * @param to the inbox email address
   * @param auth optional auth configuration (null for all pass)
   * @return the email ID from the response
   */
  private String sendTestEmail(String to, Map<String, Object> auth) throws IOException {
    Map<String, Object> body = new HashMap<>();
    body.put("to", to);
    body.put("subject", "Auth Test Email");
    body.put("text", "Test email for authentication testing");
    if (auth != null) {
      body.put("auth", auth);
    }

    RequestBody requestBody = RequestBody.create(GSON.toJson(body), JSON);
    Request request =
        new Request.Builder()
            .url(baseUrl + "/api/test/emails")
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      ResponseBody responseBody = response.body();
      if (!response.isSuccessful()) {
        String errorBody = responseBody != null ? responseBody.string() : "";
        if (response.code() == 404) {
          throw new IOException(
              "Test email API not available (404). Ensure server is running with VSB_DEVELOPMENT=true");
        }
        throw new IOException("Test email API failed: " + response.code() + " - " + errorBody);
      }

      if (responseBody == null) {
        throw new IOException("Response body is null");
      }
      String responseBodyStr = responseBody.string();
      @SuppressWarnings("unchecked")
      Map<String, String> result = GSON.fromJson(responseBodyStr, Map.class);
      return result.get("emailId");
    }
  }

  @Nested
  @DisplayName("All Auth Passing")
  class AllAuthPassingTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("All authentication checks pass by default")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAllAuthPassByDefault() throws Exception {
      inbox = client.createInbox();

      // Send test email with default auth (all pass)
      sendTestEmail(inbox.getEmailAddress(), null);

      // Wait for and retrieve the email
      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));

      assertNotNull(email, "Email should be received");
      assertNotNull(email.getAuthResults(), "Auth results should be present");

      AuthResults auth = email.getAuthResults();
      AuthValidation validation = auth.validate();

      assertTrue(validation.isFullyAuthenticated(), "All checks should pass");
      assertTrue(validation.hasSpf(), "SPF should pass");
      assertTrue(validation.hasDkim(), "DKIM should pass");
      assertTrue(validation.hasDmarc(), "DMARC should pass");
      assertTrue(validation.hasReverseDns(), "Reverse DNS should pass");
      assertTrue(validation.getFailed().isEmpty(), "No failures should be present");
    }

    @Test
    @DisplayName("SPF result field is 'result' not 'status'")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfResultField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getSpf(), "SPF result should be present");
      assertEquals("pass", auth.getSpf().getResult(), "SPF result should be 'pass'");
      assertNotNull(auth.getSpf().getDomain(), "SPF domain should be present");
    }

    @Test
    @DisplayName("DKIM result field is 'result' not 'status'")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDkimResultField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getDkim(), "DKIM results should be present");
      assertFalse(auth.getDkim().isEmpty(), "DKIM should have at least one signature");
      assertEquals("pass", auth.getDkim().get(0).getResult(), "DKIM result should be 'pass'");
      assertNotNull(auth.getDkim().get(0).getSignature(), "DKIM signature should be present");
    }

    @Test
    @DisplayName("DMARC result field is 'result' not 'status'")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDmarcResultField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getDmarc(), "DMARC result should be present");
      assertEquals("pass", auth.getDmarc().getResult(), "DMARC result should be 'pass'");
      assertTrue(auth.getDmarc().isAligned(), "DMARC should be aligned");
    }

    @Test
    @DisplayName("ReverseDNS uses 'verified' boolean not 'status' string")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testReverseDnsVerifiedField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getReverseDns(), "ReverseDNS result should be present");
      assertTrue(auth.getReverseDns().isVerified(), "ReverseDNS should be verified");
      assertNotNull(auth.getReverseDns().getHostname(), "Hostname should be present");
      assertNotNull(auth.getReverseDns().getIp(), "IP should be present");
    }
  }

  @Nested
  @DisplayName("SPF Failure Tests")
  class SpfFailureTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("SPF fail is detected correctly")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfFail() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "fail");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasSpf(), "SPF should not pass");
      assertTrue(
          validation.getFailed().stream().anyMatch(s -> s.contains("SPF")),
          "SPF failure should be in failed list");
      assertEquals("fail", authResults.getSpf().getResult(), "SPF result should be 'fail'");
    }

    @Test
    @DisplayName("SPF softfail is treated as neutral")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfSoftfail() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "softfail");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasSpf(), "SPF softfail should not count as pass");
      assertFalse(
          validation.getFailed().stream().anyMatch(s -> s.contains("SPF")),
          "SPF softfail should not be in failed list (neutral)");
      assertEquals("softfail", authResults.getSpf().getResult(), "SPF result should be 'softfail'");
    }

    @Test
    @DisplayName("SPF none is treated as neutral")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfNone() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "none");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasSpf(), "SPF none should not count as pass");
      assertFalse(
          validation.getFailed().stream().anyMatch(s -> s.contains("SPF")),
          "SPF none should not be in failed list (neutral)");
    }
  }

  @Nested
  @DisplayName("DKIM Failure Tests")
  class DkimFailureTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("DKIM fail is detected correctly")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDkimFail() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("dkim", "fail");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasDkim(), "DKIM should not pass");
      assertTrue(
          validation.getFailed().stream().anyMatch(s -> s.contains("DKIM")),
          "DKIM failure should be in failed list");
      assertEquals(
          "fail", authResults.getDkim().get(0).getResult(), "DKIM result should be 'fail'");
    }

    @Test
    @DisplayName("DKIM none is treated as neutral")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDkimNone() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("dkim", "none");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasDkim(), "DKIM none should not count as pass");
      assertFalse(
          validation.getFailed().stream().anyMatch(s -> s.contains("DKIM")),
          "DKIM none should not be in failed list (neutral)");
    }
  }

  @Nested
  @DisplayName("DMARC Failure Tests")
  class DmarcFailureTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("DMARC fail is detected correctly")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDmarcFail() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("dmarc", "fail");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasDmarc(), "DMARC should not pass");
      assertTrue(
          validation.getFailed().stream().anyMatch(s -> s.contains("DMARC")),
          "DMARC failure should be in failed list");
      assertEquals("fail", authResults.getDmarc().getResult(), "DMARC result should be 'fail'");
    }

    @Test
    @DisplayName("DMARC none is treated as neutral")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDmarcNone() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("dmarc", "none");

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasDmarc(), "DMARC none should not count as pass");
      assertFalse(
          validation.getFailed().stream().anyMatch(s -> s.contains("DMARC")),
          "DMARC none should not be in failed list (neutral)");
    }
  }

  @Nested
  @DisplayName("Reverse DNS Failure Tests")
  class ReverseDnsFailureTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("Reverse DNS not verified is detected correctly")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testReverseDnsNotVerified() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("reverseDns", false);

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasReverseDns(), "ReverseDNS should not pass");
      assertTrue(
          validation.getFailed().stream().anyMatch(s -> s.contains("ReverseDNS")),
          "ReverseDNS failure should be in failed list");
      assertFalse(authResults.getReverseDns().isVerified(), "ReverseDNS should not be verified");
    }
  }

  @Nested
  @DisplayName("Mixed Auth Results Tests")
  class MixedAuthResultsTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("All auth failing")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAllAuthFailing() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "fail");
      auth.put("dkim", "fail");
      auth.put("dmarc", "fail");
      auth.put("reverseDns", false);

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.isFullyAuthenticated(), "Not fully authenticated");
      assertFalse(validation.hasSpf());
      assertFalse(validation.hasDkim());
      assertFalse(validation.hasDmarc());
      assertFalse(validation.hasReverseDns());
      assertEquals(4, validation.getFailed().size(), "All 4 checks should fail");
    }

    @Test
    @DisplayName("SPF softfail with other passing")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfSoftfailWithOthersPassing() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "softfail");
      auth.put("dkim", "pass");
      auth.put("dmarc", "pass");
      auth.put("reverseDns", true);

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.hasSpf(), "SPF softfail should not pass");
      assertTrue(validation.hasDkim(), "DKIM should pass");
      assertTrue(validation.hasDmarc(), "DMARC should pass");
      assertTrue(validation.hasReverseDns(), "ReverseDNS should pass");

      // SPF softfail is neutral, not in failed list
      assertTrue(validation.getFailed().isEmpty(), "No failures expected (softfail is neutral)");
    }

    @Test
    @DisplayName("DMARC fail with others passing")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDmarcFailWithOthersPassing() throws Exception {
      inbox = client.createInbox();

      Map<String, Object> auth = new HashMap<>();
      auth.put("spf", "pass");
      auth.put("dkim", "pass");
      auth.put("dmarc", "fail");
      auth.put("reverseDns", true);

      sendTestEmail(inbox.getEmailAddress(), auth);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults authResults = email.getAuthResults();
      AuthValidation validation = authResults.validate();

      assertFalse(validation.isFullyAuthenticated(), "Not fully authenticated");
      assertTrue(validation.hasSpf(), "SPF should pass");
      assertTrue(validation.hasDkim(), "DKIM should pass");
      assertFalse(validation.hasDmarc(), "DMARC should not pass");
      assertTrue(validation.hasReverseDns(), "ReverseDNS should pass");
      assertEquals(1, validation.getFailed().size(), "Only DMARC should fail");
    }
  }

  @Nested
  @DisplayName("Wire Format Verification")
  class WireFormatVerificationTests {

    private Inbox inbox;

    @AfterEach
    void tearDown() {
      if (inbox != null) {
        try {
          client.deleteInbox(inbox.getEmailAddress());
        } catch (Exception ignored) {
        }
      }
    }

    @Test
    @DisplayName("SPF details field is populated")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSpfDetailsField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      // The test email API should populate the details field
      assertNotNull(auth.getSpf().getDetails(), "SPF details should be present");
    }

    @Test
    @DisplayName("DKIM signature field is populated")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDkimSignatureField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getDkim().get(0).getSignature(), "DKIM signature should be present");
    }

    @Test
    @DisplayName("DKIM selector field is populated")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDkimSelectorField() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getDkim().get(0).getSelector(), "DKIM selector should be present");
    }

    @Test
    @DisplayName("DMARC policy and aligned fields are populated")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDmarcPolicyAndAlignedFields() throws Exception {
      inbox = client.createInbox();
      sendTestEmail(inbox.getEmailAddress(), null);

      Email email = inbox.waitForEmail(EmailFilter.any(), Duration.ofSeconds(15));
      AuthResults auth = email.getAuthResults();

      assertNotNull(auth.getDmarc().getPolicy(), "DMARC policy should be present");
      assertNotNull(auth.getDmarc().getAligned(), "DMARC aligned should be present");
    }
  }
}
