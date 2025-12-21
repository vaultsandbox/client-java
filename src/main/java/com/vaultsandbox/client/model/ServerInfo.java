package com.vaultsandbox.client.model;

import java.util.List;

public class ServerInfo {
  private String serverSigPk;
  private Algorithms algs;
  private String context;
  private int maxTtl;
  private int defaultTtl;
  private boolean sseConsole;
  private List<String> allowedDomains;

  // Legacy fields for backward compatibility
  private String version;
  private String domain;
  private Algorithms algorithms;
  private Limits limits;

  public ServerInfo() {}

  public String getServerSigPk() {
    return serverSigPk;
  }

  public String getContext() {
    return context;
  }

  public int getMaxTtl() {
    return maxTtl;
  }

  public int getDefaultTtl() {
    return defaultTtl;
  }

  public boolean isSseConsole() {
    return sseConsole;
  }

  public List<String> getAllowedDomains() {
    return allowedDomains;
  }

  public String getVersion() {
    return version;
  }

  public String getDomain() {
    return domain;
  }

  public Algorithms getAlgorithms() {
    // Return algs first (new API format), fall back to algorithms (old format)
    return algs != null ? algs : algorithms;
  }

  public Limits getLimits() {
    return limits;
  }

  public static class Limits {
    private int maxInboxes;
    private int maxEmailsPerInbox;
    private int inboxTtlSeconds;

    public Limits() {}

    public int getMaxInboxes() {
      return maxInboxes;
    }

    public int getMaxEmailsPerInbox() {
      return maxEmailsPerInbox;
    }

    public int getInboxTtlSeconds() {
      return inboxTtlSeconds;
    }
  }
}
