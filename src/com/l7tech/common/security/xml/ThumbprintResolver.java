/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import java.security.cert.X509Certificate;

/**
 * Interface implemented by entities capable of lookup up X.509 certificate by their SHA1 thumbprint.
 */
public interface ThumbprintResolver {
    /**
     * Look up a certificate by its base64-ed SHA1 thumbprint.
     *
     * @param thumbprint  the base64'ed thumbprint of the cert to look up.  Must not be null or empty.
     * @return the certificate that was found, or null if one was not found.
     */
    X509Certificate lookup(String thumbprint);

    X509Certificate lookupBySki(String ski);
}
