/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The <code>SSLTrustFailureHandler</code> implementation that trusts the single
 * certificate.
 *
 * @author emil
 * @version Jan 17, 2005
 */
public final class CertificateTrustedTrustFailureHandler implements SSLTrustFailureHandler {
    private X509Certificate trusted;

    /**
     * Create the new instance that trusts the single certificate.
     * @param trusted the trusted certificate
     */
    public CertificateTrustedTrustFailureHandler(X509Certificate trusted) {
        this.trusted = trusted;
    }

    /**
     * Handle the trust failure and return whether the connection should proceed or
     * fail.
     *
     * @param e        the exception that caused the trust failure
     * @param chain    the peer certificate chain
     * @param authType the authentication type that was requested
     * @return true to proceed with the ssl connection, false otherwise
     */
    public boolean handle(CertificateException e, X509Certificate[] chain, String authType) {
        if (chain == null || chain.length == 0) {
            return false;
        }
        return chain[0].equals(trusted);
    }
}