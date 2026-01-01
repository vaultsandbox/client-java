package com.vaultsandbox.client.model;

import java.util.ArrayList;
import java.util.List;

public class AuthResults {
  private SpfResult spf;
  private List<DkimResult> dkim;
  private DmarcResult dmarc;
  private ReverseDnsResult reverseDns;

  public AuthResults() {}

  public AuthValidation validate() {
    List<String> passed = new ArrayList<>();
    List<String> failed = new ArrayList<>();

    // SPF - pass is success, fail is failure, softfail/neutral/none/temperror/permerror are neutral
    if (spf != null) {
      String status = spf.getResult();
      if ("pass".equalsIgnoreCase(status)) {
        passed.add("SPF");
      } else if ("fail".equalsIgnoreCase(status)) {
        failed.add("SPF: " + status);
      }
      // softfail, neutral, none, temperror, permerror are treated as neutral (not added to either
      // list)
    }

    // DKIM (any passing signature counts) - none is neutral
    if (dkim != null && !dkim.isEmpty()) {
      boolean anyPass = dkim.stream().anyMatch(d -> "pass".equalsIgnoreCase(d.getResult()));
      boolean anyFail = dkim.stream().anyMatch(d -> "fail".equalsIgnoreCase(d.getResult()));
      if (anyPass) {
        passed.add("DKIM");
      } else if (anyFail) {
        failed.add("DKIM: fail");
      }
      // If all are 'none', treat as neutral (not added to either list)
    }

    // DMARC - pass is success, fail is failure, none is neutral
    if (dmarc != null) {
      String status = dmarc.getResult();
      if ("pass".equalsIgnoreCase(status)) {
        passed.add("DMARC");
      } else if ("fail".equalsIgnoreCase(status)) {
        failed.add("DMARC: " + status);
      }
      // 'none' is treated as neutral (not added to either list)
    }

    // Reverse DNS - uses verified boolean
    if (reverseDns != null) {
      if (reverseDns.isVerified()) {
        passed.add("ReverseDNS");
      } else {
        failed.add("ReverseDNS: not verified");
      }
    }

    return new AuthValidation(passed, failed);
  }

  public SpfResult getSpf() {
    return spf;
  }

  public List<DkimResult> getDkim() {
    return dkim;
  }

  public DmarcResult getDmarc() {
    return dmarc;
  }

  public ReverseDnsResult getReverseDns() {
    return reverseDns;
  }
}
