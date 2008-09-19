/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.spring.remoting.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Implementations are notified during ssl handskhake that the trust could
 * not be established because of certificate related issues. The peer certificate
 * missing from the trust store for example.
 * @author emil
 * @version Dec 20, 2004
 */
public interface SSLTrustFailureHandler {
    /**
     * Handle the trust failure and return whether the connection should proceed or
     * fail.
     *
     * @param e the exception that caused the trust failure
     * @param chain the peer certificate chain
     * @param authType the authentication type that was requested
     * @param trustFailed true if this is a trust failure (else just informational)
     * @return true to proceed with the ssl connection, false otherwise
     */
    boolean handle(CertificateException e, X509Certificate[] chain, String authType, boolean trustFailed);
}