/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.KerberosSecurityToken;

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
     * Look up an EncryptedKey by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to look up.  Never null or empty.
     * @return the matching EncryptedKey token, or null if no match was found.
     * @see com.l7tech.common.security.xml.processor.WssProcessorUtil#makeEncryptedKey(javax.crypto.SecretKey,String)
     */
    EncryptedKey getEncryptedKeyBySha1(String encryptedKeySha1);

    /**
     * Report that an EncryptedKey was decrypted, so it can be saved for later reuse by its EncryptedKeySHA1.
     *
     * @param encryptedKey      the encrypted key to cache for later reuse.  Must not be null.
     */
    void cacheEncryptedKey(EncryptedKey encryptedKey);

    /**
     * Look up a Kerberos token using a Kerberosv5APREQSHA1 reference.
     *
     * @param kerberosSha1  the base64-encoded Kerberosv5APREQSHA1 identifier to look up
     * @return the matching already-known Kerberos token, or null if no match was found.
     */
    KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1);
}
