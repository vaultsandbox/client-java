package com.vaultsandbox.client.model;

/**
 * Response from testing a webhook.
 *
 * @see com.vaultsandbox.client.Inbox#testWebhook(String)
 */
public class TestWebhookResponse {
  private boolean success;
  private Integer statusCode;
  private Long responseTime;
  private String responseBody;
  private String error;
  private Object payloadSent;

  public TestWebhookResponse() {}

  /**
   * Returns whether the test succeeded.
   *
   * @return true if the webhook endpoint responded with 2xx
   */
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Returns the HTTP status code from the endpoint.
   *
   * @return the status code, or null if connection failed
   */
  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Returns the response time in milliseconds.
   *
   * @return the response time, or null if connection failed
   */
  public Long getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(Long responseTime) {
    this.responseTime = responseTime;
  }

  /**
   * Returns the response body (truncated to 1KB).
   *
   * @return the response body, or null if connection failed
   */
  public String getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  /**
   * Returns the error message if the test failed.
   *
   * @return the error message, or null if successful
   */
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  /**
   * Returns the test payload that was sent.
   *
   * @return the payload, or null if not included
   */
  public Object getPayloadSent() {
    return payloadSent;
  }

  public void setPayloadSent(Object payloadSent) {
    this.payloadSent = payloadSent;
  }
}
