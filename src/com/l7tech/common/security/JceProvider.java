/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine;

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
    private static JceProviderEngine engine = new BouncyCastleJceProviderEngine();

    public static void init() {
        // No action required; work was done when this class was loaded.
    }

    public static Provider getProvider() {
        return engine.getProvider();
    }

    public static RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        return engine.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass);
    }

    public static KeyPair generateRsaKeyPair() {
        return engine.generateRsaKeyPair();
    }

    public static CertificateRequest makeCsr(String username, KeyPair keyPair)
                 throws SignatureException, InvalidKeyException, RuntimeException
    {
        return engine.makeCsr(username, keyPair);
    }
}
