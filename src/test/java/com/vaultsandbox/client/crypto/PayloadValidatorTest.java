package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.exception.DecryptionException;
import com.vaultsandbox.client.model.Algorithms;
import com.vaultsandbox.client.model.EncryptedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayloadValidatorTest {

  private PayloadValidator validator;
  private String validServerKey;

  @BeforeEach
  void setUp() {
    validator = new PayloadValidator();
    // Create a valid 1952-byte server key and encode it
    validServerKey = Base64Url.encode(new byte[1952]);
  }

  @Test
  void testValidPayloadPassesValidation() {
    EncryptedPayload payload = createValidPayload();
    assertDoesNotThrow(() -> validator.validate(payload, validServerKey));
  }

  @Test
  void testRejectsInvalidVersion() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.v = 0;

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Unsupported protocol version"));
  }

  @Test
  void testRejectsVersionTwo() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.v = 2;

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Unsupported protocol version: 2"));
  }

  @Test
  void testRejectsNullAlgorithms() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.algs = null;

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Missing algorithm specification"));
  }

  @Test
  void testRejectsInvalidKemAlgorithm() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.algs = new Algorithms("INVALID-KEM", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid KEM algorithm"));
  }

  @Test
  void testRejectsInvalidSigAlgorithm() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.algs = new Algorithms("ML-KEM-768", "INVALID-SIG", "AES-256-GCM", "HKDF-SHA-512");

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid signature algorithm"));
  }

  @Test
  void testRejectsInvalidAeadAlgorithm() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "INVALID-AEAD", "HKDF-SHA-512");

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid AEAD algorithm"));
  }

  @Test
  void testRejectsInvalidKdfAlgorithm() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "INVALID-KDF");

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid KDF algorithm"));
  }

  @Test
  void testRejectsInvalidCtKemSize() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.ctKem = Base64Url.encode(new byte[1000]); // Wrong size (should be 1088)

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid ct_kem size"));
  }

  @Test
  void testRejectsInvalidNonceSize() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.nonce = Base64Url.encode(new byte[16]); // Wrong size (should be 12)

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid nonce size"));
  }

  @Test
  void testRejectsInvalidSignatureSize() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.sig = Base64Url.encode(new byte[3000]); // Wrong size (should be 3309)

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid signature size"));
  }

  @Test
  void testRejectsInvalidServerSigPkSize() {
    TestEncryptedPayload payload = createValidTestPayload();
    payload.serverSigPk = Base64Url.encode(new byte[1000]); // Wrong size (should be 1952)

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, validServerKey));
    assertTrue(ex.getMessage().contains("Invalid server_sig_pk size"));
  }

  @Test
  void testRejectsNullPinnedServerKey() {
    EncryptedPayload payload = createValidPayload();

    DecryptionException ex =
        assertThrows(DecryptionException.class, () -> validator.validate(payload, null));
    assertTrue(ex.getMessage().contains("No pinned server key available"));
  }

  @Test
  void testRejectsServerKeyMismatch() {
    EncryptedPayload payload = createValidPayload();
    // Create a different server key
    byte[] differentKey = new byte[1952];
    differentKey[0] = 1; // Make it different
    String differentServerKey = Base64Url.encode(differentKey);

    DecryptionException ex =
        assertThrows(
            DecryptionException.class, () -> validator.validate(payload, differentServerKey));
    assertTrue(ex.getMessage().contains("Server key mismatch"));
  }

  @Test
  void testAcceptsMatchingServerKey() {
    TestEncryptedPayload payload = createValidTestPayload();
    // The payload's serverSigPk matches validServerKey
    assertDoesNotThrow(() -> validator.validate(payload, validServerKey));
  }

  private EncryptedPayload createValidPayload() {
    return createValidTestPayload();
  }

  private TestEncryptedPayload createValidTestPayload() {
    TestEncryptedPayload payload = new TestEncryptedPayload();
    payload.v = 1;
    payload.algs = new Algorithms("ML-KEM-768", "ML-DSA-65", "AES-256-GCM", "HKDF-SHA-512");
    payload.ctKem = Base64Url.encode(new byte[1088]);
    payload.nonce = Base64Url.encode(new byte[12]);
    payload.aad = Base64Url.encode("test-aad".getBytes());
    payload.ciphertext = Base64Url.encode("test-ciphertext".getBytes());
    payload.sig = Base64Url.encode(new byte[3309]);
    payload.serverSigPk = validServerKey;
    return payload;
  }

  /** Test implementation of EncryptedPayload with writable fields. */
  private static final class TestEncryptedPayload extends EncryptedPayload {
    int v;
    Algorithms algs;
    String ctKem;
    String nonce;
    String aad;
    String ciphertext;
    String sig;
    String serverSigPk;

    @Override
    public int getV() {
      return v;
    }

    @Override
    public Algorithms getAlgs() {
      return algs;
    }

    @Override
    public String getCtKem() {
      return ctKem;
    }

    @Override
    public String getNonce() {
      return nonce;
    }

    @Override
    public String getAad() {
      return aad;
    }

    @Override
    public String getCiphertext() {
      return ciphertext;
    }

    @Override
    public String getSig() {
      return sig;
    }

    @Override
    public String getServerSigPk() {
      return serverSigPk;
    }
  }
}
