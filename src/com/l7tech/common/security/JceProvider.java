/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Provider;
import java.security.SignatureException;

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

    public static Provider getProvider() {
        return Holder.engine.getProvider();
    }

    public static RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        return Holder.engine.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass);
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
