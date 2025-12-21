package com.vaultsandbox.client.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import javax.crypto.KEM;
import javax.crypto.SecretKey;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KemIntegrationTest {

  private KeypairGenerator keypairGenerator;
  private Decryptor decryptor;

  @BeforeEach
  void setUp() {
    CryptoProvider.ensureInitialized();
    keypairGenerator = new KeypairGenerator();
    decryptor = new Decryptor();
  }

  @Test
  void testKemEncapsulateDecapsulate() throws Exception {
    // Generate keypair
    Keypair kp = keypairGenerator.generate();

    // Manually encapsulate using the public key
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768", "BC");
    KeyPair javaKp = kpg.generateKeyPair();

    // Use fresh keypair for encapsulation test
    KEM kem = KEM.getInstance("ML-KEM", "BC");
    KEM.Encapsulator encapsulator = kem.newEncapsulator(javaKp.getPublic());
    KEM.Encapsulated encapsulated = encapsulator.encapsulate();

    byte[] ctKem = encapsulated.encapsulation();
    SecretKey originalSecret = encapsulated.key();

    // Now decapsulate
    KEM.Decapsulator decapsulator = kem.newDecapsulator(javaKp.getPrivate());
    SecretKey recoveredSecret = decapsulator.decapsulate(ctKem);

    assertArrayEquals(
        originalSecret.getEncoded(),
        recoveredSecret.getEncoded(),
        "Decapsulated secret should match original");
  }

  @Test
  void testFullKemWithGeneratedKeypair() throws Exception {
    // Generate our keypair with raw keys
    Keypair kp = keypairGenerator.generate();

    // Use Bouncy Castle's low-level API for encapsulation with raw public key
    MLKEMPublicKeyParameters pubParams =
        new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, kp.getPublicKey());

    // Encapsulate using MLKEMGenerator
    MLKEMGenerator generator = new MLKEMGenerator(new SecureRandom());
    SecretWithEncapsulation encapsulated = generator.generateEncapsulated(pubParams);

    byte[] ctKem = encapsulated.getEncapsulation();
    byte[] originalSecret = encapsulated.getSecret();

    // Decapsulate using our decryptor
    byte[] recoveredSecret = decryptor.decapsulate(ctKem, kp.getSecretKey());

    assertArrayEquals(originalSecret, recoveredSecret, "Decapsulated secret should match original");
  }
}
