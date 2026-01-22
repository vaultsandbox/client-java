<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./assets/logo-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="./assets/logo-light.svg">
  <img alt="VaultSandbox" src="./assets/logo-dark.svg">
</picture>

> **VaultSandbox is in Public Beta.** Join the journey to 1.0. Share feedback on [GitHub](https://github.com/vaultsandbox/gateway/discussions).

# VaultSandbox Java Client

[![CI](https://github.com/vaultsandbox/client-java/actions/workflows/ci.yml/badge.svg)](https://github.com/vaultsandbox/client-java/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-%3E%3D21-blue.svg)](https://openjdk.org/)

**Production-like email testing. Self-hosted & secure.**

The official Java SDK for [VaultSandbox Gateway](https://github.com/vaultsandbox/gateway) — a secure, receive-only SMTP server for QA/testing environments. This SDK abstracts encryption complexity, making email testing workflows transparent and effortless.

Stop mocking your email stack. If your app sends real emails in production, it must send real emails in testing. VaultSandbox provides isolated inboxes that behave exactly like production without exposing a single byte of customer data.

> **Java 21+** required.

## Why VaultSandbox?

| Feature             | Simple Mocks     | Public SaaS  | **VaultSandbox**    |
| :------------------ | :--------------- | :----------- | :------------------ |
| **TLS/SSL**         | Ignored/Disabled | Partial      | **Real ACME certs** |
| **Data Privacy**    | Local only       | Shared cloud | **Private VPC**     |
| **Inbound Mail**    | Outbound only    | Yes          | **Real MX**         |
| **Auth (SPF/DKIM)** | None             | Limited      | **Full Validation** |
| **Crypto**          | Plaintext        | Varies       | **Zero-Knowledge**  |

## Features

- **Quantum-Safe Encryption** — Automatic ML-KEM-768 (Kyber768) key encapsulation + AES-256-GCM encryption
- **Zero Crypto Knowledge Required** — All cryptographic operations are invisible to the user
- **Real-Time Email Delivery** — SSE-based delivery with smart polling fallback
- **Built for CI/CD** — Deterministic tests without sleeps, polling, or flakiness
- **Full Email Access** — Decrypt and access email content, headers, links, and attachments
- **Email Authentication** — Built-in SPF/DKIM/DMARC validation helpers
- **Thread-Safe** — Designed for concurrent use in test frameworks
- **[Spam Analysis](https://vaultsandbox.dev/client-java/concepts/spam-analysis/)** — Rspamd integration for spam scores, classifications, and rule analysis
- **[Webhooks](https://vaultsandbox.dev/client-java/guides/webhooks/)** — Global and per-inbox HTTP callbacks for email events with filtering and templates
- **[Chaos Engineering](https://vaultsandbox.dev/client-java/guides/chaos/)** — Per-inbox SMTP failure simulation (latency, drops, errors, greylisting, blackhole)

## Installation

### Gradle

```groovy
dependencies {
    testImplementation 'com.vaultsandbox:client:0.9.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.vaultsandbox</groupId>
    <artifactId>client</artifactId>
    <version>0.9.0</version>
    <scope>test</scope>
</dependency>
```

## Quick Start

```java
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.Email;

// Initialize client with your API key
VaultSandboxClient client = VaultSandboxClient.create("your-api-key");

// Create inbox (keypair generated automatically)
Inbox inbox = client.createInbox();
System.out.println("Send email to: " + inbox.getEmailAddress());

// Wait for email with timeout
Email email = inbox.waitForEmail();

// Email is already decrypted - just use it!
System.out.println("From: " + email.getFrom());
System.out.println("Subject: " + email.getSubject());
System.out.println("Text: " + email.getText());
System.out.println("HTML: " + email.getHtml());

// Cleanup
client.deleteInbox(inbox.getEmailAddress());
client.close();
```

## Usage Examples

### Testing Password Reset Emails

```java
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.ClientConfig;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.strategy.EmailFilter;
import java.time.Duration;

ClientConfig config = ClientConfig.builder()
    .apiKey("your-api-key")
    .baseUrl("https://smtp.vaultsandbox.com")
    .build();

try (VaultSandboxClient client = VaultSandboxClient.create(config)) {
    Inbox inbox = client.createInbox();

    // Trigger password reset in your app
    yourApp.requestPasswordReset(inbox.getEmailAddress());

    // Wait for and validate the reset email
    Email email = inbox.waitForEmail(
        EmailFilter.subjectContains("Reset your password"),
        Duration.ofSeconds(10)
    );

    // Extract reset link
    String resetLink = email.getLinks().stream()
        .filter(url -> url.contains("/reset-password"))
        .findFirst()
        .orElse(null);
    System.out.println("Reset link: " + resetLink);

    // Validate email authentication
    var validation = email.getAuthResults().validate();
    assertNotNull(validation);
}
```

### Testing Email Authentication (SPF/DKIM/DMARC)

```java
Email email = inbox.waitForEmail(Duration.ofSeconds(5));
var validation = email.getAuthResults().validate();

if (!validation.isPassed()) {
    System.err.println("Email authentication failed:");
    validation.getFailures().forEach(reason ->
        System.err.println("  - " + reason)
    );
}

// Or check individual results
var authResults = email.getAuthResults();
if (authResults.getSpf() != null) {
    System.out.println("SPF result: " + authResults.getSpf().getResult());
}
if (authResults.getDkim() != null) {
    System.out.println("DKIM signatures: " + authResults.getDkim().size());
}
if (authResults.getDmarc() != null) {
    System.out.println("DMARC result: " + authResults.getDmarc().getResult());
}
```

### Extracting and Validating Links

```java
Email email = inbox.waitForEmail(EmailFilter.subjectContains("Verify your email"));

// All links are automatically extracted
String verifyLink = email.getLinks().stream()
    .filter(url -> url.contains("/verify"))
    .findFirst()
    .orElseThrow();

assertNotNull(verifyLink);
assertTrue(verifyLink.startsWith("https://"));
```

### Working with Email Attachments

Email attachments are automatically decrypted and available as byte arrays, ready to be processed or saved.

```java
import java.nio.file.Files;
import java.nio.file.Path;

Email email = inbox.waitForEmail(EmailFilter.subjectContains("Documents Attached"));

// Access attachments list
System.out.println("Found " + email.getAttachments().size() + " attachments");

// Iterate through attachments
for (var attachment : email.getAttachments()) {
    System.out.println("Filename: " + attachment.getFilename());
    System.out.println("Content-Type: " + attachment.getContentType());
    System.out.println("Size: " + attachment.getSize() + " bytes");

    if (attachment.getContent() == null) continue;

    // Decode text-based attachments
    if (attachment.getContentType().contains("text")) {
        String textContent = new String(attachment.getContent(), StandardCharsets.UTF_8);
        System.out.println("Content: " + textContent);
    }

    // Parse JSON attachments
    if (attachment.getContentType().contains("json")) {
        String jsonContent = new String(attachment.getContent(), StandardCharsets.UTF_8);
        // Parse with your preferred JSON library
    }

    // Save binary files to disk
    if (attachment.getContentType().contains("pdf")
            || attachment.getContentType().contains("image")) {
        Files.write(Path.of("./downloads/" + attachment.getFilename()),
                    attachment.getContent());
        System.out.println("Saved " + attachment.getFilename());
    }
}

// Find and verify specific attachment in tests
var pdfAttachment = email.getAttachments().stream()
    .filter(att -> "invoice.pdf".equals(att.getFilename()))
    .findFirst()
    .orElseThrow();
assertEquals("application/pdf", pdfAttachment.getContentType());
assertTrue(pdfAttachment.getSize() > 0);
```

### Testing with JUnit 5

```java
import org.junit.jupiter.api.*;
import com.vaultsandbox.client.VaultSandboxClient;
import com.vaultsandbox.client.Inbox;
import com.vaultsandbox.client.strategy.EmailFilter;

class EmailFlowTest {
    private VaultSandboxClient client;
    private Inbox inbox;

    @BeforeEach
    void setUp() {
        client = VaultSandboxClient.create("your-api-key");
        inbox = client.createInbox();
    }

    @AfterEach
    void tearDown() {
        if (inbox != null) {
            client.deleteInbox(inbox.getEmailAddress());
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void shouldReceiveWelcomeEmail() {
        sendWelcomeEmail(inbox.getEmailAddress());

        var email = inbox.waitForEmail(
            EmailFilter.subjectContains("Welcome"),
            Duration.ofSeconds(5)
        );

        assertEquals("noreply@example.com", email.getFrom());
        assertTrue(email.getText().contains("Thank you for signing up"));
    }
}
```

### Waiting for Multiple Emails

When testing scenarios that send multiple emails, use `waitForEmailCount()` instead of arbitrary timeouts for faster and more reliable tests:

```java
@Test
void shouldReceiveMultipleNotificationEmails() {
    // Send multiple emails
    sendNotifications(inbox.getEmailAddress(), 3);

    // Wait for all 3 emails to arrive
    List<Email> emails = inbox.waitForEmailCount(3, Duration.ofSeconds(30));

    assertEquals(3, emails.size());
    assertTrue(emails.get(0).getSubject().contains("Notification"));
}
```

### Real-time Monitoring

For scenarios where you need to process emails as they arrive without blocking, you can use the `onNewEmail` subscription.

```java
import com.vaultsandbox.client.strategy.Subscription;

Inbox inbox = client.createInbox();
System.out.println("Watching for emails at: " + inbox.getEmailAddress());

// Subscribe to new emails
Subscription subscription = inbox.onNewEmail(email -> {
    System.out.println("New email received: \"" + email.getSubject() + "\"");
    // Process the email here...
});

// To stop listening for emails later:
// subscription.unsubscribe();
```

### Null-Safe Waiting with awaitEmail

If you prefer to handle timeouts without exceptions, use `awaitEmail()` which returns `null` on timeout:

```java
// Returns null instead of throwing TimeoutException
Email email = inbox.awaitEmail(EmailFilter.any(), Duration.ofSeconds(2));
if (email != null) {
    // Process the email
}
```

## API Reference

### VaultSandboxClient

The main client class for interacting with the VaultSandbox Gateway.

#### Factory Methods

```java
// Simple creation with just API key
VaultSandboxClient client = VaultSandboxClient.create("your-api-key");

// Full configuration
VaultSandboxClient client = VaultSandboxClient.create(
    ClientConfig.builder()
        .apiKey("your-api-key")
        .baseUrl("https://smtp.vaultsandbox.com")
        .strategy(StrategyType.AUTO)
        .waitTimeout(Duration.ofSeconds(30))
        .maxRetries(3)
        .build()
);
```

#### ClientConfig Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiKey` | String | *required* | Your API key |
| `baseUrl` | String | `https://smtp.vaultsandbox.com` | Gateway URL |
| `strategy` | StrategyType | `AUTO` | Delivery strategy (`SSE`, `POLLING`, `AUTO`) |
| `httpTimeout` | Duration | 30s | HTTP request timeout |
| `waitTimeout` | Duration | 30s | Default timeout for waitForEmail |
| `maxRetries` | int | 3 | Max retry attempts for HTTP requests |
| `retryDelay` | Duration | 1s | Delay between retry attempts |
| `pollInterval` | Duration | 2s | Polling interval |
| `sseReconnectInterval` | Duration | 5s | Initial SSE reconnection delay |
| `sseMaxReconnectAttempts` | int | 10 | Max SSE reconnection attempts |

#### Methods

| Method | Description |
|--------|-------------|
| `createInbox()` | Creates a new inbox with auto-generated keypair |
| `deleteInbox(String emailAddress)` | Deletes a specific inbox |
| `deleteAllInboxes()` | Deletes all inboxes for this API key, returns count |
| `getServerInfo()` | Gets server information |
| `checkKey()` | Validates API key, returns boolean |
| `getInbox(String emailAddress)` | Gets a locally tracked inbox by email address |
| `exportInbox(Inbox inbox)` | Exports an inbox's data for backup or sharing |
| `importInbox(ExportedInbox data)` | Imports an inbox from exported data |
| `exportInboxToFile(Inbox inbox, Path path)` | Exports an inbox to a JSON file |
| `importInboxFromFile(Path path)` | Imports an inbox from a JSON file |
| `monitorInboxes(Inbox... inboxes)` | Monitors multiple inboxes for new emails |
| `close()` | Closes the client and cleans up resources |

### Inbox

Represents a single email inbox.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `emailAddress` | String | The inbox email address |
| `hash` | String | Unique inbox identifier |
| `expiresAt` | Instant | When the inbox expires |

#### Methods

| Method | Description |
|--------|-------------|
| `listEmails()` | Lists all emails (decrypted) |
| `getEmail(String emailId)` | Gets a specific email |
| `getRawEmail(String emailId)` | Gets the raw email source (RFC 5322) |
| `waitForEmail()` | Waits for any email (default timeout) |
| `waitForEmail(EmailFilter filter)` | Waits for an email matching filter |
| `waitForEmail(EmailFilter filter, Duration timeout)` | Waits with custom timeout |
| `waitForEmailCount(int count)` | Waits for N emails to arrive |
| `waitForEmailCount(int count, Duration timeout)` | Waits for N emails with timeout |
| `awaitEmail()` | Like waitForEmail but returns null on timeout |
| `awaitEmail(EmailFilter filter)` | Like waitForEmail(filter) but returns null |
| `awaitEmail(EmailFilter filter, Duration timeout)` | Like waitForEmail with timeout, returns null |
| `onNewEmail(Consumer<Email> callback)` | Subscribes to new emails in real-time |
| `getSyncStatus()` | Gets inbox sync status |
| `markEmailAsRead(String emailId)` | Marks email as read |
| `deleteEmail(String emailId)` | Deletes an email |
| `export()` | Exports inbox data for backup/sharing |

### EmailFilter

Filter criteria for waiting on emails.

```java
// Match any email
EmailFilter.any()

// Match by subject (contains)
EmailFilter.subjectContains("Welcome")

// Match by subject (regex)
EmailFilter.subjectMatches(Pattern.compile("Order #\\d+"))

// Match by sender (contains)
EmailFilter.from("noreply@")

// Match by sender (regex)
EmailFilter.fromMatches(Pattern.compile(".*@example\\.com"))

// Custom predicate
EmailFilter.matching(email -> !email.getLinks().isEmpty())

// Combine filters
EmailFilter.subjectContains("Reset").and(EmailFilter.from("auth@"))
```

### Email

Represents a decrypted email.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | String | Email ID |
| `from` | String | Sender address |
| `to` | List<String> | Recipient addresses |
| `subject` | String | Email subject |
| `text` | String | Plain text content (may be null) |
| `html` | String | HTML content (may be null) |
| `receivedAt` | Instant | When the email was received |
| `isRead` | boolean | Read status |
| `links` | List<String> | Extracted URLs from email |
| `headers` | Map<String, String> | All email headers |
| `attachments` | List<Attachment> | Email attachments |
| `authResults` | AuthResults | Email authentication results |

#### Methods

| Method | Description |
|--------|-------------|
| `markAsRead()` | Marks this email as read |
| `delete()` | Deletes this email |
| `getRaw()` | Gets raw email source (RFC 5322) |

### AuthResults

Email authentication results (SPF/DKIM/DMARC).

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `spf` | SpfResult | SPF result |
| `dkim` | List<DkimResult> | All DKIM results |
| `dmarc` | DmarcResult | DMARC result |
| `reverseDns` | ReverseDnsResult | Reverse DNS result |

#### Methods

| Method | Description |
|--------|-------------|
| `validate()` | Returns AuthValidation with `isPassed()`, per-check booleans, and `getFailures()` list |

## Error Handling

The SDK includes automatic retries for transient network and server errors, and throws specific exceptions for different failure scenarios.

All custom exceptions extend from the base `VaultSandboxException` class.

### Automatic Retries

By default, the client automatically retries failed HTTP requests that result in status codes: `408`, `429`, `500`, `502`, `503`, `504`.

### Exception Types

| Exception | Description |
|-----------|-------------|
| `ApiException` | API-level errors (includes `getStatusCode()`) |
| `NetworkException` | Network-level failures |
| `TimeoutException` | Timeout waiting for email |
| `InboxNotFoundException` | Inbox does not exist (HTTP 404) |
| `EmailNotFoundException` | Email does not exist (HTTP 404) |
| `InboxAlreadyExistsException` | Attempting to import existing inbox |
| `InvalidImportDataException` | Invalid import data |
| `DecryptionException` | Failed to decrypt email |
| `SignatureVerificationException` | Signature verification failed (potential MITM) |
| `SseException` | SSE connection errors |

### Example

```java
import com.vaultsandbox.client.exception.*;

try (VaultSandboxClient client = VaultSandboxClient.create(config)) {
    Inbox inbox = client.createInbox();
    System.out.println("Send email to: " + inbox.getEmailAddress());

    // This might throw TimeoutException
    Email email = inbox.waitForEmail(Duration.ofSeconds(5));

    System.out.println("Email received: " + email.getSubject());
} catch (TimeoutException e) {
    System.err.println("Timed out waiting for email: " + e.getMessage());
} catch (ApiException e) {
    System.err.println("API Error (" + e.getStatusCode() + "): " + e.getMessage());
} catch (VaultSandboxException e) {
    // Catch any other SDK-specific exception
    System.err.println("An unexpected SDK error occurred: " + e.getMessage());
}
```

## Requirements

- Java 21 or later
- VaultSandbox Gateway server
- Valid API key

## Building

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run integration tests (requires gateway)
./gradlew integrationTest

# Run all tests with coverage
./gradlew jacocoFullReport

# Format code
./gradlew spotlessApply

# Check code style
./gradlew checkstyleMain checkstyleTest
```

## Architecture

The SDK is built on several layers:

1. **Crypto Layer** — ML-KEM-768 keypair generation, AES-256-GCM encryption/decryption, and ML-DSA-65 signature verification using Bouncy Castle
2. **HTTP Layer** — REST API client using OkHttp with automatic retry and error handling
3. **Domain Layer** — Email, Inbox, and Client classes with intuitive APIs
4. **Strategy Layer** — SSE and polling strategies for email delivery

All cryptographic operations are performed transparently — developers never need to handle keys, encryption, or signatures directly.

## Security

- **Cryptography**: ML-KEM-768 (Kyber768) for key encapsulation + AES-256-GCM for payload encryption, with HKDF-SHA-512 key derivation
- **Signatures**: ML-DSA-65 (Dilithium3) signatures are verified **before** any decryption
- **Threat model**: Protects confidentiality/integrity of gateway responses and detects tampering/MITM
- **Key handling**: Inbox keypairs stay in memory only; exported inbox data contains secrets and must be treated as sensitive
- **Validation**: Signature verification failures throw `SignatureVerificationException`; decryption issues throw `DecryptionException`

## Related

- [VaultSandbox Gateway](https://github.com/vaultsandbox/gateway) — The self-hosted SMTP server this SDK connects to
- [VaultSandbox Documentation](https://vaultsandbox.dev) — Full documentation and guides

## Support

- [Documentation](https://vaultsandbox.dev/client-java/)
- [Issue Tracker](https://github.com/vaultsandbox/client-java/issues)
- [Discussions](https://github.com/vaultsandbox/gateway/discussions)
- [Website](https://www.vaultsandbox.com)

## Contributing

Contributions are welcome! Please read our [contributing guidelines](CONTRIBUTING.md) before submitting PRs.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.
