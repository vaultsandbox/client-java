package com.vaultsandbox.client.crypto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

public class HkdfDeriver {
  private static final String CONTEXT = "vaultsandbox:email:v1";
  private static final int AES_KEY_LENGTH = 32;

  public HkdfDeriver() {
    CryptoProvider.ensureInitialized();
  }

  public byte[] deriveKey(byte[] sharedSecret, byte[] aad, byte[] ctKem) {
    // Salt = SHA-256(ctKem)
    byte[] salt = sha256(ctKem);

    // Info = context || aad_length (4 bytes BE) || aad
    byte[] info = buildInfo(aad);

    // HKDF-SHA-512, 32 bytes output
    HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
    hkdf.init(new HKDFParameters(sharedSecret, salt, info));

    byte[] aesKey = new byte[AES_KEY_LENGTH];
    hkdf.generateBytes(aesKey, 0, AES_KEY_LENGTH);
    return aesKey;
  }

  private byte[] buildInfo(byte[] aad) {
    byte[] contextBytes = CONTEXT.getBytes(StandardCharsets.UTF_8);
    byte[] aadLength =
        ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(aad.length).array();

    return CryptoUtils.concat(contextBytes, aadLength, aad);
  }

  private byte[] sha256(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
