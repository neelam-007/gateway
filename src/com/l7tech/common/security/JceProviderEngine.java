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
    RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType);

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

    /**
     * Get an implementation of RSA configured to work in raw mode, with padding disabled.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException;
}
