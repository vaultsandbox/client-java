package com.vaultsandbox.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecryptedEmailContent {
  private String text;
  private String html;

  @JsonAdapter(HeadersAdapter.class)
  private Map<String, String> headers;

  private List<AttachmentData> attachments;
  private List<String> links;

  @SerializedName("auth_results")
  private AuthResults authResults;

  private Map<String, Object> metadata;

  public DecryptedEmailContent() {}

  public String getText() {
    return text;
  }

  public String getHtml() {
    return html;
  }

  public Map<String, String> getHeaders() {
    return headers != null ? headers : Collections.emptyMap();
  }

  public List<AttachmentData> getAttachments() {
    return attachments != null ? attachments : Collections.emptyList();
  }

  public List<String> getLinks() {
    return links != null ? links : Collections.emptyList();
  }

  public AuthResults getAuthResults() {
    return authResults;
  }

  /**
   * Returns additional metadata associated with the email.
   *
   * @return a map of metadata, empty if none
   */
  public Map<String, Object> getMetadata() {
    return metadata != null ? metadata : Collections.emptyMap();
  }

  /**
   * Custom adapter that handles headers where values can be strings, objects, or arrays. Converts
   * all values to their string representation.
   */
  public static class HeadersAdapter
      implements JsonDeserializer<Map<String, String>>, JsonSerializer<Map<String, String>> {
    @Override
    public Map<String, String> deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      Map<String, String> result = new HashMap<>();
      if (json.isJsonObject()) {
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
          result.put(entry.getKey(), extractStringValue(entry.getValue()));
        }
      }
      return result;
    }

    private String extractStringValue(JsonElement value) {
      if (value == null || value.isJsonNull()) {
        return "";
      } else if (value.isJsonPrimitive()) {
        return value.getAsString();
      } else if (value.isJsonArray()) {
        // For arrays, join elements with comma
        StringBuilder sb = new StringBuilder();
        for (JsonElement elem : value.getAsJsonArray()) {
          if (sb.length() > 0) {
            sb.append(", ");
          }
          sb.append(extractStringValue(elem));
        }
        return sb.toString();
      } else if (value.isJsonObject()) {
        // For object values, try to get "value" field or convert to string
        JsonObject obj = value.getAsJsonObject();
        if (obj.has("value")) {
          return extractStringValue(obj.get("value"));
        } else {
          return value.toString();
        }
      } else {
        return value.toString();
      }
    }

    @Override
    public JsonElement serialize(
        Map<String, String> src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject obj = new JsonObject();
      for (Map.Entry<String, String> entry : src.entrySet()) {
        obj.addProperty(entry.getKey(), entry.getValue());
      }
      return obj;
    }
  }
}
