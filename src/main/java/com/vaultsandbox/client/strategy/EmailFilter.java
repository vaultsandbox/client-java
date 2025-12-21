package com.vaultsandbox.client.strategy;

import com.vaultsandbox.client.Email;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Filter for matching emails based on various criteria. */
public final class EmailFilter {
  private final String subject;
  private final Pattern subjectPattern;
  private final String from;
  private final Pattern fromPattern;
  private final Predicate<Email> predicate;

  private EmailFilter(Builder builder) {
    this.subject = builder.subject;
    this.subjectPattern = builder.subjectPattern;
    this.from = builder.from;
    this.fromPattern = builder.fromPattern;
    this.predicate = builder.predicate;
  }

  /** Create a new filter builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Create a filter that matches any email. */
  public static EmailFilter any() {
    return new Builder().build();
  }

  // ==================== Static Factory Methods ====================

  /**
   * Create a filter that matches emails where the subject contains the given substring.
   *
   * @param substring the substring to search for in the subject
   * @return a new filter
   */
  public static EmailFilter subjectContains(String substring) {
    return new Builder().subject(substring).build();
  }

  /**
   * Create a filter that matches emails where the subject matches the given regex pattern.
   *
   * @param pattern the regex pattern to match against the subject
   * @return a new filter
   */
  public static EmailFilter subjectMatches(Pattern pattern) {
    return new Builder().subjectMatches(pattern).build();
  }

  /**
   * Create a filter that matches emails where the from address contains the given substring.
   *
   * @param substring the substring to search for in the from address
   * @return a new filter
   */
  public static EmailFilter from(String substring) {
    return new Builder().from(substring).build();
  }

  /**
   * Create a filter that matches emails where the from address matches the given regex pattern.
   *
   * @param pattern the regex pattern to match against the from address
   * @return a new filter
   */
  public static EmailFilter fromMatches(Pattern pattern) {
    return new Builder().fromMatches(pattern).build();
  }

  /**
   * Create a filter using a custom predicate.
   *
   * @param predicate the predicate to test emails against
   * @return a new filter
   */
  public static EmailFilter matching(Predicate<Email> predicate) {
    return new Builder().where(predicate).build();
  }

  /**
   * Combine this filter with another using AND logic.
   *
   * @param other the other filter to combine with
   * @return a new filter that matches only if both filters match
   */
  public EmailFilter and(EmailFilter other) {
    return new Builder().where(email -> this.matches(email) && other.matches(email)).build();
  }

  /**
   * Check if an email matches this filter.
   *
   * @param email the email to check
   * @return true if the email matches all filter criteria
   */
  public boolean matches(Email email) {
    if (subject != null && (email.getSubject() == null || !email.getSubject().contains(subject))) {
      return false;
    }
    if (subjectPattern != null
        && (email.getSubject() == null || !subjectPattern.matcher(email.getSubject()).find())) {
      return false;
    }
    if (from != null && (email.getFrom() == null || !email.getFrom().contains(from))) {
      return false;
    }
    if (fromPattern != null
        && (email.getFrom() == null || !fromPattern.matcher(email.getFrom()).find())) {
      return false;
    }
    if (predicate != null && !predicate.test(email)) {
      return false;
    }
    return true;
  }

  /** Builder for creating email filters. */
  public static class Builder {
    private String subject;
    private Pattern subjectPattern;
    private String from;
    private Pattern fromPattern;
    private Predicate<Email> predicate;

    /** Filter emails where subject contains the given substring. */
    public Builder subject(String substring) {
      this.subject = substring;
      return this;
    }

    /** Filter emails where subject matches the given regex. */
    public Builder subjectMatches(String regex) {
      this.subjectPattern = Pattern.compile(regex);
      return this;
    }

    /** Filter emails where subject matches the given pattern. */
    public Builder subjectMatches(Pattern pattern) {
      this.subjectPattern = pattern;
      return this;
    }

    /** Filter emails where from address contains the given substring. */
    public Builder from(String substring) {
      this.from = substring;
      return this;
    }

    /** Filter emails where from address matches the given regex. */
    public Builder fromMatches(String regex) {
      this.fromPattern = Pattern.compile(regex);
      return this;
    }

    /** Filter emails where from address matches the given pattern. */
    public Builder fromMatches(Pattern pattern) {
      this.fromPattern = pattern;
      return this;
    }

    /** Filter emails using a custom predicate. */
    public Builder where(Predicate<Email> predicate) {
      this.predicate = predicate;
      return this;
    }

    /** Build the email filter. */
    public EmailFilter build() {
      return new EmailFilter(this);
    }
  }
}
