package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailHashComputerTest {

  @Test
  void computeEmptyList() {
    String hash = CryptoUtils.computeEmailsHash(List.of());
    // SHA-256 of empty string
    assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", hash);
  }

  @Test
  void computeHashSortsIds() {
    String hash1 = CryptoUtils.computeEmailsHash(List.of("c", "a", "b"));
    String hash2 = CryptoUtils.computeEmailsHash(List.of("a", "b", "c"));
    assertEquals(hash1, hash2);
  }

  @Test
  void computeHashIsDeterministic() {
    String hash1 = CryptoUtils.computeEmailsHash(List.of("id1", "id2"));
    String hash2 = CryptoUtils.computeEmailsHash(List.of("id1", "id2"));
    assertEquals(hash1, hash2);
  }

  @Test
  void computeHashDiffersForDifferentIds() {
    String hash1 = CryptoUtils.computeEmailsHash(List.of("id1"));
    String hash2 = CryptoUtils.computeEmailsHash(List.of("id2"));
    assertNotEquals(hash1, hash2);
  }
}
