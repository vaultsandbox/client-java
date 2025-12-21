package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class RawEmailData {
  private String id;

  @SerializedName("raw_content")
  private String rawContent;

  private EncryptedPayload encryptedRaw;

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
}
