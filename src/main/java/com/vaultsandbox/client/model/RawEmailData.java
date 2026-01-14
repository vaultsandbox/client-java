package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class RawEmailData {
  private String id;

  @SerializedName("raw_content")
  private String rawContent;

  private EncryptedPayload encryptedRaw;

  // Plain (unencrypted) inbox field - Base64-encoded raw email
  private String raw;

  public RawEmailData() {}

  public String getId() {
    return id;
  }

  public String getRawContent() {
    return rawContent;
  }

  public EncryptedPayload getEncryptedRaw() {
    return encryptedRaw;
  }

  /**
   * Returns the plain (Base64-encoded) raw email for plain inboxes.
   *
   * @return the Base64-encoded raw email, or {@code null} if this is an encrypted email
   */
  public String getRaw() {
    return raw;
  }

  /**
   * Returns whether this raw email is from an encrypted inbox.
   *
   * @return {@code true} if encrypted, {@code false} if plain
   */
  public boolean isEncrypted() {
    return encryptedRaw != null;
  }
}
