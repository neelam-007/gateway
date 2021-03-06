/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import com.l7tech.security.token.KerberosSigningSecurityToken;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * Interface implemented by entities capable of lookup up X.509 certificate by their SHA1 thumbprint, SKI, or KeyName.
 */
public interface SecurityTokenResolver extends EncryptedKeyCache {
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
     * Look up a certificate by Issuer and Serial.
     *
     * @param issuer The certificate issuing authority
     * @param serial The certificate serial number
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookupByIssuerAndSerial( X500Principal issuer, BigInteger serial );

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
     * Look up a private key by its certificate Issuer and Serial.
     *
     * @param issuer The certificate issuing authority
     * @param serial The certificate serial number
     * @return the private key that was found, or null if one was not found.
     */
    SignerInfo lookupPrivateKeyByIssuerAndSerial( X500Principal issuer, BigInteger serial );

    /**
     * Look up a Kerberos token using a Kerberosv5APREQSHA1 reference.
     *
     * @param kerberosSha1  the base64-encoded Kerberosv5APREQSHA1 identifier to look up
     * @return the matching already-known Kerberos token, or null if no match was found.
     */
    KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1);

    /**
     * Lookup a secret key by token type and identifier.
     *
     * <p>WARNING: This method is a temporary solution until we implement full
     * support for SAML tokens with symmetric proof keys.</p>
     *
     * @param type The token type
     * @param identifier The identifier for the token
     * @return The secret or null
     */
    byte[] getSecretKeyByTokenIdentifier( String type, String identifier );

    /**
     * Store a secret key by token type and identifier.
     *
     * <p>WARNING: This method is a temporary solution until we implement full
     * support for SAML tokens with symmetric proof keys.</p>
     *
     * @param type The token type
     * @param identifier The identifier for the token
     * @param secretKey The secret
     */
    void putSecretKeyByTokenIdentifier( String type, String identifier, byte[] secretKey );
}
