package com.vaultsandbox.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaultsandbox.client.model.DecryptedEmailContent;
import com.vaultsandbox.client.model.EmailData;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTest {

  private static final Gson GSON = new GsonBuilder().create();

  @Test
  void testMetadataFromContent() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithMetadata();

    Email email = new Email(emailData, null, content, null);

    assertNotNull(email.getMetadata());
    assertEquals(2, email.getMetadata().size());
    assertEquals("value1", email.getMetadata().get("key1"));
    assertEquals(123.0, email.getMetadata().get("key2"));
  }

  @Test
  void testMetadataEmptyWhenContentNull() {
    EmailData emailData = createEmailData();

    Email email = new Email(emailData, null, null, null);

    assertNotNull(email.getMetadata());
    assertTrue(email.getMetadata().isEmpty());
  }

  @Test
  void testMetadataEmptyWhenNotPresent() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithoutMetadata();

    Email email = new Email(emailData, null, content, null);

    assertNotNull(email.getMetadata());
    assertTrue(email.getMetadata().isEmpty());
  }

  @Test
  void testMetadataImmutability() {
    EmailData emailData = createEmailData();
    DecryptedEmailContent content = createContentWithMetadata();

    Email email = new Email(emailData, null, content, null);

    assertThrows(
        UnsupportedOperationException.class, () -> email.getMetadata().put("new", "value"));
  }

  private EmailData createEmailData() {
    String json =
        GSON.toJson(
            Map.of(
                "id",
                "test-id",
                "from",
                "sender@example.com",
                "to",
                List.of("recipient@example.com"),
                "subject",
                "Test Subject",
                "receivedAt",
                Instant.now().toString(),
                "isRead",
                false));
    return GSON.fromJson(json, EmailData.class);
  }

  private DecryptedEmailContent createContentWithMetadata() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": [],
            "metadata": {
                "key1": "value1",
                "key2": 123
            }
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }

  private DecryptedEmailContent createContentWithoutMetadata() {
    String json =
        """
        {
            "text": "Test body",
            "html": "<p>Test body</p>",
            "headers": {},
            "attachments": [],
            "links": []
        }
        """;
    return GSON.fromJson(json, DecryptedEmailContent.class);
  }
}
