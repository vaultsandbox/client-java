package com.vaultsandbox.client.model;

import com.google.gson.annotations.SerializedName;

public class EncryptedPayload {
  private int v;
  private Algorithms algs;

  @SerializedName("ct_kem")
  private String ctKem;

  private String nonce;
  private String aad;
  private String ciphertext;
  private String sig;

  @SerializedName("server_sig_pk")
  private String serverSigPk;

  public EncryptedPayload() {}

  public int getV() {
    return v;
  }

  public Algorithms getAlgs() {
    return algs;
  }

  public String getCtKem() {
    return ctKem;
  }

  public String getNonce() {
    return nonce;
  }

  public String getAad() {
    return aad;
  }

  public String getCiphertext() {
    return ciphertext;
  }

  public String getSig() {
    return sig;
  }

  public String getServerSigPk() {
    return serverSigPk;
  }
}
