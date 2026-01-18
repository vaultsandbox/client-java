package com.vaultsandbox.client.model;

import java.util.List;

/**
 * Response data for a webhook.
 *
 * @see com.vaultsandbox.client.Inbox#createWebhook(String, List)
 */
public class WebhookData {
  private String id;
  private String url;
  private List<WebhookEventType> events;
  private WebhookScope scope;
  private String inboxEmail;
  private String inboxHash;
  private Boolean enabled;
  private String secret;
  private Object template;
  private FilterConfig filter;
  private String description;
  private String createdAt;
  private String updatedAt;
  private String lastDeliveryAt;
  private DeliveryStatus lastDeliveryStatus;
  private WebhookStats stats;

  public WebhookData() {}

  /**
   * Returns the webhook ID (whk_ prefix).
   *
   * @return the webhook ID
   */
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the target URL.
   *
   * @return the webhook URL
   */
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Returns the subscribed event types.
   *
   * @return the list of event types
   */
  public List<WebhookEventType> getEvents() {
    return events;
  }

  public void setEvents(List<WebhookEventType> events) {
    this.events = events;
  }

  /**
   * Returns the webhook scope.
   *
   * @return 'global' or 'inbox'
   */
  public WebhookScope getScope() {
    return scope;
  }

  public void setScope(WebhookScope scope) {
    this.scope = scope;
  }

  /**
   * Returns the inbox email (inbox webhooks only).
   *
   * @return the inbox email, or null for global webhooks
   */
  public String getInboxEmail() {
    return inboxEmail;
  }

  public void setInboxEmail(String inboxEmail) {
    this.inboxEmail = inboxEmail;
  }

  /**
   * Returns the inbox hash (inbox webhooks only).
   *
   * @return the inbox hash, or null for global webhooks
   */
  public String getInboxHash() {
    return inboxHash;
  }

  public void setInboxHash(String inboxHash) {
    this.inboxHash = inboxHash;
  }

  /**
   * Returns whether the webhook is enabled.
   *
   * @return true if enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether the webhook is active.
   *
   * @return true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  /**
   * Returns the signing secret (whsec_ prefix).
   *
   * <p>Only included on create and get operations, not on list.
   *
   * @return the signing secret, or null if not included
   */
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Returns the template configuration.
   *
   * <p>Can be a string (built-in template name) or a {@link CustomTemplate}.
   *
   * @return the template, or null if using default
   */
  public Object getTemplate() {
    return template;
  }

  public void setTemplate(Object template) {
    this.template = template;
  }

  /**
   * Returns the template name if using a built-in template.
   *
   * @return the template name, or null if custom or not set
   */
  public String getTemplateName() {
    if (template instanceof String) {
      return (String) template;
    }
    return null;
  }

  /**
   * Returns the filter configuration.
   *
   * @return the filter config, or null if not filtering
   */
  public FilterConfig getFilter() {
    return filter;
  }

  public void setFilter(FilterConfig filter) {
    this.filter = filter;
  }

  /**
   * Returns the human-readable description.
   *
   * @return the description, or null if not set
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the creation timestamp (ISO 8601).
   *
   * @return the creation time
   */
  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Returns the last update timestamp (ISO 8601).
   *
   * @return the last update time, or null if never updated
   */
  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Returns the last delivery attempt timestamp (ISO 8601).
   *
   * @return the last delivery time, or null if never delivered
   */
  public String getLastDeliveryAt() {
    return lastDeliveryAt;
  }

  public void setLastDeliveryAt(String lastDeliveryAt) {
    this.lastDeliveryAt = lastDeliveryAt;
  }

  /**
   * Returns the status of the last delivery.
   *
   * @return 'success' or 'failed', or null if never delivered
   */
  public DeliveryStatus getLastDeliveryStatus() {
    return lastDeliveryStatus;
  }

  public void setLastDeliveryStatus(DeliveryStatus lastDeliveryStatus) {
    this.lastDeliveryStatus = lastDeliveryStatus;
  }

  /**
   * Returns the delivery statistics.
   *
   * <p>Only included on get operations, not on list.
   *
   * @return the stats, or null if not included
   */
  public WebhookStats getStats() {
    return stats;
  }

  public void setStats(WebhookStats stats) {
    this.stats = stats;
  }
}
