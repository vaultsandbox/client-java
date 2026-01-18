package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Logic mode for combining filter rules.
 *
 * @see FilterConfig
 */
public enum FilterMode {
  /** All rules must match (AND logic). */
  @SerializedName("all")
  ALL,

  /** At least one rule must match (OR logic). */
  @SerializedName("any")
  ANY
}
