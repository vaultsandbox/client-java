package com.vaultsandbox.client;

/**
 * Email delivery strategy for receiving emails.
 *
 * <p>Determines how the client receives notifications about new emails.
 *
 * @see ClientConfig.Builder#strategy(StrategyType)
 */
public enum StrategyType {
  /**
   * Server-Sent Events strategy.
   *
   * <p>Uses a persistent HTTP connection for real-time email notifications. Most efficient for
   * receiving emails with minimal latency.
   */
  SSE,

  /**
   * Polling strategy.
   *
   * <p>Periodically polls the server for new emails. Use this when SSE is not available or blocked
   * by a proxy/firewall.
   */
  POLLING,

  /**
   * Automatic strategy selection (default).
   *
   * <p>Attempts to use SSE first, falling back to polling if SSE is not available or fails.
   */
  AUTO
}
