package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Status of the last webhook delivery attempt.
 *
 * @see WebhookData
 */
public enum DeliveryStatus {
  /** Delivery succeeded. */
  @SerializedName("success")
  SUCCESS,

  /** Delivery failed. */
  @SerializedName("failed")
  FAILED
}
