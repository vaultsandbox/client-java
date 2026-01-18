package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Operators available for webhook filter rules.
 *
 * @see FilterRule
 */
public enum FilterOperator {
  /** Exact match. */
  @SerializedName("equals")
  EQUALS,

  /** Substring match. */
  @SerializedName("contains")
  CONTAINS,

  /** Prefix match. */
  @SerializedName("starts_with")
  STARTS_WITH,

  /** Suffix match. */
  @SerializedName("ends_with")
  ENDS_WITH,

  /** Email domain match (supports subdomains). */
  @SerializedName("domain")
  DOMAIN,

  /** Regular expression match. */
  @SerializedName("regex")
  REGEX,

  /** Field presence check. */
  @SerializedName("exists")
  EXISTS
}
