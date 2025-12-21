package com.vaultsandbox.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.TestHelpers;
import org.junit.jupiter.api.Test;

class AuthResultsTest {

  // ==================== Passing Auth Tests ====================

  @Test
  void testAllAuthPassing() {
    AuthResults results = TestHelpers.createPassingAuthResults();
    AuthValidation validation = results.validate();

    assertTrue(validation.isFullyAuthenticated());
    assertTrue(validation.hasSpf());
    assertTrue(validation.hasDkim());
    assertTrue(validation.hasDmarc());
    assertEquals(4, validation.getPassed().size()); // SPF, DKIM, DMARC, ReverseDNS
    assertTrue(validation.getFailed().isEmpty());
  }

  @Test
  void testPassedContainsExpectedValues() {
    AuthResults results = TestHelpers.createPassingAuthResults();
    AuthValidation validation = results.validate();

    assertTrue(validation.getPassed().contains("SPF"));
    assertTrue(validation.getPassed().contains("DKIM"));
    assertTrue(validation.getPassed().contains("DMARC"));
    assertTrue(validation.getPassed().contains("ReverseDNS"));
  }

  // ==================== Failing SPF Tests ====================

  @Test
  void testFailingSpf() {
    AuthResults results = TestHelpers.createFailingSpfAuthResults();
    AuthValidation validation = results.validate();

    assertFalse(validation.isFullyAuthenticated());
    assertFalse(validation.hasSpf());
    assertTrue(validation.hasDkim());
    assertTrue(validation.hasDmarc());

    assertTrue(validation.getFailed().stream().anyMatch(s -> s.contains("SPF")));
  }

  @Test
  void testFailingSpfMessage() {
    AuthResults results = TestHelpers.createFailingSpfAuthResults();
    AuthValidation validation = results.validate();

    // Should contain the result value
    assertTrue(
        validation.getFailed().stream().anyMatch(s -> s.contains("SPF") && s.contains("fail")));
  }

  // ==================== Failing DKIM Tests ====================

  @Test
  void testFailingDkim() {
    AuthResults results = TestHelpers.createFailingDkimAuthResults();
    AuthValidation validation = results.validate();

    assertFalse(validation.isFullyAuthenticated());
    assertTrue(validation.hasSpf());
    assertFalse(validation.hasDkim());
    assertTrue(validation.hasDmarc());

    assertTrue(validation.getFailed().stream().anyMatch(s -> s.contains("DKIM")));
  }

  // ==================== Failing DMARC Tests ====================

  @Test
  void testFailingDmarc() {
    AuthResults results = TestHelpers.createFailingDmarcAuthResults();
    AuthValidation validation = results.validate();

    assertFalse(validation.isFullyAuthenticated());
    assertTrue(validation.hasSpf());
    assertTrue(validation.hasDkim());
    assertFalse(validation.hasDmarc());

    assertTrue(validation.getFailed().stream().anyMatch(s -> s.contains("DMARC")));
  }

  // ==================== Invalid Reverse DNS Tests ====================

  @Test
  void testInvalidReverseDns() {
    AuthResults results = TestHelpers.createInvalidReverseDnsAuthResults();
    AuthValidation validation = results.validate();

    assertFalse(validation.isFullyAuthenticated());
    assertTrue(validation.hasSpf());
    assertTrue(validation.hasDkim());
    assertTrue(validation.hasDmarc());
    assertFalse(validation.hasReverseDns());

    assertTrue(validation.getFailed().stream().anyMatch(s -> s.contains("ReverseDNS")));
  }

  @Test
  void testHasReverseDnsWhenPassing() {
    AuthResults results = TestHelpers.createPassingAuthResults();
    AuthValidation validation = results.validate();

    assertTrue(validation.hasReverseDns());
  }

  @Test
  void testHasReverseDnsWhenAbsent() {
    AuthResults results = TestHelpers.createEmptyAuthResults();
    AuthValidation validation = results.validate();

    assertFalse(validation.hasReverseDns());
  }

  // ==================== Empty Results Tests ====================

  @Test
  void testEmptyAuthResults() {
    AuthResults results = TestHelpers.createEmptyAuthResults();
    AuthValidation validation = results.validate();

    // No checks at all - should not be considered fully authenticated
    assertFalse(validation.isFullyAuthenticated());
    assertFalse(validation.hasSpf());
    assertFalse(validation.hasDkim());
    assertFalse(validation.hasDmarc());
    assertTrue(validation.getPassed().isEmpty());
    assertTrue(validation.getFailed().isEmpty());
  }

  // ==================== None Status Tests ====================

  @Test
  void testNoneStatusNotPassing() {
    AuthResults results = TestHelpers.createNoneStatusAuthResults();
    AuthValidation validation = results.validate();

    // "none" status means the check was not performed or not applicable
    // Should NOT be considered as passing
    assertFalse(validation.isFullyAuthenticated(), "None status should not pass");
    assertFalse(validation.hasSpf(), "SPF with 'none' result should not count as passed");
    assertFalse(validation.hasDkim(), "DKIM with 'none' result should not count as passed");
    assertFalse(validation.hasDmarc(), "DMARC with 'none' result should not count as passed");
    assertFalse(validation.hasReverseDns(), "Invalid reverse DNS should not pass");
  }

  // ==================== Getter Tests ====================

  @Test
  void testSpfResultGetter() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getSpf());
    assertEquals("pass", results.getSpf().getResult());
    assertEquals("example.com", results.getSpf().getDomain());
  }

  @Test
  void testDkimResultGetter() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getDkim());
    assertFalse(results.getDkim().isEmpty());
    assertEquals("pass", results.getDkim().get(0).getResult());
    assertEquals("example.com", results.getDkim().get(0).getDomain());
    assertEquals("default", results.getDkim().get(0).getSelector());
  }

  @Test
  void testDmarcResultGetter() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getDmarc());
    assertEquals("pass", results.getDmarc().getResult());
    assertEquals("example.com", results.getDmarc().getDomain());
  }

  @Test
  void testReverseDnsResultGetter() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getReverseDns());
    assertTrue(results.getReverseDns().isValid());
    assertEquals("mail.example.com", results.getReverseDns().getHostname());
  }

  // ==================== AuthValidation Tests ====================

  @Test
  void testAuthValidationImmutability() {
    AuthResults results = TestHelpers.createPassingAuthResults();
    AuthValidation validation = results.validate();

    // Lists should be immutable
    assertThrows(UnsupportedOperationException.class, () -> validation.getPassed().add("Test"));
    assertThrows(UnsupportedOperationException.class, () -> validation.getFailed().add("Test"));
  }

  @Test
  void testFullyAuthenticatedRequiresPassingChecks() {
    AuthResults results = new AuthResults();
    AuthValidation validation = results.validate();

    // Empty results should not be fully authenticated
    assertFalse(validation.isFullyAuthenticated());
  }

  // ==================== Multiple DKIM Signatures Tests ====================

  @Test
  void testMultipleDkimSignaturesOnePass() {
    // If any DKIM signature passes, DKIM should be considered passed
    AuthResults results = TestHelpers.createPassingAuthResults();
    AuthValidation validation = results.validate();

    assertTrue(validation.hasDkim());
    assertTrue(validation.getPassed().contains("DKIM"));
  }
}
