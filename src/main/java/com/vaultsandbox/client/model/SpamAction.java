package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Recommended action from Rspamd based on spam score thresholds.
 *
 * <p>Each action represents a different level of confidence that the email is spam:
 *
 * <ul>
 *   <li>{@link #NO_ACTION} - Email is clean, deliver normally
 *   <li>{@link #GREYLIST} - Temporarily reject, retry later (anti-spam technique)
 *   <li>{@link #ADD_HEADER} - Add spam headers but deliver
 *   <li>{@link #REWRITE_SUBJECT} - Modify subject to indicate spam
 *   <li>{@link #SOFT_REJECT} - Temporary rejection (4xx SMTP code)
 *   <li>{@link #REJECT} - Permanent rejection (5xx SMTP code)
 * </ul>
 *
 * @see SpamAnalysisResult
 */
public enum SpamAction {
  /** Email is clean, deliver normally. */
  @SerializedName("no action")
  NO_ACTION,

  /** Temporarily reject and retry later (anti-spam technique). */
  @SerializedName("greylist")
  GREYLIST,

  /** Add spam headers but deliver the email. */
  @SerializedName("add header")
  ADD_HEADER,

  /** Modify the subject to indicate the email is spam. */
  @SerializedName("rewrite subject")
  REWRITE_SUBJECT,

  /** Temporary rejection (would result in 4xx SMTP code). */
  @SerializedName("soft reject")
  SOFT_REJECT,

  /** Permanent rejection (would result in 5xx SMTP code). */
  @SerializedName("reject")
  REJECT
}
