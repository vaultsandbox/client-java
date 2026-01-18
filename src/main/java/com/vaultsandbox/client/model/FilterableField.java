package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Fields that can be filtered on in webhook filter rules.
 *
 * @see FilterRule
 */
public enum FilterableField {
  /** Email subject line. */
  @SerializedName("subject")
  SUBJECT,

  /** Sender email address. */
  @SerializedName("from.address")
  FROM_ADDRESS,

  /** Sender display name. */
  @SerializedName("from.name")
  FROM_NAME,

  /** First recipient email address. */
  @SerializedName("to.address")
  TO_ADDRESS,

  /** First recipient display name. */
  @SerializedName("to.name")
  TO_NAME,

  /** Plain text body (first 5KB). */
  @SerializedName("body.text")
  BODY_TEXT,

  /** HTML body (first 5KB). */
  @SerializedName("body.html")
  BODY_HTML
}
