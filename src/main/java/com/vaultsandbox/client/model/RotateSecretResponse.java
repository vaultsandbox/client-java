package com.vaultsandbox.client.model;

/**
 * Response from rotating a webhook secret.
 *
 * @see com.vaultsandbox.client.Inbox#rotateWebhookSecret(String)
 */
public class RotateSecretResponse {
  private String id;
  private String secret;
  private String previousSecretValidUntil;

  public RotateSecretResponse() {}

  /**
   * Returns the webhook ID.
   *
   * @return the webhook ID
   */
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the new signing secret (whsec_ prefix).
   *
   * @return the new secret
   */
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Returns when the old secret expires (ISO 8601).
   *
   * <p>The old secret remains valid until this time (1 hour grace period).
   *
   * @return the expiration timestamp
   */
  public String getPreviousSecretValidUntil() {
    return previousSecretValidUntil;
  }

  public void setPreviousSecretValidUntil(String previousSecretValidUntil) {
    this.previousSecretValidUntil = previousSecretValidUntil;
  }
}
