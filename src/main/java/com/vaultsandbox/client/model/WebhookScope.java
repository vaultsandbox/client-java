package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Scope of a webhook.
 *
 * @see WebhookData
 */
public enum WebhookScope {
  /** Receives events for all inboxes. */
  @SerializedName("global")
  GLOBAL,

  /** Receives events for a specific inbox only. */
  @SerializedName("inbox")
  INBOX
}
