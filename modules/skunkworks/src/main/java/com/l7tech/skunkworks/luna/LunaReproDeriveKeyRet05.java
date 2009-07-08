package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Simulate enough of client side of TLS_ECDH_ECDSA_WITH_AES128_CBC_SHA cipher suite
 * to attempt reproduce KeyAgreement.ECDH failure: C_DeriveKey returns 0x05.
 *
 * However, this test does not, in fact, manage to reproduce the failure.
 */
public class LunaReproDeriveKeyRet05 {
    public static void main(String[] args) throws Exception {
        Security.insertProviderAt(new LunaJCEProvider(), 1);
        Security.insertProviderAt(new LunaJCAProvider(), 1);
        LunaTokenManager.getInstance().Login("t46/-979S-LHW3-dS6W");

        // These params would normally come from the server's certified EC public key, sent to the client
        // in the Certificate message.
        ECPublicKey serverPublicKey = (ECPublicKey) getServerPublicKey();

        // On receipt of ServerHelloDone
        // Client generates ephemeral keypair on same curve as server cert (for static ECDH with no client auth)
        // It will later send the ephemeral public key back to the server in an ECDH client key exchange message.
        KeyPairGenerator clientKpg = KeyPairGenerator.getInstance("EC");
        clientKpg.initialize(serverPublicKey.getParams());
        final KeyPair clientKp = clientKpg.generateKeyPair();
        PublicKey clientPublicKey = clientKp.getPublic();
        PrivateKey clientPrivateKey = clientKp.getPrivate();

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(clientPrivateKey);
        ka.doPhase(serverPublicKey, true);
        SecretKey preMasterSecret = ka.generateSecret("TlsPremasterSecret");
    }

    static PublicKey getServerPublicKey() throws Exception {
        KeySpec spec = new X509EncodedKeySpec(
                new BASE64Decoder().decodeBuffer("MCswEAYHKoZIzj0CAQYFK4EEAAEDFwACATzXiS/x65CGqS4H/Nv3dRi7g1Lk"));
        return KeyFactory.getInstance("EC", new BouncyCastleProvider()).generatePublic(spec);
    }
}
