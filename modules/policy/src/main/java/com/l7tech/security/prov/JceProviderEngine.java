/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

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
     */
    RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType);

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     */
    KeyPair generateRsaKeyPair();

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider with the specified key size.
     */
    KeyPair generateRsaKeyPair(int keysize);

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
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

    /**
     * Get an implementation of RSA configured to work in raw mode, with OAEP padding.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException;

    /**
     * Get an implementation of RSA configured to work in raw mode, with PKCS#1 version 1.5 padding.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException;
}
