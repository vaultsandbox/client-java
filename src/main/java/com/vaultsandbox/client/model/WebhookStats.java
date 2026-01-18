package com.vaultsandbox.client.model;

/**
 * Delivery statistics for a webhook.
 *
 * @see WebhookData
 */
public class WebhookStats {
  private int totalDeliveries;
  private int successfulDeliveries;
  private int failedDeliveries;

  public WebhookStats() {}

  public int getTotalDeliveries() {
    return totalDeliveries;
  }

  public void setTotalDeliveries(int totalDeliveries) {
    this.totalDeliveries = totalDeliveries;
  }

  public int getSuccessfulDeliveries() {
    return successfulDeliveries;
  }

  public void setSuccessfulDeliveries(int successfulDeliveries) {
    this.successfulDeliveries = successfulDeliveries;
  }

  public int getFailedDeliveries() {
    return failedDeliveries;
  }

  public void setFailedDeliveries(int failedDeliveries) {
    this.failedDeliveries = failedDeliveries;
  }

  /**
   * Calculates the success rate as a percentage.
   *
   * @return the success rate (0-100), or 0 if no deliveries
   */
  public double getSuccessRate() {
    if (totalDeliveries == 0) {
      return 0;
    }
    return (double) successfulDeliveries / totalDeliveries * 100;
  }
}
