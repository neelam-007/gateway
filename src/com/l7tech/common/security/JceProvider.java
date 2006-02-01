/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

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
    public static final String BC_ENGINE = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String PHAOS_ENGINE = "com.l7tech.common.security.prov.phaos.PhaosJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.common.security.prov.rsa.RsaJceProviderEngine";
    public static final String NCIPHER_ENGINE = "com.l7tech.common.security.prov.ncipher.NcipherJceProviderEngine";
    public static final String ENTRUST_ENGINE = "com.l7tech.common.security.prov.entrust.EntrustJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.common.security.prov.luna.LunaJceProviderEngine";

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

    public static RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return Holder.engine.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType);
    }

    public static KeyPair generateRsaKeyPair() {
        return Holder.engine.generateRsaKeyPair();
    }

    public static CertificateRequest makeCsr(String username, KeyPair keyPair)
                 throws SignatureException, InvalidKeyException, RuntimeException
    {
        return Holder.engine.makeCsr(username, keyPair);
    }
}
