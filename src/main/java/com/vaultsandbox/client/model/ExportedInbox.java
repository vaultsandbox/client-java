package com.vaultsandbox.client.model;

import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.exception.InvalidImportDataException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Inbox export format per VaultSandbox spec §9.
 *
 * <p>JSON structure:
 *
 * <pre>{@code
 * {
 *   "version": 1,
 *   "emailAddress": "example@vaultsandbox.com",
 *   "expiresAt": "2024-01-20T15:30:00Z",
 *   "inboxHash": "abc123...",
 *   "serverSigPk": "<base64url>",
 *   "secretKey": "<base64url>",
 *   "exportedAt": "2024-01-13T10:00:00Z"
 * }
 * }</pre>
 *
 * <p>Per spec §9.4, the public key is NOT included as it can be derived from the secret key.
 */
public class ExportedInbox {
  // Export format version per spec §9.3
  private int version = 1;

  private String emailAddress;
  private String expiresAt;
  private String inboxHash;
  private String serverSigPk;

  // Per spec §9.3: field is "secretKey", not "secretKeyB64"
  // Public key is NOT included per spec §9.4
  private String secretKey;

  private String exportedAt;

  // Whether this inbox is encrypted (true) or plain (false)
  // Null defaults to true for backward compatibility
  private Boolean encrypted;

  // Size constants per spec Appendix B
  private static final int MLKEM_SECRET_KEY_SIZE = 2400;
  private static final int MLKEM_PUBLIC_KEY_SIZE = 1184;
  private static final int MLKEM_PUBLIC_KEY_OFFSET = 1152;
  private static final int MLDSA_PUBLIC_KEY_SIZE = 1952;

  public ExportedInbox() {}

  public void validate() throws InvalidImportDataException {
    List<String> errors = new ArrayList<>();

    // Determine if this is an encrypted inbox (default true for backward compatibility)
    boolean isEncrypted = encrypted == null || encrypted;

    // Step 2 per spec §10.1: Validate version
    if (version != 1) {
      errors.add("Unsupported export version: " + version + " (expected 1)");
    }

    // Step 3 per spec §10.1: Validate required fields
    if (emailAddress == null || emailAddress.isBlank()) {
      errors.add("emailAddress is required");
    }
    if (expiresAt == null) {
      errors.add("expiresAt is required");
    }
    if (inboxHash == null || inboxHash.isBlank()) {
      errors.add("inboxHash is required");
    }

    // For encrypted inboxes, serverSigPk and secretKey are required
    if (isEncrypted) {
      if (serverSigPk == null || serverSigPk.isBlank()) {
        errors.add("serverSigPk is required for encrypted inboxes");
      }
      if (secretKey == null || secretKey.isBlank()) {
        errors.add("secretKey is required for encrypted inboxes");
      }
    }

    // Step 4 per spec §10.1: Validate emailAddress contains exactly one @
    if (emailAddress != null && !emailAddress.isBlank()) {
      long atCount = emailAddress.chars().filter(c -> c == '@').count();
      if (atCount != 1) {
        errors.add("emailAddress must contain exactly one '@' character");
      }
    }

    // Step 5 per spec §10.1: Validate inboxHash is non-empty (already checked above)

    // Step 6 per spec §10.1: Validate and decode secretKey (only for encrypted inboxes)
    byte[] sk = null;
    if (secretKey != null && !secretKey.isBlank()) {
      try {
        sk = Base64Url.decode(secretKey);
        if (sk.length != MLKEM_SECRET_KEY_SIZE) {
          errors.add("secretKey must be " + MLKEM_SECRET_KEY_SIZE + " bytes (raw ML-KEM-768)");
        }
      } catch (IllegalArgumentException e) {
        errors.add("secretKey is not valid base64url: " + e.getMessage());
      }
    }

    // Step 7 per spec §10.1: Validate and decode serverSigPk (only for encrypted inboxes)
    if (serverSigPk != null && !serverSigPk.isBlank()) {
      try {
        byte[] sigPk = Base64Url.decode(serverSigPk);
        if (sigPk.length != MLDSA_PUBLIC_KEY_SIZE) {
          errors.add("serverSigPk must be " + MLDSA_PUBLIC_KEY_SIZE + " bytes (raw ML-DSA-65)");
        }
      } catch (IllegalArgumentException e) {
        errors.add("serverSigPk is not valid base64url: " + e.getMessage());
      }
    }

    // Step 8 per spec §10.1: Validate timestamps
    if (expiresAt != null) {
      try {
        Instant.parse(expiresAt);
      } catch (DateTimeParseException e) {
        errors.add("expiresAt must be valid ISO 8601");
      }
    }

    if (exportedAt != null) {
      try {
        Instant.parse(exportedAt);
      } catch (DateTimeParseException e) {
        errors.add("exportedAt must be valid ISO 8601");
      }
    }

    // Check expiration
    if (expiresAt != null) {
      try {
        Instant expires = Instant.parse(expiresAt);
        if (expires.isBefore(Instant.now())) {
          errors.add("inbox has expired");
        }
      } catch (DateTimeParseException e) {
        // Already caught above
      }
    }

    if (!errors.isEmpty()) {
      throw new InvalidImportDataException(errors);
    }
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getInboxHash() {
    return inboxHash;
  }

  public void setInboxHash(String inboxHash) {
    this.inboxHash = inboxHash;
  }

  public String getServerSigPk() {
    return serverSigPk;
  }

  public void setServerSigPk(String serverSigPk) {
    this.serverSigPk = serverSigPk;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  /**
   * Derives the public key from the secret key per spec §10.2.
   *
   * @return the derived public key bytes, or null if secretKey is not set
   */
  public byte[] derivePublicKey() {
    if (secretKey == null || secretKey.isBlank()) {
      return null;
    }
    byte[] sk = Base64Url.decode(secretKey);
    if (sk.length != MLKEM_SECRET_KEY_SIZE) {
      return null;
    }
    return Arrays.copyOfRange(
        sk, MLKEM_PUBLIC_KEY_OFFSET, MLKEM_PUBLIC_KEY_OFFSET + MLKEM_PUBLIC_KEY_SIZE);
  }

  public String getExportedAt() {
    return exportedAt;
  }

  public void setExportedAt(String exportedAt) {
    this.exportedAt = exportedAt;
  }

  /**
   * Returns whether this inbox is encrypted.
   *
   * @return {@code true} if encrypted, {@code false} if plain, {@code null} if not specified
   */
  public Boolean getEncrypted() {
    return encrypted;
  }

  public void setEncrypted(Boolean encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Returns whether this inbox uses encryption.
   *
   * <p>Defaults to {@code true} for backward compatibility.
   *
   * @return {@code true} if encrypted or not specified, {@code false} if explicitly plain
   */
  public boolean isEncrypted() {
    return encrypted == null || encrypted;
  }
}
