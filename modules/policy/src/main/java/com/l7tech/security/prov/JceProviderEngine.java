/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Provides routines that do cryptographic operations using an underlying crypto api.
 * @author mike
 * @version 1.0
 */
public interface JceProviderEngine {
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
     * Get the Provider for Signature instances.
     * @return a Provider.
     */
    Provider getSignatureProvider();

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     * This can be used to sign certificates.
     *
     * @param caKey       CA private key for signing.  Required.
     * @param caCertChain CA cert chain for signing.  Required.
     * @return an RsaSignerEngine that can be used to sign certificates.  Never null.
     */
    RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain);

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider with the specified key size.
     * @param keysize  desired RSA key size in bits, ie 1024.
     * @return a new RSA KeyPair instance with the specified key size.
     */
    KeyPair generateRsaKeyPair(int keysize);

    /**
     * Generate an ECC public key / private key pair using the current Crypto provider with the specified curve name.
     *
     * @param curveName  curve name to use, ie "p384".  Required.
     * @return the generated key pair.  Never null.
     * @throws NoSuchAlgorithmException  if ECC is not currently available.
     * @throws InvalidAlgorithmParameterException if the specified curve name is unrecognized.
     */
    KeyPair generateEcKeyPair(String curveName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return a new PKCS#10 CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if the specified key is unsuitable
     * @throws java.security.SignatureException   if there is a problem signing the CSR
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

    /**
     * Get a Provider appropriate for the specified service.  See JceProvider for a list of recognized service
     * names.
     *
     * @param service the service to get.  Uses the name from JceProvider rather than the "real" name, which may be
     *          different if the current engine names it differently.
     * @return a Provider for the service, or null if the default provider should be used instead.
     */
    Provider getProviderFor(String service);
}
