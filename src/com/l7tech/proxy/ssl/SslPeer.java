/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Holds information that must be known about the current SSL peer.
 */
public interface SslPeer extends SslContextHaver {
    String PROP_SSL_PROVIDER = "com.l7tech.proxy.sslProvider";
    String DEFAULT_SSL_PROVIDER = "SunJSSE";

    X509Certificate getServerCertificate();
    X509Certificate getClientCertificate();
    PrivateKey getClientCertificatePrivateKey() throws OperationCanceledException, BadCredentialsException;
    String getHostname();
}
