/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.ncipher;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine;
import com.ncipher.provider.km.KMRSAKeyPairGenerator;
import com.ncipher.provider.km.nCipherKM;

import java.security.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author mike
 * @version 1.0
 */
public class NcipherJceProviderEngine implements JceProviderEngine {
    private static final Provider PROVIDER = new nCipherKM();

    public NcipherJceProviderEngine() {
        Security.insertProviderAt(PROVIDER, 0);
        Security.addProvider( new BouncyCastleProvider() );
    }

    /**
     * Get the Provider.
     * @return the JCE Provider
     */
    public Provider getProvider() {
        return PROVIDER;
    }

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     *
     * @param keyStorePath
     * @param storePass
     * @param privateKeyAlias
     * @param privateKeyPass
     * @return
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        return new NcipherRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass);
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     *
     * @return
     */
    public KeyPair generateRsaKeyPair() {
        KMRSAKeyPairGenerator kpg = new KMRSAKeyPairGenerator();
        return kpg.generateKeyPair();
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return
     */
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        return BouncyCastleJceProviderEngine.staticMakeCsr( username, keyPair );
    }
}
