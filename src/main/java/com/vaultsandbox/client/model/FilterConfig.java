package com.vaultsandbox.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for webhook event filtering.
 *
 * @see WebhookData
 */
public class FilterConfig {
  private List<FilterRule> rules;
  private FilterMode mode;
  private Boolean requireAuth;

  public FilterConfig() {
    this.rules = new ArrayList<>();
  }

  /**
   * Creates a filter configuration.
   *
   * @param rules the filter rules (max 10)
   * @param mode 'all' = AND logic, 'any' = OR logic
   */
  public FilterConfig(List<FilterRule> rules, FilterMode mode) {
    this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    this.mode = mode;
  }

  public List<FilterRule> getRules() {
    return rules;
  }

  public void setRules(List<FilterRule> rules) {
    this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
  }

  public FilterMode getMode() {
    return mode;
  }

  public void setMode(FilterMode mode) {
    this.mode = mode;
  }

  public Boolean getRequireAuth() {
    return requireAuth;
  }

  public void setRequireAuth(Boolean requireAuth) {
    this.requireAuth = requireAuth;
  }

  /**
   * Returns whether email authentication is required.
   *
   * @return true if emails must pass SPF/DKIM/DMARC checks
   */
  public boolean isRequireAuth() {
    return requireAuth != null && requireAuth;
  }

  /** Builder for creating FilterConfig instances. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for FilterConfig. */
  public static class Builder {
    private final List<FilterRule> rules = new ArrayList<>();
    private FilterMode mode = FilterMode.ALL;
    private Boolean requireAuth;

    /**
     * Adds a filter rule.
     *
     * @param rule the rule to add
     * @return this builder
     */
    public Builder addRule(FilterRule rule) {
      this.rules.add(rule);
      return this;
    }

    /**
     * Adds a filter rule.
     *
     * @param field the field to filter on
     * @param operator the comparison operator
     * @param value the value to match
     * @return this builder
     */
    public Builder addRule(FilterableField field, FilterOperator operator, String value) {
      this.rules.add(new FilterRule(field, operator, value));
      return this;
    }

    /**
     * Sets the filter mode.
     *
     * @param mode 'all' = AND logic, 'any' = OR logic
     * @return this builder
     */
    public Builder mode(FilterMode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * Sets whether email authentication is required.
     *
     * @param requireAuth true to require SPF/DKIM/DMARC pass
     * @return this builder
     */
    public Builder requireAuth(boolean requireAuth) {
      this.requireAuth = requireAuth;
      return this;
    }

    /**
     * Builds the filter configuration.
     *
     * @return the filter configuration
     */
    public FilterConfig build() {
      FilterConfig config = new FilterConfig(rules, mode);
      config.setRequireAuth(requireAuth);
      return config;
    }
  }
}
