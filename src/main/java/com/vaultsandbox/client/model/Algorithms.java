package com.vaultsandbox.client.model;

public class Algorithms {
  private String kem; // "ML-KEM-768"
  private String sig; // "ML-DSA-65"
  private String aead; // "AES-256-GCM"
  private String kdf; // "HKDF-SHA-512"

  public Algorithms() {}

  public Algorithms(String kem, String sig, String aead, String kdf) {
    this.kem = kem;
    this.sig = sig;
    this.aead = aead;
    this.kdf = kdf;
  }

  public String getKem() {
    return kem;
  }

  public String getSig() {
    return sig;
  }

  public String getAead() {
    return aead;
  }

  public String getKdf() {
    return kdf;
  }
}
