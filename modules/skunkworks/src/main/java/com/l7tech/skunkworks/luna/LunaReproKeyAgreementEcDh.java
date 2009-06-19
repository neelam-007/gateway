package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

/**
 * Simulates relevant portion of client side of TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA handshake.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class LunaReproKeyAgreementEcDh {
    public static void main(String[] args) throws Exception {
        Security.insertProviderAt(new LunaJCEProvider(), 1);
        Security.insertProviderAt(new LunaJCAProvider(), 1);
        LunaTokenManager.getInstance().Login("FGAA-3LJT-tsHW-NC3E");

        // This happens on the server; the server then sends the generated ephemeral keypair to the client
        // in the ECDH server key exchange message.
        KeyPairGenerator serverKpg = KeyPairGenerator.getInstance("EC");
        serverKpg.initialize(new ECGenParameterSpec("1.2.840.10045.3.1.7")); // secp256r1
        KeyPair serverKeyPair = serverKpg.generateKeyPair();

        // On receipt of ServerKeyExchange
        ECPublicKey serverPublicKey = (ECPublicKey)serverKeyPair.getPublic();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        ECParameterSpec params = serverPublicKey.getParams();
        kpg.initialize(params, new SecureRandom());
        KeyPair clientKeyPair = kpg.generateKeyPair();
        PrivateKey privateKey = clientKeyPair.getPrivate();

        // On receipt of ServerHelloDone
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(privateKey);
        ka.doPhase(serverPublicKey, true);
        SecretKey premasterSecret = ka.generateSecret("TlsPremasterSecret");

        // Client now computes Master Secret from premaster secret bytes, TLS version, and client and server randoms
        // SunJSSE does this with code similar to the following:
        /*
        AlgorithmParameterSpec masterSpec =
                new TlsMasterSecretParameterSpec(premasterSecret, 3, 1, new byte[32], new byte[32]);
        KeyGenerator kg = KeyGenerator.getInstance("SunTlsMasterSecret");
        kg.init(masterSpec); // fails here if premasterSecret came from LunaKeyAgreementEcDh (see stack trace below)
        SecretKey masterSecret = kg.generateKey();
        */

        /*
            Stack trace when initializing the SunTlsMasterSecret KeyGenerator:

Exception in thread "main" java.security.InvalidAlgorithmParameterException: Key format must be RAW
	at com.sun.crypto.provider.TlsMasterSecretGenerator.engineInit(DashoA13*..)
	at javax.crypto.KeyGenerator.init(DashoA13*..)
	at javax.crypto.KeyGenerator.init(DashoA13*..)
	at com.l7tech.skunkworks.luna.LunaReproKeyAgreementEcDh.main(LunaReproKeyAgreementEcDh.java:51)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:90)
	    */

        /*
         SunJSSE includes its own implementation of KeyGenerator.SunTlsMasterSecret which requires
         that the premasterSecret's encoding be RAW as it needs to call premasterSecret.getEncoded()
         to get the raw bytes to pass into the TLS pseudo-random function (which for TLS 1.0 is a combination
         of both SHA-1 and MD5).

         This will not work if the premaster secret is a SecretKey with the format "proprietary" that does
         not provide access to the raw key bytes.

         Best possible solution:

          KeyAgreement.ECDH (and KeyAgreement.DiffieHellman) could return a SecretKeySpec instance (or equivalent)
          instead of a LunaSecretKey instance when the algorithm is "TlsPremasterSecret" (or is unrecognized).
          The algorithm of the SecretKeySpec should match the requested algorithm (rather than "GenericSecret")
         */
    }
}