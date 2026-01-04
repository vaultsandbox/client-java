package com.vaultsandbox.client;

import com.vaultsandbox.client.model.AuthResults;
import com.vaultsandbox.client.model.DecryptedEmailContent;
import com.vaultsandbox.client.model.DecryptedEmailMetadata;
import com.vaultsandbox.client.model.EmailData;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an email received by an inbox.
 *
 * <p>An email contains standard email properties (from, to, subject, body) as well as additional
 * metadata like authentication results (SPF, DKIM, DMARC) and extracted content (links,
 * attachments).
 *
 * <p>All email retrieval methods ({@link Inbox#listEmails()}, {@link Inbox#getEmail(String)}, and
 * wait methods) return fully hydrated emails with complete content including body text, HTML,
 * headers, attachments, links, and authentication results.
 *
 * <p>The email object also provides convenience methods for common operations:
 *
 * <pre>{@code
 * email.markAsRead();  // Mark this email as read
 * email.delete();      // Delete this email
 * String raw = email.getRaw();  // Get raw MIME content
 * }</pre>
 *
 * @see Inbox
 * @see Attachment
 * @see AuthResults
 */
public class Email {
  private final String id;
  private final String from;
  private final List<String> to;
  private final String subject;
  private final Instant receivedAt;
  private volatile boolean isRead;

  private final String text;
  private final String html;
  private final Map<String, String> headers;
  private final List<Attachment> attachments;
  private final List<String> links;
  private final AuthResults authResults;
  private final Map<String, Object> metadata;

  private final Inbox inbox;

  Email(
      EmailData data, DecryptedEmailMetadata metadata, DecryptedEmailContent content, Inbox inbox) {
    this.id = data.getId();
    this.isRead = data.isRead();

    // Use decrypted metadata if available, fall back to legacy fields
    if (metadata != null) {
      this.from = metadata.getFrom();
      this.to = List.copyOf(metadata.getTo());
      this.subject = metadata.getSubject();
      // receivedAt may be in metadata or EmailData
      String receivedAtStr =
          data.getReceivedAt() != null ? data.getReceivedAt() : metadata.getReceivedAt();
      this.receivedAt = receivedAtStr != null ? Instant.parse(receivedAtStr) : Instant.now();
    } else {
      this.from = data.getFrom();
      this.to = data.getTo() != null ? List.copyOf(data.getTo()) : List.of();
      this.subject = data.getSubject();
      this.receivedAt =
          data.getReceivedAt() != null ? Instant.parse(data.getReceivedAt()) : Instant.now();
    }

    // Content may be null when listing emails (only metadata is returned)
    if (content != null) {
      this.text = content.getText();
      this.html = content.getHtml();
      this.headers = Map.copyOf(content.getHeaders());
      this.attachments =
          content.getAttachments().stream()
              .map(Attachment::new)
              .collect(Collectors.toUnmodifiableList());
      this.links = List.copyOf(content.getLinks());
      this.authResults = content.getAuthResults();
      this.metadata =
          content.getMetadata() != null && !content.getMetadata().isEmpty()
              ? Map.copyOf(content.getMetadata())
              : Map.of();
    } else {
      this.text = null;
      this.html = null;
      this.headers = Map.of();
      this.attachments = List.of();
      this.links = List.of();
      this.authResults = null;
      this.metadata = Map.of();
    }

    this.inbox = inbox;
  }

  /**
   * Marks this email as read.
   *
   * <p>This method updates both the server state and the local {@link #isRead()} flag. After this
   * method returns successfully, {@link #isRead()} will return {@code true}.
   *
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void markAsRead() {
    inbox.markEmailAsRead(id);
    this.isRead = true;
  }

  /**
   * Deletes this email from the inbox.
   *
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public void delete() {
    inbox.deleteEmail(id);
  }

  /**
   * Retrieves the raw MIME content of this email.
   *
   * @return the raw email content as a string
   * @throws ApiException if the API request fails
   * @throws NetworkException if unable to connect to the server
   */
  public String getRaw() {
    return inbox.getRawEmail(id);
  }

  /**
   * Returns the unique identifier of this email.
   *
   * @return the email ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the sender's email address.
   *
   * @return the "From" address
   */
  public String getFrom() {
    return from;
  }

  /**
   * Returns the list of recipient email addresses.
   *
   * @return the "To" addresses
   */
  public List<String> getTo() {
    return to;
  }

  /**
   * Returns the email subject line.
   *
   * @return the subject, or {@code null} if not present
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Returns when this email was received by the server.
   *
   * @return the receive timestamp
   */
  public Instant getReceivedAt() {
    return receivedAt;
  }

  /**
   * Returns whether this email has been marked as read.
   *
   * @return {@code true} if the email has been read
   */
  public boolean isRead() {
    return isRead;
  }

  /**
   * Returns the plain text body of the email.
   *
   * <p>May be {@code null} if the email only contains HTML.
   *
   * @return the plain text content, or {@code null} if not available
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the HTML body of the email.
   *
   * <p>May be {@code null} if the email is plain text only.
   *
   * @return the HTML content, or {@code null} if not available
   */
  public String getHtml() {
    return html;
  }

  /**
   * Returns the email headers.
   *
   * <p>Includes all headers from the original email such as Content-Type, Message-ID, etc.
   *
   * @return an immutable map of header names to values
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Returns the list of attachments.
   *
   * @return an immutable list of attachments, empty if none
   */
  public List<Attachment> getAttachments() {
    return attachments;
  }

  /**
   * Returns URLs extracted from the email body.
   *
   * <p>Links are extracted from both HTML (href attributes) and plain text content.
   *
   * @return an immutable list of URLs found in the email
   */
  public List<String> getLinks() {
    return links;
  }

  /**
   * Returns email authentication results.
   *
   * <p>Includes SPF, DKIM, DMARC, and reverse DNS validation results. May be {@code null} if
   * authentication results are not available.
   *
   * @return the authentication results, or {@code null} if not available
   * @see AuthResults
   */
  public AuthResults getAuthResults() {
    return authResults;
  }

  /**
   * Returns additional metadata associated with the email.
   *
   * <p>This map contains any extra fields returned by the server that are not part of the standard
   * email properties.
   *
   * @return an immutable map of metadata, empty if none
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }
}
