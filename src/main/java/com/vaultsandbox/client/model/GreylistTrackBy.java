package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Method for tracking unique senders during greylisting.
 *
 * @see GreylistConfig
 */
public enum GreylistTrackBy {
  /** Track by sender IP address only. */
  @SerializedName("ip")
  IP,

  /** Track by sender email address only. */
  @SerializedName("sender")
  SENDER,

  /** Track by combination of IP and sender email. */
  @SerializedName("ip_sender")
  IP_SENDER
}
