package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Status of spam analysis for an email.
 *
 * <ul>
 *   <li>{@link #ANALYZED} - Successfully analyzed by Rspamd
 *   <li>{@link #SKIPPED} - Analysis was skipped (disabled globally or per-inbox)
 *   <li>{@link #ERROR} - Analysis failed (Rspamd unavailable, timeout, etc.)
 * </ul>
 *
 * @see SpamAnalysisResult
 */
public enum SpamAnalysisStatus {
  /** Successfully analyzed by Rspamd. */
  @SerializedName("analyzed")
  ANALYZED,

  /** Analysis was skipped (disabled globally or per-inbox). */
  @SerializedName("skipped")
  SKIPPED,

  /** Analysis failed (Rspamd unavailable, timeout, etc.). */
  @SerializedName("error")
  ERROR
}
