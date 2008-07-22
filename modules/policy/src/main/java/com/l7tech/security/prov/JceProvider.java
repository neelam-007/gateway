/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * Provide a single point where our JCE provider can be altered.
 * @author mike
 * @version 1.0
 */
public abstract class JceProvider {
    public static final String ENGINE_PROPERTY = "com.l7tech.common.security.jceProviderEngine";

    // Available drivers
    public static final String BC_ENGINE = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String PHAOS_ENGINE = "com.l7tech.security.prov.phaos.PhaosJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
    public static final String NCIPHER_ENGINE = "com.l7tech.security.prov.ncipher.NcipherJceProviderEngine";
    public static final String ENTRUST_ENGINE = "com.l7tech.security.prov.entrust.EntrustJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.security.prov.sun.SunJceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.security.prov.luna.LunaJceProviderEngine";
    public static final String PKCS11_ENGINE = "com.l7tech.security.prov.pkcs11.Pkcs11JceProviderEngine";

    // Default driver
    private static final String DEFAULT_ENGINE = BC_ENGINE;

    private static class Holder {
        private static final String ENGINE = System.getProperty(ENGINE_PROPERTY, DEFAULT_ENGINE);
        private static final JceProviderEngine engine = createEngine();

        private static JceProviderEngine createEngine() {
            try {
                return (JceProviderEngine) Class.forName(ENGINE).getConstructors()[0].newInstance(new Object[0]);
            } catch (Throwable t) {
                throw new RuntimeException("Requested JceProviderEngine could not be instantiated: " + t.getMessage(), t);
            }
        }

        private static JceProviderEngine getEngine() {
            return engine;
        }
    }

    public static void init() {
        // no action should be required
        Holder.getEngine();
    }

    /** @return the JceProviderEngine class name currently being used. */
    public static String getEngineClass() {
        init();
        return Holder.ENGINE;
    }

    public static Provider getSymmetricJceProvider() {
        return Holder.engine.getSymmetricProvider();
    }

    public static Provider getAsymmetricJceProvider() {
        return Holder.engine.getAsymmetricProvider();
    }

    public static Cipher getRsaNoPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaNoPaddingCipher();
    }

    public static Cipher getRsaOaepPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaOaepPaddingCipher();
    }

    public static Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Holder.engine.getRsaPkcs1PaddingCipher();
    }

    public static RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return Holder.engine.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType);
    }

    public static KeyPair generateRsaKeyPair() {
        return Holder.engine.generateRsaKeyPair();
    }

    public static KeyPair generateRsaKeyPair(int keysize) {
        return Holder.engine.generateRsaKeyPair(keysize);
    }

    public static CertificateRequest makeCsr(String username, KeyPair keyPair)
                 throws SignatureException, InvalidKeyException, RuntimeException
    {
        return Holder.engine.makeCsr(username, keyPair);
    }
}
