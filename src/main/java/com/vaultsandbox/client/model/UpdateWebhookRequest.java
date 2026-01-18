package com.vaultsandbox.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request body for updating a webhook.
 *
 * <p>All fields are optional. Set {@code template} or {@code filter} to explicit null to remove
 * them.
 *
 * @see com.vaultsandbox.client.Inbox#updateWebhook(String, UpdateWebhookRequest)
 */
public class UpdateWebhookRequest {
  private String url;
  private List<WebhookEventType> events;
  private Object template;
  private FilterConfig filter;
  private String description;
  private Boolean enabled;

  // Internal flags to track explicit null assignments
  private transient boolean templateSetToNull;
  private transient boolean filterSetToNull;

  public UpdateWebhookRequest() {}

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
    this.events = events != null ? new ArrayList<>(events) : null;
  }

  public Object getTemplate() {
    return template;
  }

  /**
   * Sets the payload template.
   *
   * @param template built-in template name or {@link CustomTemplate}, or null to remove
   */
  public void setTemplate(Object template) {
    this.template = template;
    this.templateSetToNull = (template == null);
  }

  /**
   * Sets a built-in template by name.
   *
   * @param templateName the template name
   */
  public void setTemplateName(String templateName) {
    this.template = templateName;
    this.templateSetToNull = false;
  }

  /**
   * Sets a custom template.
   *
   * @param customTemplate the custom template
   */
  public void setCustomTemplate(CustomTemplate customTemplate) {
    this.template = customTemplate;
    this.templateSetToNull = false;
  }

  /** Removes the template (sets to null). */
  public void removeTemplate() {
    this.template = null;
    this.templateSetToNull = true;
  }

  public FilterConfig getFilter() {
    return filter;
  }

  /**
   * Sets the filter configuration.
   *
   * @param filter the filter config, or null to remove
   */
  public void setFilter(FilterConfig filter) {
    this.filter = filter;
    this.filterSetToNull = (filter == null);
  }

  /** Removes the filter (sets to null). */
  public void removeFilter() {
    this.filter = null;
    this.filterSetToNull = true;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether the template was explicitly set to null.
   *
   * @return true if template should be removed
   */
  public boolean isTemplateSetToNull() {
    return templateSetToNull;
  }

  /**
   * Returns whether the filter was explicitly set to null.
   *
   * @return true if filter should be removed
   */
  public boolean isFilterSetToNull() {
    return filterSetToNull;
  }

  /** Builder for creating UpdateWebhookRequest instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for UpdateWebhookRequest. */
  public static class Builder {
    private String url;
    private List<WebhookEventType> events;
    private Object template;
    private boolean templateSetToNull;
    private FilterConfig filter;
    private boolean filterSetToNull;
    private String description;
    private Boolean enabled;

    /**
     * Sets the target URL.
     *
     * @param url the webhook URL
     * @return this builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the event types.
     *
     * @param events the event types
     * @return this builder
     */
    public Builder events(WebhookEventType... events) {
      this.events = new ArrayList<>(Arrays.asList(events));
      return this;
    }

    /**
     * Sets a built-in template.
     *
     * @param templateName the template name
     * @return this builder
     */
    public Builder template(String templateName) {
      this.template = templateName;
      this.templateSetToNull = false;
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
      this.templateSetToNull = false;
      return this;
    }

    /**
     * Removes the template.
     *
     * @return this builder
     */
    public Builder removeTemplate() {
      this.template = null;
      this.templateSetToNull = true;
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
      this.filterSetToNull = false;
      return this;
    }

    /**
     * Removes the filter.
     *
     * @return this builder
     */
    public Builder removeFilter() {
      this.filter = null;
      this.filterSetToNull = true;
      return this;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this builder
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Sets the enabled state.
     *
     * @param enabled true to enable, false to disable
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Builds the request.
     *
     * @return the update webhook request
     */
    public UpdateWebhookRequest build() {
      UpdateWebhookRequest request = new UpdateWebhookRequest();
      request.setUrl(url);
      request.setEvents(events);
      request.setDescription(description);
      request.setEnabled(enabled);
      if (templateSetToNull) {
        request.removeTemplate();
      } else if (template != null) {
        request.setTemplate(template);
      }
      if (filterSetToNull) {
        request.removeFilter();
      } else if (filter != null) {
        request.setFilter(filter);
      }
      return request;
    }
  }
}
