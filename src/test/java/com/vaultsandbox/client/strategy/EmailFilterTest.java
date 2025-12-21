package com.vaultsandbox.client.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaultsandbox.client.Email;
import com.vaultsandbox.client.TestHelpers;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailFilterTest {

  private Email testEmail;

  @BeforeEach
  void setUp() {
    testEmail = TestHelpers.createTestEmail("sender@example.com", "Welcome to our service");
  }

  // ==================== Subject Filter Tests ====================

  @Test
  void testSubjectContains() {
    EmailFilter filter = EmailFilter.builder().subject("Welcome").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testSubjectContainsCaseInsensitive() {
    // Note: The current implementation is case-sensitive
    EmailFilter filter = EmailFilter.builder().subject("welcome").build();

    // This test verifies case-sensitivity behavior
    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testSubjectNotMatching() {
    EmailFilter filter = EmailFilter.builder().subject("Goodbye").build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testSubjectContainsPartialMatch() {
    EmailFilter filter = EmailFilter.builder().subject("to our").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testSubjectRegex() {
    EmailFilter filter = EmailFilter.builder().subjectMatches("Welcome.*service").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testSubjectRegexNoMatch() {
    EmailFilter filter = EmailFilter.builder().subjectMatches("Goodbye.*service").build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testSubjectRegexPattern() {
    Pattern pattern = Pattern.compile("Welcome.*service", Pattern.CASE_INSENSITIVE);
    EmailFilter filter = EmailFilter.builder().subjectMatches(pattern).build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testSubjectRegexCaseInsensitive() {
    EmailFilter filter = EmailFilter.builder().subjectMatches("(?i)welcome.*SERVICE").build();

    assertTrue(filter.matches(testEmail));
  }

  // ==================== From Filter Tests ====================

  @Test
  void testFromContains() {
    EmailFilter filter = EmailFilter.builder().from("example.com").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testFromContainsUsername() {
    EmailFilter filter = EmailFilter.builder().from("sender").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testFromNotMatching() {
    EmailFilter filter = EmailFilter.builder().from("different.com").build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testFromRegex() {
    EmailFilter filter = EmailFilter.builder().fromMatches("sender@.*\\.com").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testFromRegexNoMatch() {
    EmailFilter filter = EmailFilter.builder().fromMatches("admin@.*\\.com").build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testFromRegexPattern() {
    Pattern pattern = Pattern.compile(".*@example\\.com");
    EmailFilter filter = EmailFilter.builder().fromMatches(pattern).build();

    assertTrue(filter.matches(testEmail));
  }

  // ==================== Custom Predicate Tests ====================

  @Test
  void testCustomPredicate() {
    EmailFilter filter = EmailFilter.builder().where(e -> e.getSubject().length() > 10).build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testCustomPredicateNoMatch() {
    EmailFilter filter = EmailFilter.builder().where(e -> e.getSubject().length() > 100).build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testCustomPredicateCheckingFrom() {
    EmailFilter filter = EmailFilter.builder().where(e -> e.getFrom().endsWith(".com")).build();

    assertTrue(filter.matches(testEmail));
  }

  // ==================== Combined Filter Tests ====================

  @Test
  void testCombinedSubjectAndFrom() {
    EmailFilter filter = EmailFilter.builder().subject("Welcome").from("sender").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testCombinedFiltersAllMustMatch() {
    EmailFilter filter = EmailFilter.builder().subject("Welcome").from("other@domain.com").build();

    assertFalse(filter.matches(testEmail));
  }

  @Test
  void testCombinedSubjectRegexAndFrom() {
    EmailFilter filter =
        EmailFilter.builder().subjectMatches("Welcome.*").from("example.com").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testCombinedAllFilters() {
    EmailFilter filter =
        EmailFilter.builder()
            .subject("Welcome")
            .subjectMatches(".*service$")
            .from("sender")
            .fromMatches(".*@example\\.com")
            .where(e -> !e.isRead())
            .build();

    assertTrue(filter.matches(testEmail));
  }

  // ==================== Any Filter Tests ====================

  @Test
  void testAnyFilter() {
    EmailFilter filter = EmailFilter.any();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testAnyFilterMatchesAnyEmail() {
    EmailFilter filter = EmailFilter.any();

    Email anotherEmail =
        TestHelpers.createTestEmail("other@domain.org", "Different subject entirely");

    assertTrue(filter.matches(testEmail));
    assertTrue(filter.matches(anotherEmail));
  }

  // ==================== Empty Builder Tests ====================

  @Test
  void testEmptyBuilderMatchesAll() {
    EmailFilter filter = EmailFilter.builder().build();

    assertTrue(filter.matches(testEmail));
  }

  // ==================== Null Handling Tests ====================

  @Test
  void testSubjectFilterWithNullSubject() {
    Email emailWithNullSubject = TestHelpers.createTestEmail("sender@example.com", null);

    EmailFilter filter = EmailFilter.builder().subject("Test").build();

    // Should handle null subject gracefully
    assertFalse(filter.matches(emailWithNullSubject));
  }

  @Test
  void testSubjectRegexWithNullSubject() {
    Email emailWithNullSubject = TestHelpers.createTestEmail("sender@example.com", null);

    EmailFilter filter = EmailFilter.builder().subjectMatches("Test.*").build();

    assertFalse(filter.matches(emailWithNullSubject));
  }

  @Test
  void testFromFilterWithNullFrom() {
    // Create email with minimal data - from might be null in edge cases
    Email emailWithNullFrom = TestHelpers.createTestEmail(null, "Test Subject");

    EmailFilter filter = EmailFilter.builder().from("example").build();

    assertFalse(filter.matches(emailWithNullFrom));
  }

  @Test
  void testFromRegexWithNullFrom() {
    Email emailWithNullFrom = TestHelpers.createTestEmail(null, "Test Subject");

    EmailFilter filter = EmailFilter.builder().fromMatches(".*@example\\.com").build();

    assertFalse(filter.matches(emailWithNullFrom));
  }

  // ==================== Edge Case Tests ====================

  @Test
  void testEmptySubjectFilter() {
    EmailFilter filter = EmailFilter.builder().subject("").build();

    // Empty string is contained in any string
    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testEmptyFromFilter() {
    EmailFilter filter = EmailFilter.builder().from("").build();

    assertTrue(filter.matches(testEmail));
  }

  @Test
  void testRegexSpecialCharacters() {
    Email specialEmail = TestHelpers.createTestEmail("sender@example.com", "Price: $100.00");

    // Must escape special regex characters
    EmailFilter filter = EmailFilter.builder().subjectMatches("Price: \\$100\\.00").build();

    assertTrue(filter.matches(specialEmail));
  }
}
