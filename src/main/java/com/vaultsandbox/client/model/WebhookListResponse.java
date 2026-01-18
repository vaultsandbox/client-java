package com.vaultsandbox.client.model;

import java.util.List;

/**
 * Response for listing webhooks.
 *
 * @see com.vaultsandbox.client.Inbox#listWebhooks()
 */
public class WebhookListResponse {
  private List<WebhookData> webhooks;
  private int total;

  public WebhookListResponse() {}

  /**
   * Returns the list of webhooks.
   *
   * <p>Note: The {@code secret} field is excluded from list responses.
   *
   * @return the webhooks
   */
  public List<WebhookData> getWebhooks() {
    return webhooks;
  }

  public void setWebhooks(List<WebhookData> webhooks) {
    this.webhooks = webhooks;
  }

  /**
   * Returns the total count of webhooks.
   *
   * @return the total
   */
  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }
}
