package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Types of SMTP errors to generate during chaos testing.
 *
 * @see RandomErrorConfig
 */
public enum ChaosErrorType {
  /** Temporary 4xx errors (421, 450, 451, 452). */
  @SerializedName("temporary")
  TEMPORARY,

  /** Permanent 5xx errors (550, 551, 552, 553, 554). */
  @SerializedName("permanent")
  PERMANENT
}
