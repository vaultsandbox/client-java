package com.vaultsandbox.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decrypted email metadata containing from, to, and subject. This is decrypted from the
 * encryptedMetadata payload.
 */
public class DecryptedEmailMetadata {
  private String from;

  @JsonAdapter(ToFieldAdapter.class)
  private List<String> to;

  private String subject;
  private String receivedAt;

  public DecryptedEmailMetadata() {}

  public String getFrom() {
    return from;
  }

  public List<String> getTo() {
    return to != null ? to : Collections.emptyList();
  }

  public String getSubject() {
    return subject;
  }

  public String getReceivedAt() {
    return receivedAt;
  }

  /** Custom adapter that handles "to" field being either a string or array. */
  public static class ToFieldAdapter
      implements JsonDeserializer<List<String>>, JsonSerializer<List<String>> {
    @Override
    public List<String> deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      List<String> result = new ArrayList<>();
      if (json.isJsonArray()) {
        for (JsonElement element : json.getAsJsonArray()) {
          result.add(element.getAsString());
        }
      } else if (json.isJsonPrimitive()) {
        result.add(json.getAsString());
      }
      return result;
    }

    @Override
    public JsonElement serialize(
        List<String> src, Type typeOfSrc, JsonSerializationContext context) {
      JsonArray array = new JsonArray();
      for (String s : src) {
        array.add(s);
      }
      return array;
    }
  }
}
