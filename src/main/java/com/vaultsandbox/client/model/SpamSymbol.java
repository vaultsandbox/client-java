package com.vaultsandbox.client.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents an individual spam rule/symbol that was triggered during spam analysis.
 *
 * <p>Each symbol represents a specific spam indicator detected by Rspamd. Symbols have scores that
 * contribute to the overall spam score:
 *
 * <ul>
 *   <li>Positive scores indicate spam indicators (e.g., {@code FORGED_SENDER})
 *   <li>Negative scores indicate legitimacy/ham indicators (e.g., {@code DKIM_SIGNED})
 *   <li>Zero scores are informational
 * </ul>
 *
 * <p>Common symbol examples:
 *
 * <table>
 * <tr><th>Symbol</th><th>Typical Score</th><th>Meaning</th></tr>
 * <tr><td>{@code DKIM_SIGNED}</td><td>-0.1</td><td>Email has valid DKIM signature</td></tr>
 * <tr><td>{@code SPF_ALLOW}</td><td>-0.2</td><td>SPF check passed</td></tr>
 * <tr><td>{@code FORGED_SENDER}</td><td>+3.0</td><td>Sender address appears forged</td></tr>
 * <tr><td>{@code RCVD_IN_SBL}</td><td>+6.5</td><td>Sender in Spamhaus blocklist</td></tr>
 * </table>
 *
 * @see SpamAnalysisResult
 */
public class SpamSymbol {
  private String name;
  private double score;
  private String description;
  private List<String> options;

  public SpamSymbol() {}

  /**
   * Returns the rule identifier.
   *
   * <p>Examples: {@code DKIM_SIGNED}, {@code FORGED_SENDER}, {@code SPF_ALLOW}
   *
   * @return the symbol name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the score contribution from this rule.
   *
   * <p>Positive scores increase the spam score (spam indicators). Negative scores decrease the spam
   * score (legitimacy indicators).
   *
   * @return the score contribution
   */
  public double getScore() {
    return score;
  }

  /**
   * Returns a human-readable description of what this rule detects.
   *
   * @return the description, or {@code null} if not available
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns additional context or matched values.
   *
   * <p>For example, URL-based rules may include the matched URLs in this list.
   *
   * @return a list of options, or empty list if none
   */
  public List<String> getOptions() {
    return options != null ? options : Collections.emptyList();
  }
}
