package com.vaultsandbox.client.model;

/**
 * A single filter rule for webhook filtering.
 *
 * @see FilterConfig
 */
public class FilterRule {
  private FilterableField field;
  private FilterOperator operator;
  private String value;
  private Boolean caseSensitive;

  public FilterRule() {}

  /**
   * Creates a new filter rule.
   *
   * @param field the field to filter on
   * @param operator the comparison operator
   * @param value the value to match (max 1000 chars)
   */
  public FilterRule(FilterableField field, FilterOperator operator, String value) {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }

  /**
   * Creates a new filter rule with case sensitivity option.
   *
   * @param field the field to filter on
   * @param operator the comparison operator
   * @param value the value to match (max 1000 chars)
   * @param caseSensitive whether matching is case-sensitive (default: false)
   */
  public FilterRule(
      FilterableField field, FilterOperator operator, String value, boolean caseSensitive) {
    this.field = field;
    this.operator = operator;
    this.value = value;
    this.caseSensitive = caseSensitive;
  }

  public FilterableField getField() {
    return field;
  }

  public void setField(FilterableField field) {
    this.field = field;
  }

  public FilterOperator getOperator() {
    return operator;
  }

  public void setOperator(FilterOperator operator) {
    this.operator = operator;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Boolean getCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(Boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * Returns whether matching is case-sensitive.
   *
   * @return true if case-sensitive, false otherwise (default)
   */
  public boolean isCaseSensitive() {
    return caseSensitive != null && caseSensitive;
  }
}
