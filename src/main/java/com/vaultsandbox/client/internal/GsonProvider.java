package com.vaultsandbox.client.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Provides a shared Gson instance for JSON serialization/deserialization.
 *
 * <p>Gson is thread-safe and should be shared across the application rather than creating multiple
 * instances. This class provides a single shared instance configured for VaultSandbox use cases.
 */
public final class GsonProvider {

  private static final Gson INSTANCE = new GsonBuilder().create();

  private GsonProvider() {} // Prevent instantiation

  /**
   * Returns the shared Gson instance.
   *
   * @return the shared Gson instance
   */
  public static Gson get() {
    return INSTANCE;
  }
}
