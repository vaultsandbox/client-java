package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Types of events that can trigger a webhook.
 *
 * @see WebhookData
 */
public enum WebhookEventType {
  /** Email arrived at inbox. */
  @SerializedName("email.received")
  EMAIL_RECEIVED,

  /** Email persisted to storage. */
  @SerializedName("email.stored")
  EMAIL_STORED,

  /** Email deleted. */
  @SerializedName("email.deleted")
  EMAIL_DELETED
}
