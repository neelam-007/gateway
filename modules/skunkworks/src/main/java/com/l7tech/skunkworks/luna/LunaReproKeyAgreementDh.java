package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;
import sun.security.internal.spec.TlsMasterSecretParameterSpec;

import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

/**
 *
 */
public class LunaReproKeyAgreementDh {
    public static void main(String[] args) throws Exception {
        Security.insertProviderAt(new LunaJCEProvider(), 1);
        Security.insertProviderAt(new LunaJCAProvider(), 1);
        LunaTokenManager.getInstance().Login("FGAA-3LJT-tsHW-NC3E");

        // Simulate relevant portion of client side of TLS_DHE_RSA_WITH_AES_256_CBC_SHA handshake
        BigInteger peerPublicValue =
                new BigInteger("401529659108852617627849015107559631987751737825726293237728995470130840825955710650572431514039812225136577451798594879738721808561417090425752705436216299785199168592980289976224892348623319086783407467245256214968762360016551852");
        BigInteger base =
                new BigInteger("292780102671262920733869914750196906070339539436403467286169475770103366229486393481167074580815754315242306181493165983732855378657608308358389962076024024370672319480776517906088402587602106408859465786897237798036043145069892178");
        BigInteger modulus =
                new BigInteger("1418488780399624169246918906980830188668962659968489177172519612007411971965075884911751185624649475197807409457369163882960326663412481439463507475025544888587052733646843233033458377686354235239579046252542291754237282749312023983");

        // On receipt of ServerKeyExchange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
        DHParameterSpec params = new DHParameterSpec(modulus, base);
        kpg.initialize(params, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey privateKey = kp.getPrivate();

        // On receipt of ServerHelloDone
        KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
        DHPublicKeySpec spec =
                    new DHPublicKeySpec(peerPublicValue, modulus, base);
        PublicKey publicKey = kf.generatePublic(spec);
        KeyAgreement ka = KeyAgreement.getInstance("DiffieHellman");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        SecretKey premasterSecret = ka.generateSecret("TlsPremasterSecret");

        // Client now computes Master Secret from premaster secret bytes, TLS version, and client and server randoms
        // SunJSSE does this with code similar to the following:
        AlgorithmParameterSpec masterSpec =
                new TlsMasterSecretParameterSpec(premasterSecret, 3, 0, new byte[32], new byte[32]);
        KeyGenerator kg = KeyGenerator.getInstance("SunTlsMasterSecret");
        kg.init(masterSpec);
        SecretKey masterSecret = kg.generateKey(); // fails here if premasterSecret came from LunaKeyAgreementDh

        /*
         SunJSSE includes its own implementation of KeyGenerator.SunTlsMasterSecret which requires
         that the premasterSecret's encoding be RAW as it needs to call premasterSecret.getEncoded()
         to get the raw bytes to pass into the TLS pseudo-random function (which for TLS 1.0 is a combination
         of both SHA-1 and MD5).

         This will not work if the premaster secret is a SecretKey with the format "proprietary" that does
         not provide access to the raw key bytes.

         Possible solutions:

         1. KeyAgreement.DiffieHellman (and KeyAgreement.ECDH) could return a SecretKeySpec instance
            instead of a LunaSecretKey instance when the algorithm is "TlsPremasterSecret" or is unrecognized.
            The algorithm of the SecretKeySpec should match the algorithm requested in the generateKey() call.

         2. Provide an implementation of KeyGenerator.SunTlsMasterSecret that is able to perform the
            TLS PRF on a LunaSecretKey instance.  At best, this may be possible to do right on the token.
            At worst, this would have to wrap off the premaster secret and then chain to Sun's TlsMasterSecretGenerator.
         */
    }
}
