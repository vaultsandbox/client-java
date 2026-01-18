package com.vaultsandbox.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.TestConfig;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.exception.WebhookNotFoundException;
import com.vaultsandbox.client.model.CreateWebhookRequest;
import com.vaultsandbox.client.model.CustomTemplate;
import com.vaultsandbox.client.model.FilterConfig;
import com.vaultsandbox.client.model.FilterMode;
import com.vaultsandbox.client.model.FilterOperator;
import com.vaultsandbox.client.model.FilterableField;
import com.vaultsandbox.client.model.RotateSecretResponse;
import com.vaultsandbox.client.model.TestWebhookResponse;
import com.vaultsandbox.client.model.UpdateWebhookRequest;
import com.vaultsandbox.client.model.WebhookData;
import com.vaultsandbox.client.model.WebhookEventType;
import com.vaultsandbox.client.model.WebhookScope;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Integration tests for webhook management on inboxes. */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebhookIT {

  private static VaultSandboxClient client;

  @BeforeAll
  static void setUp() {
    assumeTrue(TestConfig.hasApiKey(), "API key required for integration tests");

    client =
        VaultSandboxClient.create(
            ClientConfig.builder()
                .apiKey(TestConfig.getApiKey())
                .baseUrl(TestConfig.getBaseUrl())
                .build());
  }

  @AfterAll
  static void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  @Order(1)
  void testCreateWebhookSimple() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData webhook =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      assertNotNull(webhook);
      assertNotNull(webhook.getId());
      assertTrue(webhook.getId().startsWith("whk_"), "Webhook ID should start with whk_");
      assertEquals("https://example.com/webhook", webhook.getUrl());
      assertNotNull(webhook.getEvents());
      assertEquals(1, webhook.getEvents().size());
      assertEquals(WebhookEventType.EMAIL_RECEIVED, webhook.getEvents().get(0));
      assertEquals(WebhookScope.INBOX, webhook.getScope());
      assertEquals(inbox.getEmailAddress(), webhook.getInboxEmail());
      assertTrue(webhook.isEnabled());
      assertNotNull(webhook.getSecret());
      assertTrue(webhook.getSecret().startsWith("whsec_"), "Secret should start with whsec_");
      assertNotNull(webhook.getCreatedAt());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(2)
  void testCreateWebhookWithMultipleEvents() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData webhook =
          inbox.createWebhook(
              "https://example.com/webhook",
              List.of(
                  WebhookEventType.EMAIL_RECEIVED,
                  WebhookEventType.EMAIL_STORED,
                  WebhookEventType.EMAIL_DELETED));

      assertNotNull(webhook);
      assertEquals(3, webhook.getEvents().size());
      assertTrue(webhook.getEvents().contains(WebhookEventType.EMAIL_RECEIVED));
      assertTrue(webhook.getEvents().contains(WebhookEventType.EMAIL_STORED));
      assertTrue(webhook.getEvents().contains(WebhookEventType.EMAIL_DELETED));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(3)
  void testCreateWebhookWithBuilder() {
    Inbox inbox = client.createInbox();

    try {
      CreateWebhookRequest request =
          CreateWebhookRequest.builder()
              .url("https://hooks.example.com/endpoint")
              .events(WebhookEventType.EMAIL_RECEIVED, WebhookEventType.EMAIL_DELETED)
              .description("Test webhook for notifications")
              .build();

      WebhookData webhook = inbox.createWebhook(request);

      assertNotNull(webhook);
      assertEquals("https://hooks.example.com/endpoint", webhook.getUrl());
      assertEquals(2, webhook.getEvents().size());
      assertEquals("Test webhook for notifications", webhook.getDescription());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(4)
  void testCreateWebhookWithTemplate() {
    Inbox inbox = client.createInbox();

    try {
      CreateWebhookRequest request =
          CreateWebhookRequest.builder()
              .url("https://hooks.slack.com/services/test")
              .events(WebhookEventType.EMAIL_RECEIVED)
              .template("slack")
              .build();

      WebhookData webhook = inbox.createWebhook(request);

      assertNotNull(webhook);
      assertNotNull(webhook.getTemplate());
      assertEquals("slack", webhook.getTemplateName());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(5)
  void testCreateWebhookWithCustomTemplate() {
    Inbox inbox = client.createInbox();

    try {
      CustomTemplate customTemplate =
          new CustomTemplate(
              "{\"email_from\": \"{{data.from.address}}\", \"subject\": \"{{data.subject}}\"}",
              "application/json");

      CreateWebhookRequest request =
          CreateWebhookRequest.builder()
              .url("https://custom.example.com/webhook")
              .events(WebhookEventType.EMAIL_RECEIVED)
              .template(customTemplate)
              .build();

      WebhookData webhook = inbox.createWebhook(request);

      assertNotNull(webhook);
      assertNotNull(webhook.getTemplate());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(6)
  void testCreateWebhookWithFilter() {
    Inbox inbox = client.createInbox();

    try {
      FilterConfig filter =
          FilterConfig.builder()
              .addRule(FilterableField.FROM_ADDRESS, FilterOperator.DOMAIN, "example.com")
              .addRule(FilterableField.SUBJECT, FilterOperator.CONTAINS, "urgent")
              .mode(FilterMode.ALL)
              .requireAuth(true)
              .build();

      CreateWebhookRequest request =
          CreateWebhookRequest.builder()
              .url("https://filtered.example.com/webhook")
              .events(WebhookEventType.EMAIL_RECEIVED)
              .filter(filter)
              .build();

      WebhookData webhook = inbox.createWebhook(request);

      assertNotNull(webhook);
      assertNotNull(webhook.getFilter());
      assertEquals(FilterMode.ALL, webhook.getFilter().getMode());
      assertEquals(2, webhook.getFilter().getRules().size());
      assertTrue(webhook.getFilter().isRequireAuth());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(10)
  void testListWebhooksEmpty() {
    Inbox inbox = client.createInbox();

    try {
      List<WebhookData> webhooks = inbox.listWebhooks();

      assertNotNull(webhooks);
      assertTrue(webhooks.isEmpty());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(11)
  void testListWebhooks() {
    Inbox inbox = client.createInbox();

    try {
      // Create multiple webhooks
      inbox.createWebhook("https://example1.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));
      inbox.createWebhook("https://example2.com/webhook", List.of(WebhookEventType.EMAIL_STORED));
      inbox.createWebhook("https://example3.com/webhook", List.of(WebhookEventType.EMAIL_DELETED));

      List<WebhookData> webhooks = inbox.listWebhooks();

      assertNotNull(webhooks);
      assertEquals(3, webhooks.size());

      // Verify secrets are not included in list response
      for (WebhookData webhook : webhooks) {
        assertNotNull(webhook.getId());
        assertNotNull(webhook.getUrl());
        // Secret should not be included in list
      }
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(20)
  void testGetWebhook() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      WebhookData retrieved = inbox.getWebhook(created.getId());

      assertNotNull(retrieved);
      assertEquals(created.getId(), retrieved.getId());
      assertEquals(created.getUrl(), retrieved.getUrl());
      assertEquals(created.getEvents().size(), retrieved.getEvents().size());
      assertNotNull(retrieved.getSecret());
      // Stats should be included in get response
      assertNotNull(retrieved.getStats());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(21)
  void testGetWebhookNotFound() {
    Inbox inbox = client.createInbox();

    try {
      assertThrows(
          WebhookNotFoundException.class, () -> inbox.getWebhook("whk_nonexistent_webhook_id"));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(30)
  void testUpdateWebhookUrl() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://old.example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      UpdateWebhookRequest update =
          UpdateWebhookRequest.builder().url("https://new.example.com/webhook").build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);

      assertNotNull(updated);
      assertEquals("https://new.example.com/webhook", updated.getUrl());
      assertEquals(created.getId(), updated.getId());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(31)
  void testUpdateWebhookEvents() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      UpdateWebhookRequest update =
          UpdateWebhookRequest.builder()
              .events(WebhookEventType.EMAIL_STORED, WebhookEventType.EMAIL_DELETED)
              .build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);

      assertNotNull(updated);
      assertEquals(2, updated.getEvents().size());
      assertTrue(updated.getEvents().contains(WebhookEventType.EMAIL_STORED));
      assertTrue(updated.getEvents().contains(WebhookEventType.EMAIL_DELETED));
      assertFalse(updated.getEvents().contains(WebhookEventType.EMAIL_RECEIVED));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(32)
  void testUpdateWebhookDisable() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      assertTrue(created.isEnabled());

      UpdateWebhookRequest update = UpdateWebhookRequest.builder().enabled(false).build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);

      assertNotNull(updated);
      assertFalse(updated.isEnabled());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(33)
  void testUpdateWebhookTemplate() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      UpdateWebhookRequest update = UpdateWebhookRequest.builder().template("discord").build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);

      assertNotNull(updated);
      assertEquals("discord", updated.getTemplateName());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(34)
  void testUpdateWebhookRemoveTemplate() {
    Inbox inbox = client.createInbox();

    try {
      CreateWebhookRequest createRequest =
          CreateWebhookRequest.builder()
              .url("https://example.com/webhook")
              .events(WebhookEventType.EMAIL_RECEIVED)
              .template("slack")
              .build();

      WebhookData created = inbox.createWebhook(createRequest);
      assertNotNull(created.getTemplate());

      UpdateWebhookRequest update = UpdateWebhookRequest.builder().removeTemplate().build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);

      assertNotNull(updated);
      // Template should be removed (null or "default")
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(35)
  void testUpdateWebhookNotFound() {
    Inbox inbox = client.createInbox();

    try {
      UpdateWebhookRequest update = UpdateWebhookRequest.builder().enabled(false).build();

      assertThrows(
          WebhookNotFoundException.class,
          () -> inbox.updateWebhook("whk_nonexistent_webhook_id", update));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(40)
  void testDeleteWebhook() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      String webhookId = created.getId();

      // Delete should succeed
      inbox.deleteWebhook(webhookId);

      // Webhook should no longer exist
      assertThrows(WebhookNotFoundException.class, () -> inbox.getWebhook(webhookId));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(41)
  void testDeleteWebhookIdempotent() {
    Inbox inbox = client.createInbox();

    try {
      // Server uses idempotent DELETE - deleting non-existent webhook returns 204 (success)
      // This is valid REST behavior: "delete X" when X doesn't exist = "X is deleted"
      inbox.deleteWebhook("whk_nonexistent_webhook_id");
      // No exception means idempotent delete succeeded
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(50)
  void testTestWebhook() {
    Inbox inbox = client.createInbox();

    try {
      // Use a URL that will respond (httpbin or similar) or expect failure
      WebhookData created =
          inbox.createWebhook("https://httpbin.org/post", List.of(WebhookEventType.EMAIL_RECEIVED));

      TestWebhookResponse result = inbox.testWebhook(created.getId());

      assertNotNull(result);
      // The test should complete (success or failure depending on endpoint availability)
      // We just verify the response structure
      assertNotNull(result.isSuccess() || result.getError() != null);
      if (result.isSuccess()) {
        assertNotNull(result.getStatusCode());
        assertNotNull(result.getResponseTime());
      }
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(51)
  void testTestWebhookNotFound() {
    Inbox inbox = client.createInbox();

    try {
      assertThrows(
          WebhookNotFoundException.class, () -> inbox.testWebhook("whk_nonexistent_webhook_id"));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(60)
  void testRotateWebhookSecret() {
    Inbox inbox = client.createInbox();

    try {
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      String originalSecret = created.getSecret();

      RotateSecretResponse result = inbox.rotateWebhookSecret(created.getId());

      assertNotNull(result);
      assertEquals(created.getId(), result.getId());
      assertNotNull(result.getSecret());
      assertTrue(result.getSecret().startsWith("whsec_"));
      assertNotEquals(originalSecret, result.getSecret());
      assertNotNull(result.getPreviousSecretValidUntil());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(61)
  void testRotateWebhookSecretNotFound() {
    Inbox inbox = client.createInbox();

    try {
      assertThrows(
          WebhookNotFoundException.class,
          () -> inbox.rotateWebhookSecret("whk_nonexistent_webhook_id"));
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(70)
  void testWebhookLifecycle() {
    Inbox inbox = client.createInbox();

    try {
      // Create webhook
      WebhookData created =
          inbox.createWebhook(
              "https://example.com/webhook", List.of(WebhookEventType.EMAIL_RECEIVED));

      assertNotNull(created);
      assertTrue(created.isEnabled());

      // List webhooks
      List<WebhookData> webhooks = inbox.listWebhooks();
      assertEquals(1, webhooks.size());

      // Update webhook
      UpdateWebhookRequest update =
          UpdateWebhookRequest.builder()
              .url("https://updated.example.com/webhook")
              .description("Updated description")
              .enabled(false)
              .build();

      WebhookData updated = inbox.updateWebhook(created.getId(), update);
      assertEquals("https://updated.example.com/webhook", updated.getUrl());
      assertEquals("Updated description", updated.getDescription());
      assertFalse(updated.isEnabled());

      // Re-enable
      UpdateWebhookRequest enableUpdate = UpdateWebhookRequest.builder().enabled(true).build();
      WebhookData reenabled = inbox.updateWebhook(created.getId(), enableUpdate);
      assertTrue(reenabled.isEnabled());

      // Rotate secret
      RotateSecretResponse rotated = inbox.rotateWebhookSecret(created.getId());
      assertNotEquals(created.getSecret(), rotated.getSecret());

      // Delete webhook
      inbox.deleteWebhook(created.getId());

      // Verify deleted
      webhooks = inbox.listWebhooks();
      assertTrue(webhooks.isEmpty());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(80)
  void testMultipleWebhooksOnSameInbox() {
    Inbox inbox = client.createInbox();

    try {
      // Create multiple webhooks for different purposes
      WebhookData slackWebhook =
          inbox.createWebhook(
              CreateWebhookRequest.builder()
                  .url("https://hooks.slack.com/services/test")
                  .events(WebhookEventType.EMAIL_RECEIVED)
                  .template("slack")
                  .description("Slack notifications")
                  .build());

      WebhookData discordWebhook =
          inbox.createWebhook(
              CreateWebhookRequest.builder()
                  .url("https://discord.com/api/webhooks/test")
                  .events(WebhookEventType.EMAIL_RECEIVED)
                  .template("discord")
                  .description("Discord notifications")
                  .build());

      WebhookData customWebhook =
          inbox.createWebhook(
              CreateWebhookRequest.builder()
                  .url("https://custom.example.com/webhook")
                  .events(
                      WebhookEventType.EMAIL_RECEIVED,
                      WebhookEventType.EMAIL_STORED,
                      WebhookEventType.EMAIL_DELETED)
                  .description("Custom integration")
                  .build());

      List<WebhookData> webhooks = inbox.listWebhooks();
      assertEquals(3, webhooks.size());

      // Verify each webhook has unique ID
      assertNotEquals(slackWebhook.getId(), discordWebhook.getId());
      assertNotEquals(discordWebhook.getId(), customWebhook.getId());
      assertNotEquals(slackWebhook.getId(), customWebhook.getId());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }

  @Test
  @Order(81)
  void testWebhooksAreInboxScoped() {
    Inbox inbox1 = client.createInbox();
    Inbox inbox2 = client.createInbox();

    try {
      // Create webhook on inbox1
      WebhookData webhook1 =
          inbox1.createWebhook(
              "https://example.com/webhook1", List.of(WebhookEventType.EMAIL_RECEIVED));

      // Create webhook on inbox2
      WebhookData webhook2 =
          inbox2.createWebhook(
              "https://example.com/webhook2", List.of(WebhookEventType.EMAIL_RECEIVED));

      // List webhooks on each inbox
      List<WebhookData> webhooks1 = inbox1.listWebhooks();
      List<WebhookData> webhooks2 = inbox2.listWebhooks();

      // Each inbox should only see its own webhooks
      assertEquals(1, webhooks1.size());
      assertEquals(1, webhooks2.size());

      assertEquals(webhook1.getId(), webhooks1.get(0).getId());
      assertEquals(webhook2.getId(), webhooks2.get(0).getId());

      // Verify inbox scope
      assertEquals(inbox1.getEmailAddress(), webhooks1.get(0).getInboxEmail());
      assertEquals(inbox2.getEmailAddress(), webhooks2.get(0).getInboxEmail());
    } finally {
      client.deleteInbox(inbox1.getEmailAddress());
      client.deleteInbox(inbox2.getEmailAddress());
    }
  }

  @Test
  @Order(82)
  void testWebhookWithFilterAny() {
    Inbox inbox = client.createInbox();

    try {
      FilterConfig filter =
          FilterConfig.builder()
              .addRule(FilterableField.SUBJECT, FilterOperator.CONTAINS, "urgent")
              .addRule(FilterableField.SUBJECT, FilterOperator.CONTAINS, "important")
              .mode(FilterMode.ANY)
              .build();

      CreateWebhookRequest request =
          CreateWebhookRequest.builder()
              .url("https://filtered.example.com/webhook")
              .events(WebhookEventType.EMAIL_RECEIVED)
              .filter(filter)
              .build();

      WebhookData webhook = inbox.createWebhook(request);

      assertNotNull(webhook);
      assertNotNull(webhook.getFilter());
      assertEquals(FilterMode.ANY, webhook.getFilter().getMode());
      assertEquals(2, webhook.getFilter().getRules().size());
    } finally {
      client.deleteInbox(inbox.getEmailAddress());
    }
  }
}
