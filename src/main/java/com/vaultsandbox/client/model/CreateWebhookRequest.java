package com.vaultsandbox.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request body for creating a webhook.
 *
 * @see com.vaultsandbox.client.Inbox#createWebhook(String, List)
 */
public class CreateWebhookRequest {
  private String url;
  private List<WebhookEventType> events;
  private Object template;
  private FilterConfig filter;
  private String description;

  public CreateWebhookRequest() {
    this.events = new ArrayList<>();
  }

  /**
   * Creates a webhook request with URL and events.
   *
   * @param url the target URL (HTTPS required in production)
   * @param events the event types to subscribe to (max 10)
   */
  public CreateWebhookRequest(String url, List<WebhookEventType> events) {
    this.url = url;
    this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<WebhookEventType> getEvents() {
    return events;
  }

  public void setEvents(List<WebhookEventType> events) {
    this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
  }

  public Object getTemplate() {
    return template;
  }

  /**
   * Sets the payload template.
   *
   * @param template built-in template name ('slack', 'discord', 'teams', 'simple', 'notification',
   *     'zapier', 'default') or a {@link CustomTemplate}
   */
  public void setTemplate(Object template) {
    this.template = template;
  }

  /**
   * Sets a built-in template by name.
   *
   * @param templateName the template name
   */
  public void setTemplateName(String templateName) {
    this.template = templateName;
  }

  /**
   * Sets a custom template.
   *
   * @param customTemplate the custom template
   */
  public void setCustomTemplate(CustomTemplate customTemplate) {
    this.template = customTemplate;
  }

  public FilterConfig getFilter() {
    return filter;
  }

  public void setFilter(FilterConfig filter) {
    this.filter = filter;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /** Builder for creating CreateWebhookRequest instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for CreateWebhookRequest. */
  public static class Builder {
    private String url;
    private final List<WebhookEventType> events = new ArrayList<>();
    private Object template;
    private FilterConfig filter;
    private String description;

    /**
     * Sets the target URL.
     *
     * @param url the webhook URL (HTTPS required in production)
     * @return this builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Adds event types to subscribe to.
     *
     * @param events the event types
     * @return this builder
     */
    public Builder events(WebhookEventType... events) {
      this.events.addAll(Arrays.asList(events));
      return this;
    }

    /**
     * Adds event types to subscribe to.
     *
     * @param events the event types
     * @return this builder
     */
    public Builder events(List<WebhookEventType> events) {
      this.events.addAll(events);
      return this;
    }

    /**
     * Sets a built-in template.
     *
     * @param templateName the template name ('slack', 'discord', 'teams', 'simple', 'notification',
     *     'zapier', 'default')
     * @return this builder
     */
    public Builder template(String templateName) {
      this.template = templateName;
      return this;
    }

    /**
     * Sets a custom template.
     *
     * @param customTemplate the custom template
     * @return this builder
     */
    public Builder template(CustomTemplate customTemplate) {
      this.template = customTemplate;
      return this;
    }

    /**
     * Sets the filter configuration.
     *
     * @param filter the filter config
     * @return this builder
     */
    public Builder filter(FilterConfig filter) {
      this.filter = filter;
      return this;
    }

    /**
     * Sets the description.
     *
     * @param description human-readable description (max 500 chars)
     * @return this builder
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Builds the request.
     *
     * @return the create webhook request
     */
    public CreateWebhookRequest build() {
      CreateWebhookRequest request = new CreateWebhookRequest(url, events);
      request.setTemplate(template);
      request.setFilter(filter);
      request.setDescription(description);
      return request;
    }
  }
}
