package com.vaultsandbox.client.exception;

/**
 * Exception thrown when a webhook is not found.
 *
 * @see com.vaultsandbox.client.Inbox#getWebhook(String)
 */
public class WebhookNotFoundException extends ApiException {
  public WebhookNotFoundException(String webhookId) {
    super("Webhook not found: " + webhookId, 404);
  }
}
