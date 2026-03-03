package com.vaultsandbox.client;

import com.google.gson.Gson;
import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.crypto.Decryptor;
import com.vaultsandbox.client.crypto.Keypair;
import com.vaultsandbox.client.crypto.SignatureVerifier;
import com.vaultsandbox.client.exception.InboxExpiredException;
import com.vaultsandbox.client.exception.InboxNotFoundException;
import com.vaultsandbox.client.exception.SignatureVerificationException;
import com.vaultsandbox.client.exception.TimeoutException;
import com.vaultsandbox.client.exception.WebhookNotFoundException;
import com.vaultsandbox.client.http.ApiClient;
import com.vaultsandbox.client.internal.GsonProvider;
import com.vaultsandbox.client.model.ChaosConfig;
import com.vaultsandbox.client.model.CreateWebhookRequest;
import com.vaultsandbox.client.model.DecryptedEmailContent;
import com.vaultsandbox.client.model.DecryptedEmailMetadata;
import com.vaultsandbox.client.model.EmailData;
import com.vaultsandbox.client.model.EmailMetadata;
import com.vaultsandbox.client.model.EncryptedPayload;
import com.vaultsandbox.client.model.ExportedInbox;
import com.vaultsandbox.client.model.InboxData;
import com.vaultsandbox.client.model.RawEmailData;
import com.vaultsandbox.client.model.RotateSecretResponse;
import com.vaultsandbox.client.model.SyncStatus;
import com.vaultsandbox.client.model.TestWebhookResponse;
import com.vaultsandbox.client.model.UpdateWebhookRequest;
import com.vaultsandbox.client.model.WebhookData;
import com.vaultsandbox.client.model.WebhookEventType;
import com.vaultsandbox.client.model.WebhookListResponse;
import com.vaultsandbox.client.strategy.DeliveryStrategy;
import com.vaultsandbox.client.strategy.EmailFilter;
import com.vaultsandbox.client.strategy.Subscription;
import com.vaultsandbox.client.strategy.WaitOptions;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents a test email inbox.
 *
 * <p>An inbox has a unique email address that can receive emails. Emails are end-to-end encrypted
 * and can only be decrypted by this inbox's private key. The inbox provides methods for:
 *
 * <ul>
 *   <li>Listing and retrieving emails
 *   <li>Waiting for emails to arrive (blocking or callback-based)
 *   <li>Managing email state (mark as read, delete)
 *   <li>Exporting inbox credentials for later import
 * </ul>
 *
 * <p>Inboxes are created using {@link VaultSandboxClient#createInbox()} and have a limited lifetime
 * defined by the server (typically a few hours).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Inbox inbox = client.createInbox();
 * System.out.println("Send emails to: " + inbox.getEmailAddress());
 *
 * // Wait for an email to arrive
 * Email email = inbox.waitForEmail();
 * System.out.println("Received: " + email.getSubject());
 *
 * // Or filter by criteria
 * Email verification = inbox.waitForEmail(
 *     EmailFilter.builder().subject("Verify your email").build()
 * );
 * }</pre>
 *
 * @see VaultSandboxClient#createInbox()
 * @see Email
 * @see EmailFilter
 */
public class Inbox {
  private final String emailAddress;
  private final Instant expiresAt;
  private final String inboxHash;
  private final String serverSigPk;
  private final Keypair keypair;
  private final boolean encrypted;
  private final boolean persistent;
  private final boolean emailAuth;

  private final ApiClient apiClient;
  private final Decryptor decryptor;
  private final SignatureVerifier signatureVerifier;
  private final DeliveryStrategy strategy;
  private final Duration defaultTimeout;
  private final Gson gson;
  private final Runnable onDelete;

  Inbox(
      InboxData data,
      Keypair keypair,
      ApiClient apiClient,
      Decryptor decryptor,
      SignatureVerifier signatureVerifier,
      DeliveryStrategy strategy,
      Duration defaultTimeout) {
    this(data, keypair, apiClient, decryptor, signatureVerifier, strategy, defaultTimeout, null);
  }

  Inbox(
      InboxData data,
      Keypair keypair,
      ApiClient apiClient,
      Decryptor decryptor,
      SignatureVerifier signatureVerifier,
      DeliveryStrategy strategy,
      Duration defaultTimeout,
      Runnable onDelete) {
    this.emailAddress = data.getEmailAddress();
    this.expiresAt = Instant.parse(data.getExpiresAt());
    this.inboxHash = data.getInboxHash();
    this.serverSigPk = data.getServerSigPk();
    this.keypair = keypair;
    this.encrypted = data.isEncrypted();
    this.persistent = data.isPersistent();
    this.emailAuth = data.getEmailAuth() != null && data.getEmailAuth();
    this.apiClient = apiClient;
    this.decryptor = decryptor;
    this.signatureVerifier = signatureVerifier;
    this.strategy = strategy;
    this.defaultTimeout = defaultTimeout;
    this.gson = GsonProvider.get();
    this.onDelete = onDelete;
  }

  /**
   * Lists all emails in this inbox.
   *
   * <p>Returns fully hydrated emails with complete content including body text, HTML, headers,
   * attachments, links, and authentication results.
   *
   * @return a list of emails, ordered by received time (newest first)
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public List<Email> listEmails() {
    checkNotExpired();
    List<EmailData> emailsData = apiClient.listEmails(emailAddress, true);
    return emailsData.stream().map(this::decryptEmail).collect(Collectors.toList());
  }

  /**
   * Lists all emails in this inbox, returning only metadata (no body content).
   *
   * <p>This is more efficient than {@link #listEmails()} when you only need metadata like sender,
   * subject, and received time.
   *
   * @return a list of email metadata, ordered by received time (newest first)
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public List<EmailMetadata> listEmailsMetadataOnly() {
    checkNotExpired();
    List<EmailData> emailsData = apiClient.listEmails(emailAddress, false);
    return emailsData.stream().map(this::decryptToMetadata).collect(Collectors.toList());
  }

  /**
   * Retrieves a specific email by its ID.
   *
   * <p>Returns the complete email including body text, HTML, headers, attachments, and
   * authentication results.
   *
   * @param emailId the unique identifier of the email
   * @return the email with full content
   * @throws EmailNotFoundException if the email does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Email getEmail(String emailId) {
    checkNotExpired();
    EmailData data = apiClient.getEmail(emailAddress, emailId);
    return decryptEmail(data);
  }

  /**
   * Retrieves the raw MIME content of an email.
   *
   * <p>Returns the complete, unprocessed email as it was received by the server. This includes all
   * headers, MIME boundaries, and encoded attachments.
   *
   * @param emailId the unique identifier of the email
   * @return the raw email content as a string (never null)
   * @throws EmailNotFoundException if the email does not exist
   * @throws IllegalStateException if raw content is not available for this email
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public String getRawEmail(String emailId) {
    checkNotExpired();
    RawEmailData data = apiClient.getRawEmail(emailAddress, emailId);

    // If raw content is directly available (legacy), return it
    if (data.getRawContent() != null) {
      return data.getRawContent();
    }

    if (data.isEncrypted()) {
      // Encrypted inbox: decrypt the encrypted raw content
      EncryptedPayload encryptedPayload = data.getEncryptedRaw();
      if (encryptedPayload != null) {
        if (!signatureVerifier.verify(encryptedPayload, serverSigPk)) {
          throw new SignatureVerificationException();
        }
        byte[] rawBytes = decryptor.decrypt(encryptedPayload, keypair.getSecretKey());
        String base64Content = new String(rawBytes, StandardCharsets.UTF_8);

        // Server base64 encodes raw email before encryption, decode it
        byte[] decoded = java.util.Base64.getDecoder().decode(base64Content);
        return new String(decoded, StandardCharsets.UTF_8);
      }
    } else {
      // Plain inbox: decode Base64
      String rawB64 = data.getRaw();
      if (rawB64 != null) {
        byte[] decoded = java.util.Base64.getDecoder().decode(rawB64);
        return new String(decoded, StandardCharsets.UTF_8);
      }
    }

    throw new IllegalStateException("Raw email content is not available for email " + emailId);
  }

  /**
   * Marks an email as read.
   *
   * @param emailId the unique identifier of the email
   * @throws EmailNotFoundException if the email does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void markEmailAsRead(String emailId) {
    checkNotExpired();
    apiClient.markAsRead(emailAddress, emailId);
  }

  /**
   * Deletes an email from this inbox.
   *
   * @param emailId the unique identifier of the email to delete
   * @throws EmailNotFoundException if the email does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void deleteEmail(String emailId) {
    checkNotExpired();
    apiClient.deleteEmail(emailAddress, emailId);
  }

  /**
   * Deletes this inbox from the server.
   *
   * <p>This permanently removes the inbox and all its emails from the server. If this inbox was
   * created via {@link VaultSandboxClient}, it will also be removed from the client's registry.
   *
   * @throws InboxNotFoundException if the inbox does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void delete() {
    checkNotExpired();
    apiClient.deleteInbox(emailAddress);
    if (onDelete != null) {
      onDelete.run();
    }
  }

  /**
   * Returns the synchronization status of this inbox.
   *
   * <p>The sync status includes the total number of emails and can be used to check if new emails
   * have arrived without fetching them.
   *
   * @return the current sync status
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public SyncStatus getSyncStatus() {
    checkNotExpired();
    return apiClient.getSyncStatus(emailAddress);
  }

  private Email decryptEmail(EmailData data) {
    DecryptedEmailMetadata metadata = null;
    DecryptedEmailContent content = null;

    if (data.isEncrypted()) {
      // Encrypted email: decrypt metadata and content
      EncryptedPayload metadataPayload = data.getEncryptedMetadata();
      if (metadataPayload != null) {
        if (!signatureVerifier.verify(metadataPayload, serverSigPk)) {
          throw new SignatureVerificationException();
        }
        byte[] metadataBytes = decryptor.decrypt(metadataPayload, keypair.getSecretKey());
        String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
        metadata = gson.fromJson(metadataJson, DecryptedEmailMetadata.class);
      }

      EncryptedPayload contentPayload = data.getEncryptedParsed();
      if (contentPayload != null) {
        if (!signatureVerifier.verify(contentPayload, serverSigPk)) {
          throw new SignatureVerificationException();
        }
        byte[] contentBytes = decryptor.decrypt(contentPayload, keypair.getSecretKey());
        String contentJson = new String(contentBytes, StandardCharsets.UTF_8);
        content = gson.fromJson(contentJson, DecryptedEmailContent.class);
      }
    } else {
      // Plain email: decode Base64
      String metadataB64 = data.getMetadata();
      if (metadataB64 != null) {
        byte[] metadataBytes = java.util.Base64.getDecoder().decode(metadataB64);
        String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
        metadata = gson.fromJson(metadataJson, DecryptedEmailMetadata.class);
      }

      String parsedB64 = data.getParsed();
      if (parsedB64 != null) {
        byte[] contentBytes = java.util.Base64.getDecoder().decode(parsedB64);
        String contentJson = new String(contentBytes, StandardCharsets.UTF_8);
        content = gson.fromJson(contentJson, DecryptedEmailContent.class);
      }
    }

    return new Email(data, metadata, content, this);
  }

  private EmailMetadata decryptToMetadata(EmailData data) {
    DecryptedEmailMetadata metadata = null;

    if (data.isEncrypted()) {
      // Encrypted email: decrypt metadata
      EncryptedPayload metadataPayload = data.getEncryptedMetadata();
      if (metadataPayload != null) {
        if (!signatureVerifier.verify(metadataPayload, serverSigPk)) {
          throw new SignatureVerificationException();
        }
        byte[] metadataBytes = decryptor.decrypt(metadataPayload, keypair.getSecretKey());
        String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
        metadata = gson.fromJson(metadataJson, DecryptedEmailMetadata.class);
      }
    } else {
      // Plain email: decode Base64
      String metadataB64 = data.getMetadata();
      if (metadataB64 != null) {
        byte[] metadataBytes = java.util.Base64.getDecoder().decode(metadataB64);
        String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
        metadata = gson.fromJson(metadataJson, DecryptedEmailMetadata.class);
      }
    }

    return new EmailMetadata(
        data.getId(),
        metadata != null ? metadata.getFrom() : null,
        metadata != null ? metadata.getSubject() : null,
        metadata != null ? metadata.getReceivedAt() : data.getReceivedAt(),
        data.isRead());
  }

  /**
   * Returns the email address of this inbox.
   *
   * @return the inbox's email address
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Returns when this inbox will expire.
   *
   * <p>After this time, the inbox and all its emails will be automatically deleted by the server.
   *
   * @return the expiration time
   */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /**
   * Returns the unique hash identifier of this inbox.
   *
   * <p>This is used internally for cryptographic operations and can be useful for debugging.
   *
   * @return the inbox hash
   */
  public String getHash() {
    return inboxHash;
  }

  /**
   * Returns the server's signature public key for this inbox.
   *
   * <p>This is used to verify that emails were encrypted by the server and haven't been tampered
   * with. Returns {@code null} for plain (unencrypted) inboxes.
   *
   * @return the server's signature public key (base64url encoded), or {@code null} if plain inbox
   */
  public String getServerSigPk() {
    return serverSigPk;
  }

  /**
   * Returns whether this inbox uses encryption.
   *
   * <p>Encrypted inboxes use ML-KEM and AES-256-GCM for end-to-end encryption. Plain inboxes store
   * emails as Base64-encoded JSON without encryption.
   *
   * @return {@code true} if encrypted, {@code false} if plain
   */
  public boolean isEncrypted() {
    return encrypted;
  }

  /**
   * Returns whether this inbox is persistent.
   *
   * <p>Persistent inboxes retain their data across server restarts. Ephemeral inboxes may lose data
   * on restart.
   *
   * @return {@code true} if persistent, {@code false} if ephemeral
   */
  public boolean isPersistent() {
    return persistent;
  }

  /**
   * Returns whether email authentication checks are enabled for this inbox. When false,
   * SPF/DKIM/DMARC/ReverseDNS checks return 'skipped'.
   *
   * @return true if email auth is enabled
   */
  public boolean isEmailAuth() {
    return emailAuth;
  }

  Keypair getKeypair() {
    return keypair;
  }

  /**
   * Checks if this inbox has expired and throws an exception if so.
   *
   * @throws InboxExpiredException if the inbox has expired
   */
  private void checkNotExpired() {
    if (Instant.now().isAfter(expiresAt)) {
      throw new InboxExpiredException(emailAddress, expiresAt);
    }
  }

  // ==================== Export ====================

  /**
   * Exports this inbox's credentials for later import.
   *
   * <p>This is equivalent to calling {@link VaultSandboxClient#exportInbox(Inbox)} with this inbox.
   *
   * <p>Per VaultSandbox spec §9.4, the public key is NOT included in the export as it can be
   * derived from the secret key.
   *
   * @return the exported inbox data
   * @see VaultSandboxClient#importInbox(ExportedInbox)
   */
  public ExportedInbox export() {
    ExportedInbox exported = new ExportedInbox();
    exported.setVersion(1);
    exported.setEmailAddress(emailAddress);
    exported.setExpiresAt(expiresAt.toString());
    exported.setInboxHash(inboxHash);
    exported.setEncrypted(encrypted);
    exported.setPersistent(persistent);
    exported.setEmailAuth(emailAuth);
    if (encrypted) {
      exported.setServerSigPk(serverSigPk);
      // Per spec §9.4: public key is NOT included (derived from secret key)
      exported.setSecretKey(Base64Url.encode(keypair.getSecretKey()));
    }
    exported.setExportedAt(Instant.now().toString());
    return exported;
  }

  // ==================== Webhook Methods ====================

  /**
   * Creates a webhook for this inbox.
   *
   * <p>The webhook will receive events only for this inbox.
   *
   * @param url the target URL (HTTPS required in production)
   * @param events the event types to subscribe to
   * @return the created webhook (includes the signing secret)
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public WebhookData createWebhook(String url, List<WebhookEventType> events) {
    checkNotExpired();
    CreateWebhookRequest request = new CreateWebhookRequest(url, events);
    return apiClient.createWebhook(emailAddress, request);
  }

  /**
   * Creates a webhook for this inbox using a request builder.
   *
   * <p>This method allows setting additional options like templates, filters, and descriptions.
   *
   * @param request the webhook creation request
   * @return the created webhook (includes the signing secret)
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public WebhookData createWebhook(CreateWebhookRequest request) {
    checkNotExpired();
    return apiClient.createWebhook(emailAddress, request);
  }

  /**
   * Lists all webhooks for this inbox.
   *
   * <p>Note: The {@code secret} field is not included in list responses.
   *
   * @return the list of webhooks
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public List<WebhookData> listWebhooks() {
    checkNotExpired();
    WebhookListResponse response = apiClient.listWebhooks(emailAddress);
    return response.getWebhooks();
  }

  /**
   * Retrieves a webhook by its ID.
   *
   * @param webhookId the webhook ID (whk_ prefix)
   * @return the webhook (includes secret and stats)
   * @throws WebhookNotFoundException if the webhook does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public WebhookData getWebhook(String webhookId) {
    checkNotExpired();
    return apiClient.getWebhook(emailAddress, webhookId);
  }

  /**
   * Updates a webhook.
   *
   * @param webhookId the webhook ID (whk_ prefix)
   * @param request the update request
   * @return the updated webhook
   * @throws WebhookNotFoundException if the webhook does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public WebhookData updateWebhook(String webhookId, UpdateWebhookRequest request) {
    checkNotExpired();
    return apiClient.updateWebhook(emailAddress, webhookId, request);
  }

  /**
   * Deletes a webhook.
   *
   * @param webhookId the webhook ID (whk_ prefix)
   * @throws WebhookNotFoundException if the webhook does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void deleteWebhook(String webhookId) {
    checkNotExpired();
    apiClient.deleteWebhook(emailAddress, webhookId);
  }

  /**
   * Sends a test event to a webhook endpoint to verify connectivity.
   *
   * @param webhookId the webhook ID (whk_ prefix)
   * @return the test result
   * @throws WebhookNotFoundException if the webhook does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public TestWebhookResponse testWebhook(String webhookId) {
    checkNotExpired();
    return apiClient.testWebhook(emailAddress, webhookId);
  }

  /**
   * Rotates a webhook's signing secret.
   *
   * <p>A new secret is generated immediately. The old secret remains valid for 1 hour to allow for
   * gradual migration.
   *
   * @param webhookId the webhook ID (whk_ prefix)
   * @return the new secret and grace period info
   * @throws WebhookNotFoundException if the webhook does not exist
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public RotateSecretResponse rotateWebhookSecret(String webhookId) {
    checkNotExpired();
    return apiClient.rotateWebhookSecret(emailAddress, webhookId);
  }

  // ==================== Chaos Methods ====================

  /**
   * Retrieves the current chaos configuration for this inbox.
   *
   * <p>Chaos engineering allows controlled injection of failures and delays into email processing.
   *
   * @return the chaos configuration
   * @throws ApiException if the API request fails or chaos is disabled globally
   * @throws NetworkException if unable to connect to the server
   */
  public ChaosConfig getChaos() {
    checkNotExpired();
    return apiClient.getChaosConfig(emailAddress);
  }

  /**
   * Sets the chaos configuration for this inbox.
   *
   * <p>Chaos engineering allows controlled injection of failures and delays into email processing.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * ChaosConfig config = ChaosConfig.builder()
   *     .enabled(true)
   *     .latency(LatencyConfig.builder()
   *         .minDelayMs(1000)
   *         .maxDelayMs(5000)
   *         .probability(0.5)
   *         .build())
   *     .build();
   *
   * inbox.setChaos(config);
   * }</pre>
   *
   * @param config the chaos configuration
   * @return the updated chaos configuration
   * @throws ApiException if the API request fails or chaos is disabled globally
   * @throws NetworkException if unable to connect to the server
   */
  public ChaosConfig setChaos(ChaosConfig config) {
    checkNotExpired();
    return apiClient.setChaosConfig(emailAddress, config);
  }

  /**
   * Disables all chaos for this inbox.
   *
   * @throws ApiException if the API request fails or chaos is disabled globally
   * @throws NetworkException if unable to connect to the server
   */
  public void disableChaos() {
    checkNotExpired();
    apiClient.deleteChaosConfig(emailAddress);
  }

  // ==================== Wait Methods ====================

  /**
   * Wait for any email to arrive.
   *
   * @return the received email
   * @throws TimeoutException if no email arrives within the default timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Email waitForEmail() {
    return waitForEmail(EmailFilter.any());
  }

  /**
   * Wait for an email matching the filter.
   *
   * @param filter the filter to match
   * @return the matching email
   * @throws TimeoutException if no matching email arrives within the default timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Email waitForEmail(EmailFilter filter) {
    return waitForEmail(filter, defaultTimeout);
  }

  /**
   * Wait for an email matching the filter within the specified timeout.
   *
   * @param filter the filter to match
   * @param timeout maximum time to wait
   * @return the matching email
   * @throws TimeoutException if no matching email arrives within timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Email waitForEmail(EmailFilter filter, Duration timeout) {
    return strategy.waitForEmail(this, filter, timeout);
  }

  /**
   * Wait for an email using the specified wait options.
   *
   * <p>This method allows specifying a custom poll interval for polling-based strategies.
   *
   * @param options the wait options including filter, timeout, and optional poll interval
   * @return the matching email
   * @throws TimeoutException if no matching email arrives within timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public Email waitForEmail(WaitOptions options) {
    Duration timeout = options.getTimeout() != null ? options.getTimeout() : defaultTimeout;
    return strategy.waitForEmail(this, options.getFilter(), timeout, options.getPollInterval());
  }

  /**
   * Wait for a specific number of emails to arrive.
   *
   * @param count the number of emails to wait for
   * @return list of received emails
   * @throws TimeoutException if not enough emails arrive within the default timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public List<Email> waitForEmailCount(int count) {
    return waitForEmailCount(count, defaultTimeout);
  }

  /**
   * Wait for a specific number of emails to arrive within the specified timeout.
   *
   * @param count the number of emails to wait for
   * @param timeout maximum time to wait
   * @return list of received emails
   * @throws TimeoutException if not enough emails arrive within timeout
   * @throws InboxExpiredException if the inbox has expired
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public List<Email> waitForEmailCount(int count, Duration timeout) {
    List<Email> collected = new ArrayList<>();
    Set<String> collectedIds = new HashSet<>();
    Instant deadline = Instant.now().plus(timeout);

    while (collected.size() < count && Instant.now().isBefore(deadline)) {
      Duration remaining = Duration.between(Instant.now(), deadline);
      if (remaining.isNegative() || remaining.isZero()) {
        break;
      }
      try {
        // Create a filter that excludes already-collected emails
        EmailFilter filter = EmailFilter.matching(email -> !collectedIds.contains(email.getId()));
        Email email = strategy.waitForEmail(this, filter, remaining);
        if (!collectedIds.contains(email.getId())) {
          collected.add(email);
          collectedIds.add(email.getId());
        }
      } catch (TimeoutException e) {
        break;
      }
    }

    if (collected.size() < count) {
      throw new TimeoutException(
          String.format("Only received %d of %d emails within timeout", collected.size(), count));
    }

    return collected;
  }

  /**
   * Subscribe to new emails for this inbox.
   *
   * @param callback called when new emails arrive
   * @return subscription handle to unsubscribe
   */
  public Subscription onNewEmail(Consumer<Email> callback) {
    return strategy.subscribe(this, callback);
  }

  // ==================== Await Methods (return null on timeout) ====================

  /**
   * Wait for any email to arrive, returning null on timeout.
   *
   * @return the received email, or null if timeout
   */
  public Email awaitEmail() {
    return awaitEmail(EmailFilter.any());
  }

  /**
   * Wait for an email matching the filter, returning null on timeout.
   *
   * @param filter the filter to match
   * @return the matching email, or null if timeout
   */
  public Email awaitEmail(EmailFilter filter) {
    return awaitEmail(filter, defaultTimeout);
  }

  /**
   * Wait for an email matching the filter within the specified timeout, returning null on timeout.
   *
   * @param filter the filter to match
   * @param timeout maximum time to wait
   * @return the matching email, or null if timeout
   */
  public Email awaitEmail(EmailFilter filter, Duration timeout) {
    try {
      return strategy.waitForEmail(this, filter, timeout);
    } catch (TimeoutException e) {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Inbox inbox = (Inbox) o;
    return Objects.equals(emailAddress, inbox.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress);
  }
}
