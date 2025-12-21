package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class AuthResults {
  private SpfResult spf;
  private List<DkimResult> dkim;
  private DmarcResult dmarc;

  @SerializedName("reverse_dns")
  private ReverseDnsResult reverseDns;

  public AuthResults() {}

  public AuthValidation validate() {
    List<String> passed = new ArrayList<>();
    List<String> failed = new ArrayList<>();

    // SPF
    if (spf != null) {
      if ("pass".equalsIgnoreCase(spf.getResult())) {
        passed.add("SPF");
      } else {
        failed.add("SPF: " + spf.getResult());
      }
    }

    // DKIM (any passing signature counts)
    if (dkim != null && !dkim.isEmpty()) {
      boolean anyPass = dkim.stream().anyMatch(d -> "pass".equalsIgnoreCase(d.getResult()));
      if (anyPass) {
        passed.add("DKIM");
      } else {
        failed.add("DKIM: " + dkim.get(0).getResult());
      }
    }

    // DMARC
    if (dmarc != null) {
      if ("pass".equalsIgnoreCase(dmarc.getResult())) {
        passed.add("DMARC");
      } else {
        failed.add("DMARC: " + dmarc.getResult());
      }
    }

    // Reverse DNS
    if (reverseDns != null) {
      if (reverseDns.isValid()) {
        passed.add("ReverseDNS");
      } else {
        failed.add("ReverseDNS: invalid");
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
