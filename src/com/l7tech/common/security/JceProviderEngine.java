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
 * Provides routines that do cryptographic operations using an underlying crypto api.
 * @author mike
 * @version 1.0
 */
public interface JceProviderEngine {
    int RSA_KEY_LENGTH = 1024;

    /**
     * Get the Provider.
     * @return the JCE Provider
     */
    Provider getAsymmetricProvider();

    /**
     * Get the Provider.
     * @return the JCE Provider
     */
    Provider getSymmetricProvider();

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     *
     * @param keyStorePath
     * @param storePass
     * @param privateKeyAlias
     * @param privateKeyPass
     * @return
     */
    RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass);

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     *
     * @return
     */
    KeyPair generateRsaKeyPair();

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return
     */
    CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException;
}
