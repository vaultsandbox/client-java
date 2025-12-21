package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

class DecryptedEmailContentTest {

  private static final Gson GSON = new GsonBuilder().create();

  @Test
  void testMetadataDeserialization() {
    String json =
        """
        {
            "text": "Hello",
            "html": "<p>Hello</p>",
            "metadata": {
                "customField": "customValue",
                "numericField": 42,
                "nestedObject": {"key": "value"}
            }
        }
        """;

    DecryptedEmailContent content = GSON.fromJson(json, DecryptedEmailContent.class);

    assertNotNull(content.getMetadata());
    assertEquals(3, content.getMetadata().size());
    assertEquals("customValue", content.getMetadata().get("customField"));
    assertEquals(42.0, content.getMetadata().get("numericField"));
    assertNotNull(content.getMetadata().get("nestedObject"));
  }

  @Test
  void testMetadataEmptyWhenNotPresent() {
    String json =
        """
        {
            "text": "Hello",
            "html": "<p>Hello</p>"
        }
        """;

    DecryptedEmailContent content = GSON.fromJson(json, DecryptedEmailContent.class);

    assertNotNull(content.getMetadata());
    assertTrue(content.getMetadata().isEmpty());
  }

  @Test
  void testMetadataEmptyWhenNull() {
    String json =
        """
        {
            "text": "Hello",
            "html": "<p>Hello</p>",
            "metadata": null
        }
        """;

    DecryptedEmailContent content = GSON.fromJson(json, DecryptedEmailContent.class);

    assertNotNull(content.getMetadata());
    assertTrue(content.getMetadata().isEmpty());
  }

  @Test
  void testMetadataWithEmptyObject() {
    String json =
        """
        {
            "text": "Hello",
            "html": "<p>Hello</p>",
            "metadata": {}
        }
        """;

    DecryptedEmailContent content = GSON.fromJson(json, DecryptedEmailContent.class);

    assertNotNull(content.getMetadata());
    assertTrue(content.getMetadata().isEmpty());
  }
}
