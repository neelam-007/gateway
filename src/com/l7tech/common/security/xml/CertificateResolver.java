/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import java.security.cert.X509Certificate;

/**
 * Interface implemented by entities capable of lookup up X.509 certificate by their SHA1 thumbprint, SKI, or KeyName.
 */
public interface CertificateResolver {
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
}
