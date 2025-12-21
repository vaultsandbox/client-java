package com.vaultsandbox.client.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vaultsandbox.client.exception.ApiException;
import com.vaultsandbox.client.exception.EmailNotFoundException;
import com.vaultsandbox.client.exception.InboxAlreadyExistsException;
import com.vaultsandbox.client.exception.InboxNotFoundException;
import com.vaultsandbox.client.exception.NetworkException;
import com.vaultsandbox.client.model.CheckKeyResponse;
import com.vaultsandbox.client.model.DeleteAllResponse;
import com.vaultsandbox.client.model.EmailData;
import com.vaultsandbox.client.model.InboxData;
import com.vaultsandbox.client.model.RawEmailData;
import com.vaultsandbox.client.model.ServerInfo;
import com.vaultsandbox.client.model.SyncStatus;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiClient {
  private static final Logger log = LoggerFactory.getLogger(ApiClient.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient client;
  private final String baseUrl;
  private final Gson gson;
  private final int maxRetries;
  private final long retryDelayMs;
  private final Set<Integer> retryableCodes;

  public ApiClient(ApiClientConfig config) {
    this.baseUrl = config.getBaseUrl();
    this.maxRetries = config.getMaxRetries();
    this.retryDelayMs = config.getRetryDelay().toMillis();
    this.retryableCodes = config.getRetryOn();
    this.gson = new GsonBuilder().create();
    this.client = buildClient(config);
  }

  private OkHttpClient buildClient(ApiClientConfig config) {
    return new OkHttpClient.Builder()
        .connectTimeout(config.getTimeout())
        .readTimeout(config.getTimeout())
        .writeTimeout(config.getTimeout())
        .addInterceptor(
            chain -> {
              Request request =
                  chain
                      .request()
                      .newBuilder()
                      .header("X-API-Key", config.getApiKey())
                      .header("Content-Type", "application/json")
                      .header("Accept", "application/json")
                      .build();
              return chain.proceed(request);
            })
        .build();
  }

  // ==================== HTTP Methods ====================

  public <T> T get(String path, Class<T> type) {
    return executeWithRetry(() -> doGet(path, type));
  }

  public <T> T get(String path, Type type) {
    return executeWithRetry(() -> doGet(path, type));
  }

  public <T> T post(String path, Object body, Class<T> type) {
    return executeWithRetry(() -> doPost(path, body, type));
  }

  public void delete(String path) {
    executeWithRetry(
        () -> {
          doDelete(path);
          return null;
        });
  }

  public <T> T delete(String path, Class<T> type) {
    return executeWithRetry(() -> doDeleteWithResponse(path, type));
  }

  public void patch(String path, Object body) {
    executeWithRetry(
        () -> {
          doPatch(path, body);
          return null;
        });
  }

  // ==================== API Endpoints ====================

  public ServerInfo getServerInfo() {
    return get("/api/server-info", ServerInfo.class);
  }

  public CheckKeyResponse checkKey() {
    return get("/api/check-key", CheckKeyResponse.class);
  }

  public InboxData createInbox(String publicKeyB64, Long ttlSeconds, String emailAddress) {
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("clientKemPk", publicKeyB64);
    if (ttlSeconds != null) {
      payload.put("ttl", ttlSeconds);
    }
    if (emailAddress != null) {
      payload.put("emailAddress", emailAddress);
    }
    return post("/api/inboxes", payload, InboxData.class);
  }

  public DeleteAllResponse deleteAllInboxes() {
    return delete("/api/inboxes", DeleteAllResponse.class);
  }

  public void deleteInbox(String emailAddress) {
    delete("/api/inboxes/" + urlEncode(emailAddress));
  }

  public SyncStatus getSyncStatus(String emailAddress) {
    return get("/api/inboxes/" + urlEncode(emailAddress) + "/sync", SyncStatus.class);
  }

  public List<EmailData> listEmails(String emailAddress) {
    Type listType = new TypeToken<List<EmailData>>() {}.getType();
    return get("/api/inboxes/" + urlEncode(emailAddress) + "/emails", listType);
  }

  public EmailData getEmail(String emailAddress, String emailId) {
    return get(
        "/api/inboxes/" + urlEncode(emailAddress) + "/emails/" + urlEncode(emailId),
        EmailData.class);
  }

  public RawEmailData getRawEmail(String emailAddress, String emailId) {
    return get(
        "/api/inboxes/" + urlEncode(emailAddress) + "/emails/" + urlEncode(emailId) + "/raw",
        RawEmailData.class);
  }

  public void markAsRead(String emailAddress, String emailId) {
    patch(
        "/api/inboxes/" + urlEncode(emailAddress) + "/emails/" + urlEncode(emailId) + "/read",
        new Object());
  }

  public void deleteEmail(String emailAddress, String emailId) {
    delete("/api/inboxes/" + urlEncode(emailAddress) + "/emails/" + urlEncode(emailId));
  }

  // ==================== Internal Implementation ====================

  private <T> T doGet(String path, Class<T> type) {
    Request request = new Request.Builder().url(baseUrl + path).get().build();
    return executeRequest(request, type);
  }

  private <T> T doGet(String path, Type type) {
    Request request = new Request.Builder().url(baseUrl + path).get().build();
    return executeRequest(request, type);
  }

  private <T> T doPost(String path, Object body, Class<T> type) {
    RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
    Request request = new Request.Builder().url(baseUrl + path).post(requestBody).build();
    return executeRequest(request, type);
  }

  private void doDelete(String path) {
    Request request = new Request.Builder().url(baseUrl + path).delete().build();
    executeRequest(request, Void.class);
  }

  private <T> T doDeleteWithResponse(String path, Class<T> type) {
    Request request = new Request.Builder().url(baseUrl + path).delete().build();
    return executeRequest(request, type);
  }

  private void doPatch(String path, Object body) {
    RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
    Request request = new Request.Builder().url(baseUrl + path).patch(requestBody).build();
    executeRequest(request, Void.class);
  }

  private <T> T executeRequest(Request request, Class<T> type) {
    long startTime = System.currentTimeMillis();
    log.debug("{} {}", request.method(), request.url().encodedPath());
    try (Response response = client.newCall(request).execute()) {
      long elapsed = System.currentTimeMillis() - startTime;
      log.debug(
          "{} {} completed: {} ({}ms)",
          request.method(),
          request.url().encodedPath(),
          response.code(),
          elapsed);
      return handleResponse(response, type);
    } catch (IOException e) {
      long elapsed = System.currentTimeMillis() - startTime;
      log.warn(
          "{} {} failed after {}ms: {}",
          request.method(),
          request.url().encodedPath(),
          elapsed,
          e.getMessage());
      throw new NetworkException("Network error: " + e.getMessage(), e);
    }
  }

  private <T> T executeRequest(Request request, Type type) {
    long startTime = System.currentTimeMillis();
    log.debug("{} {}", request.method(), request.url().encodedPath());
    try (Response response = client.newCall(request).execute()) {
      long elapsed = System.currentTimeMillis() - startTime;
      log.debug(
          "{} {} completed: {} ({}ms)",
          request.method(),
          request.url().encodedPath(),
          response.code(),
          elapsed);
      return handleResponse(response, type);
    } catch (IOException e) {
      long elapsed = System.currentTimeMillis() - startTime;
      log.warn(
          "{} {} failed after {}ms: {}",
          request.method(),
          request.url().encodedPath(),
          elapsed,
          e.getMessage());
      throw new NetworkException("Network error: " + e.getMessage(), e);
    }
  }

  private <T> T handleResponse(Response response, Class<T> type) throws IOException {
    if (!response.isSuccessful()) {
      handleErrorResponse(response);
    }
    if (type == Void.class || response.code() == 204) {
      return null;
    }
    ResponseBody body = response.body();
    if (body == null) {
      return null;
    }
    return gson.fromJson(body.string(), type);
  }

  private <T> T handleResponse(Response response, Type type) throws IOException {
    if (!response.isSuccessful()) {
      handleErrorResponse(response);
    }
    ResponseBody body = response.body();
    if (body == null) {
      return null;
    }
    return gson.fromJson(body.string(), type);
  }

  private void handleErrorResponse(Response response) throws IOException {
    int code = response.code();
    String body = response.body() != null ? response.body().string() : "";
    log.debug("HTTP error {}: {}", code, body);

    switch (code) {
      case 400 -> throw new ApiException("Bad request: " + body, 400);
      case 401 -> throw new ApiException("Invalid API key", 401);
      case 404 -> {
        if (body.contains("inbox") || body.contains("Inbox")) {
          throw new InboxNotFoundException(extractIdentifier(body));
        } else if (body.contains("email") || body.contains("Email")) {
          throw new EmailNotFoundException(extractIdentifier(body));
        }
        throw new ApiException("Not found: " + body, 404);
      }
      case 409 -> throw new InboxAlreadyExistsException(extractIdentifier(body));
      case 429 -> throw new ApiException("Rate limited", 429);
      default -> {
        if (code >= 500) {
          throw new ApiException("Server error: " + body, code);
        }
        throw new ApiException("HTTP error " + code + ": " + body, code);
      }
    }
  }

  private <T> T executeWithRetry(Supplier<T> operation) {
    int attempt = 0;
    while (true) {
      try {
        return operation.get();
      } catch (ApiException e) {
        if (!retryableCodes.contains(e.getStatusCode()) || attempt >= maxRetries) {
          throw e;
        }
        long delay = retryDelayMs * (1L << attempt);
        log.warn(
            "Request failed (attempt {}/{}), retrying in {}ms: HTTP {}",
            attempt + 1,
            maxRetries + 1,
            delay,
            e.getStatusCode());
        sleep(delay);
        attempt++;
      } catch (NetworkException e) {
        if (attempt >= maxRetries) {
          throw e;
        }
        long delay = retryDelayMs * (1L << attempt);
        log.warn(
            "Request failed (attempt {}/{}), retrying in {}ms: {}",
            attempt + 1,
            maxRetries + 1,
            delay,
            e.getMessage());
        sleep(delay);
        attempt++;
      }
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException("Request interrupted", 0);
    }
  }

  private String extractIdentifier(String body) {
    // Try to extract an identifier from error messages like "Inbox not found: test@example.com"
    int colonIndex = body.lastIndexOf(':');
    if (colonIndex >= 0 && colonIndex < body.length() - 1) {
      return body.substring(colonIndex + 1).trim();
    }
    return body;
  }

  private String urlEncode(String value) {
    try {
      return java.net.URLEncoder.encode(value, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      return value;
    }
  }
}
