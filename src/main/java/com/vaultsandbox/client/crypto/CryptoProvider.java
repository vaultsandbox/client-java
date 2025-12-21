package com.vaultsandbox.client.crypto;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class CryptoProvider {
  private static volatile boolean initialized = false;

  private CryptoProvider() {}

  public static synchronized void ensureInitialized() {
    if (!initialized) {
      // BC 1.79+ includes ML-KEM/ML-DSA in the main provider
      Security.addProvider(new BouncyCastleProvider());
      initialized = true;
    }
  }
}
