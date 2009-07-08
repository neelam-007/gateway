package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;

import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class LunaReproSigVerifyFalse {
    static boolean parsePublicKeyWithBouncyCastle = true;
    static boolean parsePrivateKeyWithBouncyCastle = true;

    public static void main(String[] args) throws Exception {
        Security.addProvider(new LunaJCEProvider());
        Security.addProvider(new LunaJCAProvider());
        LunaTokenManager.getInstance().Login("t46/-979S-LHW3-dS6W");

        // Test data to sign
        final byte[] data =
                ("This is some test data to use as a payload for signatures/SSL/etc.\n\n" +
                "I'd like it to be fairly long -- about a paragraph, say.  It doesn't really matter what's in it.\n" +
                "One more short line ought to do it -- there we go.").getBytes("UTF-8");

        // Sign with private key
        byte[] sigValue = signData(data, getTestPrivateKey(), "SHA1withECDSA");

        // Verify with public key
        boolean verified = verifySignature(data, getTestPublicKey(), "SHA1withECDSA", sigValue);

        if (!verified)
            throw new IllegalStateException("Signature.verify() returned false");
    }

    static boolean verifySignature(byte[] data, PublicKey publicKey, String sigAlg, byte[] sigValue) throws Exception {
        Signature sig = Signature.getInstance(sigAlg);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(sigValue);
    }

    static byte[] signData(byte[] dataToSign, final PrivateKey privateKey, String sigAlg) throws Exception {
        Signature sig = Signature.getInstance(sigAlg);
        sig.initSign(privateKey);
        sig.update(dataToSign);
        return sig.sign();
    }

    static KeyFactory getKeyFactory(boolean useBouncyCastle) throws NoSuchAlgorithmException {
        return useBouncyCastle ?
                KeyFactory.getInstance("EC", new BouncyCastleProvider()) :
                KeyFactory.getInstance("EC");
    }

    static PrivateKey getTestPrivateKey() throws Exception {
        KeySpec spec = new PKCS8EncodedKeySpec(
                new BASE64Decoder().decodeBuffer("MGICAQAwEAYHKoZIzj0CAQYFK4EEAAEESzBJAgEBBBRQieICGuF1jSJ4tU3R9yCh\n" +
                                                 "NV8g5qEuAywABAE814kv8euQhqkuB/zb93UYu4NS5AV1c/4SU86koMXBQL1pNymX\n" +
                                                 "gAVp9w=="));
        return getKeyFactory(parsePrivateKeyWithBouncyCastle).generatePrivate(spec);
    }

    static PublicKey getTestPublicKey() throws Exception {
        KeySpec spec = new X509EncodedKeySpec(
                new BASE64Decoder().decodeBuffer("MCswEAYHKoZIzj0CAQYFK4EEAAEDFwACATzXiS/x65CGqS4H/Nv3dRi7g1Lk"));
        return getKeyFactory(parsePublicKeyWithBouncyCastle).generatePublic(spec);
    }
}
