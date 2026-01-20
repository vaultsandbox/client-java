package com.vaultsandbox.client.model;

import java.util.Collections;
import java.util.List;

/**
 * Result of spam analysis for an email.
 *
 * <p>When spam analysis is enabled on the server and for the inbox, incoming emails are analyzed by
 * Rspamd. This class contains the analysis results including the spam score, recommended action,
 * and individual rules that were triggered.
 *
 * <p>The {@link #getStatus()} field indicates whether analysis was performed:
 *
 * <ul>
 *   <li>{@link SpamAnalysisStatus#ANALYZED} - Analysis completed, all fields available
 *   <li>{@link SpamAnalysisStatus#SKIPPED} - Analysis skipped, only {@link #getInfo()} available
 *   <li>{@link SpamAnalysisStatus#ERROR} - Analysis failed, check {@link #getInfo()} for details
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Email email = inbox.waitForEmail();
 * SpamAnalysisResult spam = email.getSpamAnalysis();
 *
 * if (spam != null && spam.getStatus() == SpamAnalysisStatus.ANALYZED) {
 *     System.out.println("Spam score: " + spam.getScore());
 *     System.out.println("Is spam: " + spam.isSpam());
 *
 *     // Display triggered rules
 *     for (SpamSymbol symbol : spam.getSymbols()) {
 *         System.out.printf("  %s: %.1f - %s%n",
 *             symbol.getName(), symbol.getScore(), symbol.getDescription());
 *     }
 * }
 * }</pre>
 *
 * @see SpamSymbol
 * @see SpamAction
 * @see SpamAnalysisStatus
 */
public class SpamAnalysisResult {
  private SpamAnalysisStatus status;
  private Double score;
  private Double requiredScore;
  private SpamAction action;
  private Boolean isSpam;
  private List<SpamSymbol> symbols;
  private Long processingTimeMs;
  private String info;

  public SpamAnalysisResult() {}

  /**
   * Returns the analysis status.
   *
   * @return the status indicating whether analysis was performed
   */
  public SpamAnalysisStatus getStatus() {
    return status;
  }

  /**
   * Returns the overall spam score.
   *
   * <p>Positive scores indicate more spammy content. Typical range is -10 to +15, but can vary.
   *
   * <p>Only present when status is {@link SpamAnalysisStatus#ANALYZED}.
   *
   * @return the spam score, or {@code null} if not analyzed
   */
  public Double getScore() {
    return score;
  }

  /**
   * Returns the required score threshold for spam classification.
   *
   * <p>Emails with score >= requiredScore are considered spam. The default Rspamd threshold is
   * typically 6.0.
   *
   * @return the threshold score, or {@code null} if not analyzed
   */
  public Double getRequiredScore() {
    return requiredScore;
  }

  /**
   * Returns the recommended action based on score thresholds.
   *
   * @return the recommended action, or {@code null} if not analyzed
   * @see SpamAction
   */
  public SpamAction getAction() {
    return action;
  }

  /**
   * Returns whether the email is classified as spam.
   *
   * <p>This is {@code true} when score >= requiredScore.
   *
   * @return {@code true} if spam, {@code false} if not spam, or {@code null} if not analyzed
   */
  public Boolean isSpam() {
    return isSpam;
  }

  /**
   * Returns the list of triggered spam rules/symbols.
   *
   * <p>Each symbol represents a specific spam indicator that was detected, along with its score
   * contribution.
   *
   * @return list of triggered symbols, or empty list if none or not analyzed
   * @see SpamSymbol
   */
  public List<SpamSymbol> getSymbols() {
    return symbols != null ? symbols : Collections.emptyList();
  }

  /**
   * Returns the time taken for spam analysis in milliseconds.
   *
   * <p>Useful for performance monitoring.
   *
   * @return processing time in milliseconds, or {@code null} if not available
   */
  public Long getProcessingTimeMs() {
    return processingTimeMs;
  }

  /**
   * Returns additional information about the analysis.
   *
   * <p>Contains error messages when status is {@link SpamAnalysisStatus#ERROR}, or skip reason when
   * status is {@link SpamAnalysisStatus#SKIPPED}.
   *
   * @return informational message, or {@code null} if not applicable
   */
  public String getInfo() {
    return info;
  }

  /**
   * Convenience method to check if analysis was successfully completed.
   *
   * @return {@code true} if status is {@link SpamAnalysisStatus#ANALYZED}
   */
  public boolean isAnalyzed() {
    return status == SpamAnalysisStatus.ANALYZED;
  }

  /**
   * Returns the symbols that contributed positively to the spam score (spam indicators).
   *
   * <p>These are symbols with score > 0, sorted by score descending.
   *
   * @return list of spam indicator symbols
   */
  public List<SpamSymbol> getSpamIndicators() {
    if (symbols == null) {
      return Collections.emptyList();
    }
    return symbols.stream()
        .filter(s -> s.getScore() > 0)
        .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
        .toList();
  }

  /**
   * Returns the symbols that contributed negatively to the spam score (legitimacy indicators).
   *
   * <p>These are symbols with score < 0, sorted by score ascending (most negative first).
   *
   * @return list of legitimacy indicator symbols
   */
  public List<SpamSymbol> getLegitimacyIndicators() {
    if (symbols == null) {
      return Collections.emptyList();
    }
    return symbols.stream()
        .filter(s -> s.getScore() < 0)
        .sorted((a, b) -> Double.compare(a.getScore(), b.getScore()))
        .toList();
  }
}
