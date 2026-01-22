package com.vaultsandbox.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.vaultsandbox.client.exception.ApiException;
import com.vaultsandbox.client.exception.EmailNotFoundException;
import com.vaultsandbox.client.exception.InboxAlreadyExistsException;
import com.vaultsandbox.client.exception.InboxNotFoundException;
import com.vaultsandbox.client.model.CheckKeyResponse;
import com.vaultsandbox.client.model.EmailData;
import com.vaultsandbox.client.model.InboxData;
import com.vaultsandbox.client.model.ServerInfo;
import com.vaultsandbox.client.model.SyncStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiClientTest {
  private MockWebServer mockServer;
  private ApiClient apiClient;
  private Gson gson;

  @BeforeEach
  void setUp() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();

    ApiClientConfig config =
        ApiClientConfig.builder()
            .baseUrl(mockServer.url("/").toString().replaceAll("/$", ""))
            .apiKey("test-api-key")
            .timeout(Duration.ofSeconds(5))
            .maxRetries(2)
            .retryDelay(Duration.ofMillis(100))
            .build();

    apiClient = new ApiClient(config);
    gson = new Gson();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockServer.shutdown();
  }

  // ==================== Server Info ====================

  @Test
  void testGetServerInfo() throws InterruptedException {
    String json =
        """
            {
                "version": "1.0.0",
                "domain": "vaultsandbox.com",
                "algorithms": {
                    "kem": "ML-KEM-768",
                    "sig": "ML-DSA-65",
                    "aead": "AES-256-GCM",
                    "kdf": "HKDF-SHA-512"
                },
                "limits": {
                    "maxInboxes": 10,
                    "maxEmailsPerInbox": 100,
                    "inboxTtlSeconds": 3600
                }
            }
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(200));

    ServerInfo info = apiClient.getServerInfo();

    assertNotNull(info);
    assertEquals("1.0.0", info.getVersion());
    assertEquals("vaultsandbox.com", info.getDomain());
    assertNotNull(info.getAlgorithms());
    assertEquals("ML-KEM-768", info.getAlgorithms().getKem());
    assertNotNull(info.getLimits());
    assertEquals(10, info.getLimits().getMaxInboxes());

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("GET", request.getMethod());
    assertEquals("/api/server-info", request.getPath());
    assertEquals("test-api-key", request.getHeader("X-API-Key"));
  }

  // ==================== Check Key ====================

  @Test
  void testCheckKey() {
    String json = """
            {"valid": true, "message": "API key is valid"}
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(200));

    CheckKeyResponse response = apiClient.checkKey();

    assertTrue(response.isValid());
    assertEquals("API key is valid", response.getMessage());
  }

  // ==================== Inbox Operations ====================

  @Test
  void testCreateInbox() throws InterruptedException {
    String json =
        """
            {
                "emailAddress": "test123@vaultsandbox.com",
                "expiresAt": "2024-01-01T00:00:00Z",
                "inboxHash": "abc123",
                "serverSigPk": "base64urlkey"
            }
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(201));

    InboxData inbox =
        apiClient.createInbox("testPublicKeyBase64", null, null, null, null, null, null);

    assertNotNull(inbox);
    assertEquals("test123@vaultsandbox.com", inbox.getEmailAddress());
    assertEquals("abc123", inbox.getInboxHash());

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/api/inboxes", request.getPath());
  }

  @Test
  void testDeleteInbox() throws InterruptedException {
    mockServer.enqueue(new MockResponse().setResponseCode(204));

    apiClient.deleteInbox("test@vaultsandbox.com");

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("DELETE", request.getMethod());
    assertTrue(request.getPath().contains("/api/inboxes/"));
  }

  // ==================== Email Operations ====================

  @Test
  void testListEmails() throws InterruptedException {
    String json =
        """
            [
                {
                    "id": "email1",
                    "inboxId": "inbox-hash",
                    "receivedAt": "2024-01-01T12:00:00Z",
                    "isRead": false
                }
            ]
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(200));

    List<EmailData> emails = apiClient.listEmails("test@vaultsandbox.com", false);

    assertNotNull(emails);
    assertEquals(1, emails.size());
    assertEquals("email1", emails.get(0).getId());
    assertFalse(emails.get(0).isRead());

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("GET", request.getMethod());
    assertFalse(request.getPath().contains("includeContent"));
  }

  @Test
  void testGetEmail() {
    String json =
        """
            {
                "id": "email1",
                "inboxId": "inbox-hash",
                "receivedAt": "2024-01-01T12:00:00Z",
                "isRead": true
            }
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(200));

    EmailData email = apiClient.getEmail("test@vaultsandbox.com", "email1");

    assertNotNull(email);
    assertEquals("email1", email.getId());
    assertTrue(email.isRead());
  }

  @Test
  void testMarkAsRead() throws InterruptedException {
    mockServer.enqueue(new MockResponse().setResponseCode(204));

    apiClient.markAsRead("test@vaultsandbox.com", "email1");

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("PATCH", request.getMethod());
    assertTrue(request.getPath().contains("/read"));
  }

  @Test
  void testDeleteEmail() throws InterruptedException {
    mockServer.enqueue(new MockResponse().setResponseCode(204));

    apiClient.deleteEmail("test@vaultsandbox.com", "email1");

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("DELETE", request.getMethod());
  }

  // ==================== Sync Status ====================

  @Test
  void testGetSyncStatus() {
    String json =
        """
            {
                "emailsHash": "hash123",
                "emailCount": 5
            }
            """;
    mockServer.enqueue(new MockResponse().setBody(json).setResponseCode(200));

    SyncStatus status = apiClient.getSyncStatus("test@vaultsandbox.com");

    assertNotNull(status);
    assertEquals("hash123", status.getEmailsHash());
    assertEquals(5, status.getEmailCount());
  }

  // ==================== Retry Logic ====================

  @Test
  void testRetryOnServerError() {
    mockServer.enqueue(new MockResponse().setResponseCode(503));
    mockServer.enqueue(new MockResponse().setResponseCode(503));
    mockServer.enqueue(new MockResponse().setBody("{}").setResponseCode(200));

    ServerInfo info = apiClient.getServerInfo();

    assertNotNull(info);
    assertEquals(3, mockServer.getRequestCount());
  }

  @Test
  void testRetryExhausted() {
    mockServer.enqueue(new MockResponse().setResponseCode(503));
    mockServer.enqueue(new MockResponse().setResponseCode(503));
    mockServer.enqueue(new MockResponse().setResponseCode(503));

    ApiException ex = assertThrows(ApiException.class, () -> apiClient.getServerInfo());
    assertEquals(503, ex.getStatusCode());
    assertEquals(3, mockServer.getRequestCount()); // Initial + 2 retries
  }

  @Test
  void testNoRetryOn404() {
    mockServer.enqueue(
        new MockResponse()
            .setBody("{\"error\": \"Inbox not found: test@example.com\"}")
            .setResponseCode(404));

    assertThrows(InboxNotFoundException.class, () -> apiClient.getSyncStatus("test@example.com"));

    assertEquals(1, mockServer.getRequestCount());
  }

  @Test
  void testRetryOn429() {
    mockServer.enqueue(new MockResponse().setResponseCode(429));
    mockServer.enqueue(new MockResponse().setBody("{}").setResponseCode(200));

    ServerInfo info = apiClient.getServerInfo();

    assertNotNull(info);
    assertEquals(2, mockServer.getRequestCount());
  }

  // ==================== Error Handling ====================

  @Test
  void testInvalidApiKey() {
    mockServer.enqueue(new MockResponse().setResponseCode(401));

    ApiException ex = assertThrows(ApiException.class, () -> apiClient.getServerInfo());
    assertEquals(401, ex.getStatusCode());
    assertTrue(ex.getMessage().contains("Invalid API key"));
  }

  @Test
  void testInboxNotFound() {
    mockServer.enqueue(
        new MockResponse().setBody("{\"error\": \"Inbox not found\"}").setResponseCode(404));

    assertThrows(
        InboxNotFoundException.class,
        () -> apiClient.getSyncStatus("nonexistent@vaultsandbox.com"));
  }

  @Test
  void testEmailNotFound() {
    mockServer.enqueue(
        new MockResponse().setBody("{\"error\": \"Email not found\"}").setResponseCode(404));

    assertThrows(
        EmailNotFoundException.class,
        () -> apiClient.getEmail("test@vaultsandbox.com", "nonexistent"));
  }

  @Test
  void testInboxAlreadyExists() {
    mockServer.enqueue(
        new MockResponse().setBody("{\"error\": \"Inbox already exists\"}").setResponseCode(409));

    assertThrows(
        InboxAlreadyExistsException.class,
        () -> apiClient.createInbox("testPublicKeyBase64", null, null, null, null, null, null));
  }

  @Test
  void testBadRequest() {
    mockServer.enqueue(
        new MockResponse().setBody("{\"error\": \"Invalid request\"}").setResponseCode(400));

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> apiClient.createInbox("testPublicKeyBase64", null, null, null, null, null, null));
    assertEquals(400, ex.getStatusCode());
  }
}
