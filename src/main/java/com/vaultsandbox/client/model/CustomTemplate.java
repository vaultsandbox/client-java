package com.vaultsandbox.client.model;

/**
 * A custom payload template for webhook deliveries.
 *
 * <p>Custom templates use {@code {{variable}}} placeholders with dot notation.
 *
 * <p>Available variables:
 *
 * <ul>
 *   <li>{@code {{id}}} - Event ID
 *   <li>{@code {{type}}} - Event type
 *   <li>{@code {{createdAt}}} - Unix timestamp
 *   <li>{@code {{timestamp}}} - ISO 8601 timestamp
 *   <li>{@code {{data.from.address}}} - Sender email
 *   <li>{@code {{data.from.name}}} - Sender name
 *   <li>{@code {{data.subject}}} - Email subject
 *   <li>{@code {{data.snippet}}} - Email preview
 *   <li>{@code {{data.inboxEmail}}} - Inbox address
 * </ul>
 *
 * @see WebhookData
 */
public class CustomTemplate {
  private String type;
  private String body;
  private String contentType;

  public CustomTemplate() {
    this.type = "custom";
  }

  /**
   * Creates a custom template.
   *
   * @param body JSON template with {{variable}} placeholders (max 10000 chars)
   */
  public CustomTemplate(String body) {
    this.type = "custom";
    this.body = body;
  }

  /**
   * Creates a custom template with a specific content type.
   *
   * @param body JSON template with {{variable}} placeholders (max 10000 chars)
   * @param contentType the Content-Type header to use
   */
  public CustomTemplate(String body, String contentType) {
    this.type = "custom";
    this.body = body;
    this.contentType = contentType;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
