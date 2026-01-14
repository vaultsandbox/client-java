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

  // ==================== SPF Softfail Tests ====================

  @Test
  void testSoftfailSpfIsNeutral() {
    // Per spec: softfail should be "flagged as suspicious" but NOT counted as failed
    AuthResults results = TestHelpers.createSoftfailSpfAuthResults();
    AuthValidation validation = results.validate();

    // SPF softfail should NOT be in passed list
    assertFalse(validation.hasSpf(), "SPF softfail should not count as passed");
    assertFalse(validation.getPassed().contains("SPF"), "SPF should not be in passed list");

    // SPF softfail should NOT be in failed list either (it's neutral)
    assertTrue(
        validation.getFailed().stream().noneMatch(s -> s.contains("SPF")),
        "SPF softfail should not be in failed list (neutral per spec)");

    // Other checks should still pass
    assertTrue(validation.hasDkim());
    assertTrue(validation.hasDmarc());
    assertTrue(validation.hasReverseDns());
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

  // ==================== Skipped Status Tests ====================

  @Test
  void testSkippedStatusIsNeutral() {
    AuthResults results = TestHelpers.createSkippedAuthResults();
    AuthValidation validation = results.validate();

    // "skipped" status means email auth is disabled for the inbox
    // All checks should be treated as neutral (not passed, not failed)
    assertFalse(validation.isFullyAuthenticated(), "Skipped status should not pass");
    assertFalse(validation.hasSpf(), "SPF with 'skipped' result should not count as passed");
    assertFalse(validation.hasDkim(), "DKIM with 'skipped' result should not count as passed");
    assertFalse(validation.hasDmarc(), "DMARC with 'skipped' result should not count as passed");
    assertFalse(validation.hasReverseDns(), "ReverseDNS with 'skipped' result should not pass");

    // All skipped statuses should be neutral (not in passed or failed list)
    assertTrue(validation.getPassed().isEmpty(), "Skipped status should not be in passed list");
    assertTrue(validation.getFailed().isEmpty(), "Skipped status should not be in failed list");
  }

  // ==================== None Status Tests ====================

  @Test
  void testNoneStatusIsNeutral() {
    AuthResults results = TestHelpers.createNoneStatusAuthResults();
    AuthValidation validation = results.validate();

    // "none" status means the check was not performed or not applicable
    // Should be treated as neutral (not passed, not failed) for SPF/DKIM/DMARC
    // ReverseDNS with 'none' is treated as failure (not verified)
    assertFalse(validation.isFullyAuthenticated(), "None status should not pass");
    assertFalse(validation.hasSpf(), "SPF with 'none' result should not count as passed");
    assertFalse(validation.hasDkim(), "DKIM with 'none' result should not count as passed");
    assertFalse(validation.hasDmarc(), "DMARC with 'none' result should not count as passed");
    assertFalse(validation.hasReverseDns(), "ReverseDNS with 'none' result should not pass");

    // SPF/DKIM/DMARC none status should NOT be in the failed list (it's neutral)
    // But ReverseDNS none will be in failed list
    assertTrue(validation.getPassed().isEmpty(), "None status should not be in passed list");
    assertTrue(
        validation.getFailed().stream().anyMatch(s -> s.contains("ReverseDNS")),
        "ReverseDNS none should be in failed list");
    assertEquals(1, validation.getFailed().size(), "Only ReverseDNS should be in failed list");
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
    assertEquals("pass", results.getReverseDns().getResult());
    assertEquals("mail.example.com", results.getReverseDns().getHostname());
    assertEquals("192.0.2.1", results.getReverseDns().getIp());
  }

  // ==================== Field Tests ====================

  @Test
  void testSpfFields() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getSpf());
    assertEquals("pass", results.getSpf().getResult());
    assertEquals("192.0.2.1", results.getSpf().getIp());
    assertEquals("example.com", results.getSpf().getDomain());
    assertEquals("sender SPF authorized", results.getSpf().getDetails());
  }

  @Test
  void testDkimFields() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getDkim());
    assertFalse(results.getDkim().isEmpty());
    assertEquals("pass", results.getDkim().get(0).getResult());
    assertEquals("example.com", results.getDkim().get(0).getDomain());
    assertEquals("default", results.getDkim().get(0).getSelector());
    assertEquals("dkim=pass (test)", results.getDkim().get(0).getSignature());
  }

  @Test
  void testDmarcFields() {
    AuthResults results = TestHelpers.createPassingAuthResults();

    assertNotNull(results.getDmarc());
    assertEquals("pass", results.getDmarc().getResult());
    assertEquals("reject", results.getDmarc().getPolicy());
    assertTrue(results.getDmarc().isAligned());
    assertEquals(Boolean.TRUE, results.getDmarc().getAligned());
    assertEquals("example.com", results.getDmarc().getDomain());
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
