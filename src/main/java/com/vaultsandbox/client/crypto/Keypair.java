package com.vaultsandbox.client.crypto;

import java.util.Arrays;

public class Keypair {
  private final byte[] publicKey;
  private final byte[] secretKey;

  public Keypair(byte[] publicKey, byte[] secretKey) {
    this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
    this.secretKey = Arrays.copyOf(secretKey, secretKey.length);
  }

  public byte[] getPublicKey() {
    return Arrays.copyOf(publicKey, publicKey.length);
  }

  public byte[] getSecretKey() {
    return Arrays.copyOf(secretKey, secretKey.length);
  }

  public String getPublicKeyBase64Url() {
    return Base64Url.encode(publicKey);
  }

  public String getSecretKeyBase64Url() {
    return Base64Url.encode(secretKey);
  }
}
