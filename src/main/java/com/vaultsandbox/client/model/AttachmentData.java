package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class AttachmentData {
  private String filename;

  @SerializedName(
      value = "content_type",
      alternate = {"contentType", "type", "mimeType", "mime_type"})
  private String contentType;

  private int size;

  @SerializedName(
      value = "content_b64",
      alternate = {"contentB64", "content", "data"})
  private String contentB64;

  public AttachmentData() {}

  public String getFilename() {
    return filename;
  }

  public String getContentType() {
    return contentType;
  }

  public int getSize() {
    return size;
  }

  public String getContentB64() {
    return contentB64;
  }
}
