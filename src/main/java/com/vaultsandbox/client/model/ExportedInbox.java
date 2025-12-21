package com.vaultsandbox.client.model;

import com.vaultsandbox.client.crypto.Base64Url;
import com.vaultsandbox.client.exception.InvalidImportDataException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExportedInbox {
  private String emailAddress;
  private String expiresAt;
  private String inboxHash;
  private String serverSigPk;
  private String publicKeyB64;
  private String secretKeyB64;
  private String exportedAt;

  public ExportedInbox() {}

  public void validate() throws InvalidImportDataException {
    List<String> errors = new ArrayList<>();

    // 1. Check required fields
    if (emailAddress == null || emailAddress.isBlank()) {
      errors.add("emailAddress is required");
    }
    if (expiresAt == null) {
      errors.add("expiresAt is required");
    }
    if (inboxHash == null || inboxHash.isBlank()) {
      errors.add("inboxHash is required");
    }
    if (serverSigPk == null || serverSigPk.isBlank()) {
      errors.add("serverSigPk is required");
    }
    if (publicKeyB64 == null || publicKeyB64.isBlank()) {
      errors.add("publicKeyB64 is required");
    }
    if (secretKeyB64 == null || secretKeyB64.isBlank()) {
      errors.add("secretKeyB64 is required");
    }

    // 2. Validate timestamps
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

    // 3. Validate base64 encoding and key format (raw ML-KEM-768 keys)
    byte[] pk = null;
    byte[] sk = null;

    if (publicKeyB64 != null && !publicKeyB64.isBlank()) {
      try {
        pk = Base64Url.decode(publicKeyB64);
        // Raw ML-KEM-768 public key is 1184 bytes
        if (pk.length != 1184) {
          errors.add("publicKey must be 1184 bytes (raw ML-KEM-768)");
        }
      } catch (IllegalArgumentException e) {
        errors.add("publicKeyB64 is not valid base64url");
      }
    }

    if (secretKeyB64 != null && !secretKeyB64.isBlank()) {
      try {
        sk = Base64Url.decode(secretKeyB64);
        // Raw ML-KEM-768 secret key is 2400 bytes
        if (sk.length != 2400) {
          errors.add("secretKey must be 2400 bytes (raw ML-KEM-768)");
        }
      } catch (IllegalArgumentException e) {
        errors.add("secretKeyB64 is not valid base64url");
      }
    }

    // 4. Verify public key can be derived from secret key
    if (pk != null && sk != null && pk.length == 1184 && sk.length == 2400) {
      byte[] derivedPk = Arrays.copyOfRange(sk, 1152, 2336);
      if (!Arrays.equals(derivedPk, pk)) {
        errors.add("publicKey does not match secretKey");
      }
    }

    // 5. Check expiration
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

  public String getPublicKeyB64() {
    return publicKeyB64;
  }

  public void setPublicKeyB64(String publicKeyB64) {
    this.publicKeyB64 = publicKeyB64;
  }

  public String getSecretKeyB64() {
    return secretKeyB64;
  }

  public void setSecretKeyB64(String secretKeyB64) {
    this.secretKeyB64 = secretKeyB64;
  }

  public String getExportedAt() {
    return exportedAt;
  }

  public void setExportedAt(String exportedAt) {
    this.exportedAt = exportedAt;
  }
}
