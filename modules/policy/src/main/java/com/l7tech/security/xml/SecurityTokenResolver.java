/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import com.l7tech.security.token.KerberosSecurityToken;

import java.security.cert.X509Certificate;

/**
 * Interface implemented by entities capable of lookup up X.509 certificate by their SHA1 thumbprint, SKI, or KeyName.
 */
public interface SecurityTokenResolver {
    /**
     * Look up a certificate by its base64-ed SHA1 thumbprint.
     *
     * @param thumbprint  the base64'ed thumbprint of the cert to look up.  Must not be null or empty.
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookup(String thumbprint);

    /**
     * Look up a certificate by its base64-ed SKI.
     *
     * @param ski the SKI to look up, as a base64'ed string.
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookupBySki(String ski);

    /**
     * Look up a certificate by "key name".  A KeyName can be anything, but for the purposes of this method
     * implementors may assume that it is a cert DN.
     *
     * @param keyName the key name to look up, assumed be a DN.  Must not be null or empty.
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookupByKeyName(String keyName);

    /**
     * Get the private key (and the rest of the cert chain) for the specified certificate, if we have access to it.
     *
     * @param cert the certificate to look up.  Required.
     * @return the matching private key that was found, or null if one was not found.
     */
    SignerInfo lookupPrivateKeyByCert(X509Certificate cert);

    /**
     * Look up a private key (with cert chain) by its base64-ed SHA1 thumbprint.
     *
     * @param thumbprint  the base64'ed thumbprint of the cert to look up.  Must not be null or empty.
     * @return the private key that was found, or null if one was not found.
     */
    SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint);

    /**
     * Look up a private key (with cert chain) by its base64-ed SKI.
     *
     * @param ski the SKI to look up, as a base64'ed string.
     * @return the private key that was found, or null if one was not found.
     */
    SignerInfo lookupPrivateKeyBySki(String ski);

    /**
     * Look up a private key (with cert chain) by "key name".  A KeyName can be anything, but for the purposes of this method
     * implementors may assume that it is a cert DN.
     *
     * @param keyName the key name to look up, assumed be a DN.  Must not be null or empty.
     * @return the private key that was found, or null if one was not found.
     */
    SignerInfo lookupPrivateKeyByKeyName(String keyName);

    /**
     * Look up an EncryptedKey by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to look up.  Never null or empty.
     * @return the matching EncryptedKey token, or null if no match was found.  The returned token is unmodifiable.
     * @see com.l7tech.security.xml.processor.WssProcessorUtil#makeEncryptedKey
     */
    byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1);

    /**
     * Report that an EncryptedKey was decrypted, so it can be saved for later reuse by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to store, in the form of an EncryptedKeySHA1 string, which is the base64
     *                         encoded ciphertext of the secret key.  Must not be null or empty.
     * @param secretKey  the unwrapped SecretKey that came from the EncryptedKey with the specified EncryptedKeySha1.
     */
    void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey);

    /**
     * Look up a Kerberos token using a Kerberosv5APREQSHA1 reference.
     *
     * @param kerberosSha1  the base64-encoded Kerberosv5APREQSHA1 identifier to look up
     * @return the matching already-known Kerberos token, or null if no match was found.
     */
    KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1);
}
