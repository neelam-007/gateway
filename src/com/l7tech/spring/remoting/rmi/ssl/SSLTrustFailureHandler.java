/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Implementations are notified during ssl handskhake that the trust could
 * not be established because of certificate related issues. The peer certificate
 * missing from the trust store for example.
 * @author emil
 * @version Dec 20, 2004
 * @see com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory#setTrustFailureHandler(SSLTrustFailureHandler)
 */
public interface SSLTrustFailureHandler {
    /**
     * HAndle the trust failure and return whether the connection should proceed or
     * fail.
     *
     * @param e the exception that caused the trust failure
     * @param chain the peer certificate chain
     * @param authType the authentication type that was requested
     * @return true to proceed with the ssl connection, false otherwise
     */
    boolean handle(CertificateException e, X509Certificate[] chain, String authType);
}