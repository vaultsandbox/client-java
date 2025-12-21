package com.vaultsandbox.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.crypto.Decryptor;
import com.vaultsandbox.client.crypto.Keypair;
import com.vaultsandbox.client.crypto.KeypairGenerator;
import com.vaultsandbox.client.crypto.SignatureVerifier;
import com.vaultsandbox.client.exception.ApiException;
import com.vaultsandbox.client.exception.InboxAlreadyExistsException;
import com.vaultsandbox.client.exception.InboxNotFoundException;
import com.vaultsandbox.client.exception.InvalidImportDataException;
import com.vaultsandbox.client.http.ApiClient;
import com.vaultsandbox.client.http.ApiClientConfig;
import com.vaultsandbox.client.model.DeleteAllResponse;
import com.vaultsandbox.client.model.ExportedInbox;
import com.vaultsandbox.client.model.InboxData;
import com.vaultsandbox.client.model.ServerInfo;
import com.vaultsandbox.client.model.SyncStatus;
import com.vaultsandbox.client.strategy.DeliveryStrategy;
import com.vaultsandbox.client.strategy.StrategyFactory;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for interacting with the VaultSandbox email testing API.
 *
 * <p>This is the main entry point for the library. Create an instance using {@link #create(String)}
 * or {@link #create(ClientConfig)}, then use it to create and manage test inboxes.
 *
 * <p>The client handles cryptographic operations for end-to-end encryption, including key
 * generation, signature verification, and message decryption. All emails are encrypted client-side
 * and can only be read by the inbox holder.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (VaultSandboxClient client = VaultSandboxClient.create("your-api-key")) {
 *     Inbox inbox = client.createInbox();
 *     Email email = inbox.waitForEmail();
 *     System.out.println(email.getSubject());
 * }
 * }</pre>
 *
 * <p>The client is thread-safe and can be shared across multiple threads.
 *
 * @see Inbox
 * @see Email
 * @see ClientConfig
 */
public final class VaultSandboxClient implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(VaultSandboxClient.class);
  private final ApiClient apiClient;
  private final ClientConfig config;
  private final Map<String, Inbox> inboxes = new ConcurrentHashMap<>();
  private final KeypairGenerator keypairGenerator;
  private final Decryptor decryptor;
  private final SignatureVerifier signatureVerifier;
  private final DeliveryStrategy deliveryStrategy;
  private final OkHttpClient httpClient;
  private final Gson gson;

  private volatile ServerInfo serverInfo;

  private VaultSandboxClient(ClientConfig config) {
    log.info("Initializing VaultSandbox client (strategy={})", config.getStrategy());
    this.config = config;
    this.httpClient = createHttpClient(config);
    this.apiClient = createApiClient(config);
    this.keypairGenerator = new KeypairGenerator();
    this.decryptor = new Decryptor();
    this.signatureVerifier = new SignatureVerifier();
    this.deliveryStrategy = StrategyFactory.create(config.getStrategy(), httpClient, config);
    this.gson = new GsonBuilder().create();
    log.debug("VaultSandbox client initialized: baseUrl={}", config.getBaseUrl());
  }

  /**
   * Creates a new client with the specified API key using default settings.
   *
   * <p>This is the simplest way to create a client. For advanced configuration options, use {@link
   * #create(ClientConfig)}.
   *
   * @param apiKey the VaultSandbox API key
   * @return a new client instance
   * @throws NullPointerException if apiKey is null
   */
  public static VaultSandboxClient create(String apiKey) {
    return new VaultSandboxClient(ClientConfig.builder().apiKey(apiKey).build());
  }

  /**
   * Creates a new client with the specified configuration.
   *
   * <p>Use this method when you need to customize client behavior such as timeouts, delivery
   * strategy (SSE or polling), or retry settings.
   *
   * @param config the client configuration
   * @return a new client instance
   * @throws NullPointerException if config or config.apiKey is null
   * @see ClientConfig.Builder
   */
  public static VaultSandboxClient create(ClientConfig config) {
    return new VaultSandboxClient(config);
  }

  /**
   * Creates a new test inbox with a unique email address.
   *
   * <p>The inbox is automatically registered with this client and can be retrieved later using
   * {@link #getInbox(String)}.
   *
   * @return a new inbox ready to receive emails
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Inbox createInbox() {
    return createInbox(CreateInboxOptions.defaults());
  }

  /**
   * Creates a new test inbox with the specified options.
   *
   * <p>The inbox is automatically registered with this client and can be retrieved later using
   * {@link #getInbox(String)}.
   *
   * @param options inbox creation options (prefix, TTL, etc.)
   * @return a new inbox ready to receive emails
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   * @see CreateInboxOptions
   */
  public Inbox createInbox(CreateInboxOptions options) {
    log.debug("Creating new inbox");
    Keypair keypair = keypairGenerator.generate();
    String publicKeyB64 = keypair.getPublicKeyBase64Url();

    Long ttlSeconds = options.getTtl() != null ? options.getTtl().getSeconds() : null;
    InboxData data = apiClient.createInbox(publicKeyB64, ttlSeconds, options.getEmailAddress());

    Inbox inbox =
        new Inbox(
            data,
            keypair,
            apiClient,
            decryptor,
            signatureVerifier,
            deliveryStrategy,
            config.getWaitTimeout());
    inboxes.put(inbox.getEmailAddress(), inbox);
    log.debug("Inbox created: {}", inbox.getEmailAddress());
    return inbox;
  }

  /**
   * Deletes an inbox by its email address.
   *
   * <p>This permanently removes the inbox from the server and removes it from this client's local
   * registry. Any emails in the inbox will be lost.
   *
   * @param emailAddress the email address of the inbox to delete
   * @throws InboxNotFoundException if the inbox does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void deleteInbox(String emailAddress) {
    log.debug("Deleting inbox: {}", emailAddress);
    apiClient.deleteInbox(emailAddress);
    inboxes.remove(emailAddress);
  }

  /**
   * Deletes all inboxes associated with this API key.
   *
   * <p>This permanently removes all inboxes from the server and clears this client's local
   * registry. All emails in all inboxes will be lost.
   *
   * @return the number of inboxes that were deleted
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public int deleteAllInboxes() {
    log.info("Deleting all inboxes");
    DeleteAllResponse response = apiClient.deleteAllInboxes();
    inboxes.clear();
    log.info("Deleted {} inbox(es)", response.getDeletedCount());
    return response.getDeletedCount();
  }

  /**
   * Returns information about the VaultSandbox server.
   *
   * <p>The server info includes version information, supported algorithms, and other server
   * capabilities. This information is cached after the first call.
   *
   * @return server information
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public ServerInfo getServerInfo() {
    if (serverInfo == null) {
      synchronized (this) {
        if (serverInfo == null) {
          serverInfo = apiClient.getServerInfo();
        }
      }
    }
    return serverInfo;
  }

  /**
   * Validates the API key with the server.
   *
   * <p>This can be used to verify that the API key is valid before performing other operations.
   *
   * @return {@code true} if the API key is valid, {@code false} otherwise
   * @throws ApiException if an unexpected API error occurs (other than 401 Unauthorized)
   * @throws NetworkException if unable to connect to the server
   */
  public boolean checkKey() {
    try {
      apiClient.checkKey();
      return true;
    } catch (ApiException e) {
      if (e.getStatusCode() == 401) {
        return false;
      }
      throw e;
    }
  }

  /**
   * Returns a previously created inbox by its email address.
   *
   * <p>Only returns inboxes that were created or imported by this client instance. To access an
   * inbox from a different session, use {@link #importInbox(ExportedInbox)} or {@link
   * #importInboxFromFile(Path)}.
   *
   * @param emailAddress the email address of the inbox
   * @return the inbox, or {@code null} if not found in this client's registry
   */
  public Inbox getInbox(String emailAddress) {
    return inboxes.get(emailAddress);
  }

  /**
   * Returns the configuration used by this client.
   *
   * @return the client configuration
   */
  public ClientConfig getConfig() {
    return config;
  }

  // ==================== Export/Import ====================

  /**
   * Exports an inbox's credentials for later import.
   *
   * <p>The exported data contains the cryptographic keys needed to decrypt emails in the inbox.
   * This can be used to persist inbox access across sessions or share access with other processes.
   *
   * <p><strong>Security note:</strong> The exported data contains sensitive cryptographic keys.
   * Store it securely and do not expose it to untrusted parties.
   *
   * @param inbox the inbox to export
   * @return the exported inbox data
   * @see #importInbox(ExportedInbox)
   * @see #exportInboxToFile(Inbox, Path)
   */
  public ExportedInbox exportInbox(Inbox inbox) {
    log.debug("Exporting inbox: {}", inbox.getEmailAddress());
    ExportedInbox exported = new ExportedInbox();
    exported.setEmailAddress(inbox.getEmailAddress());
    exported.setExpiresAt(inbox.getExpiresAt().toString());
    exported.setInboxHash(inbox.getHash());
    exported.setServerSigPk(inbox.getServerSigPk());
    exported.setPublicKeyB64(Base64Url.encode(inbox.getKeypair().getPublicKey()));
    exported.setSecretKeyB64(Base64Url.encode(inbox.getKeypair().getSecretKey()));
    exported.setExportedAt(Instant.now().toString());
    return exported;
  }

  /**
   * Exports an inbox's credentials by email address.
   *
   * <p>This is a convenience method that looks up the inbox by email address and exports it.
   *
   * <p><strong>Security note:</strong> The exported data contains sensitive cryptographic keys.
   * Store it securely and do not expose it to untrusted parties.
   *
   * @param emailAddress the email address of the inbox to export
   * @return the exported inbox data
   * @throws InboxNotFoundException if no inbox with the given email address is registered
   * @see #exportInbox(Inbox)
   * @see #importInbox(ExportedInbox)
   */
  public ExportedInbox exportInbox(String emailAddress) {
    Inbox inbox = inboxes.get(emailAddress);
    if (inbox == null) {
      throw new InboxNotFoundException(emailAddress);
    }
    return exportInbox(inbox);
  }

  /**
   * Exports an inbox's credentials to a file.
   *
   * <p>This is a convenience method that exports the inbox and writes the JSON to a file.
   *
   * <p><strong>Security note:</strong> The file will contain sensitive cryptographic keys. Ensure
   * appropriate file permissions and do not expose it to untrusted parties.
   *
   * @param inbox the inbox to export
   * @param path the file path to write the exported data to
   * @throws IOException if an I/O error occurs writing to the file
   * @see #importInboxFromFile(Path)
   */
  public void exportInboxToFile(Inbox inbox, Path path) throws IOException {
    ExportedInbox exported = exportInbox(inbox);
    String json = gson.toJson(exported);
    Files.writeString(path, json, StandardCharsets.UTF_8);
  }

  /**
   * Imports a previously exported inbox.
   *
   * <p>This restores access to an inbox using credentials that were previously exported. The inbox
   * must still exist on the server (not expired or deleted).
   *
   * @param data the exported inbox data
   * @return the imported inbox
   * @throws InvalidImportDataException if the import data is invalid or the inbox no longer exists
   * @throws InboxAlreadyExistsException if the inbox is already registered with this client
   * @see #exportInbox(Inbox)
   */
  public Inbox importInbox(ExportedInbox data) throws InvalidImportDataException {
    log.debug("Importing inbox: {}", data.getEmailAddress());
    // Validate the import data
    data.validate();

    // Check if inbox already exists locally
    if (inboxes.containsKey(data.getEmailAddress())) {
      throw new InboxAlreadyExistsException(data.getEmailAddress());
    }

    // Verify inbox still exists on server
    try {
      apiClient.get(
          "/api/inboxes/" + urlEncode(data.getEmailAddress()) + "/sync", SyncStatus.class);
    } catch (InboxNotFoundException e) {
      throw new InvalidImportDataException("Inbox no longer exists on server");
    }

    // Reconstruct keypair
    byte[] publicKey = Base64Url.decode(data.getPublicKeyB64());
    byte[] secretKey = Base64Url.decode(data.getSecretKeyB64());
    Keypair keypair = new Keypair(publicKey, secretKey);

    // Create inbox object
    InboxData inboxData = new InboxData();
    inboxData.setEmailAddress(data.getEmailAddress());
    inboxData.setExpiresAt(data.getExpiresAt());
    inboxData.setInboxHash(data.getInboxHash());
    inboxData.setServerSigPk(data.getServerSigPk());

    Inbox inbox =
        new Inbox(
            inboxData,
            keypair,
            apiClient,
            decryptor,
            signatureVerifier,
            deliveryStrategy,
            config.getWaitTimeout());
    inboxes.put(inbox.getEmailAddress(), inbox);
    log.debug("Inbox imported: {}", inbox.getEmailAddress());

    return inbox;
  }

  /**
   * Imports an inbox from a file.
   *
   * <p>This is a convenience method that reads the exported data from a file and imports the inbox.
   *
   * @param path the file path to read the exported data from
   * @return the imported inbox
   * @throws IOException if an I/O error occurs reading the file
   * @throws InvalidImportDataException if the import data is invalid or the inbox no longer exists
   * @throws InboxAlreadyExistsException if the inbox is already registered with this client
   * @see #exportInboxToFile(Inbox, Path)
   */
  public Inbox importInboxFromFile(Path path) throws IOException, InvalidImportDataException {
    String json = Files.readString(path, StandardCharsets.UTF_8);
    ExportedInbox data = gson.fromJson(json, ExportedInbox.class);
    return importInbox(data);
  }

  // ==================== Multi-Inbox Monitoring ====================

  /**
   * Creates a monitor for receiving emails from multiple inboxes.
   *
   * <p>The monitor allows you to register callbacks that will be invoked when emails arrive in any
   * of the monitored inboxes. This is useful for testing scenarios that involve multiple
   * recipients.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * InboxMonitor monitor = client.monitorInboxes(inbox1, inbox2, inbox3);
   * monitor.onEmail(email -> System.out.println("Got email: " + email.getSubject()));
   * }</pre>
   *
   * @param inboxes the inboxes to monitor
   * @return an inbox monitor
   * @see InboxMonitor
   */
  public InboxMonitor monitorInboxes(Inbox... inboxes) {
    return new InboxMonitor(Arrays.asList(inboxes), deliveryStrategy);
  }

  /**
   * Creates a monitor for receiving emails from multiple inboxes.
   *
   * @param inboxes the list of inboxes to monitor
   * @return an inbox monitor
   * @see #monitorInboxes(Inbox...)
   */
  public InboxMonitor monitorInboxes(List<Inbox> inboxes) {
    return new InboxMonitor(inboxes, deliveryStrategy);
  }

  /**
   * Closes this client and releases associated resources.
   *
   * <p>This clears the local inbox registry and shuts down the delivery strategy (SSE connections
   * or polling threads). Inboxes on the server are not deleted.
   */
  @Override
  public void close() {
    log.info("Closing VaultSandbox client");
    inboxes.clear();
    deliveryStrategy.close();
  }

  private OkHttpClient createHttpClient(ClientConfig config) {
    return new OkHttpClient.Builder()
        .connectTimeout(config.getHttpTimeout())
        .readTimeout(config.getHttpTimeout())
        .writeTimeout(config.getHttpTimeout())
        .build();
  }

  private ApiClient createApiClient(ClientConfig config) {
    return new ApiClient(
        ApiClientConfig.builder()
            .baseUrl(config.getBaseUrl())
            .apiKey(config.getApiKey())
            .timeout(config.getHttpTimeout())
            .maxRetries(config.getMaxRetries())
            .retryDelay(config.getRetryDelay())
            .retryOn(config.getRetryOn())
            .build());
  }

  private String urlEncode(String value) {
    try {
      return java.net.URLEncoder.encode(value, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      return value;
    }
  }
}
