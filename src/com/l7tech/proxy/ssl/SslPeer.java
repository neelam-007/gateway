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

    /** @return the server certificate to expect during the SSL handshake, or null if not known. */
    X509Certificate getServerCertificate();

    /** @return the client certificate to present during the SSL handshake, or null if not known. */
    X509Certificate getClientCertificate();

    /** @return the private key for the client certificate to present during the SSL handhsake, or null if not known. */
    PrivateKey getClientCertificatePrivateKey() throws OperationCanceledException, BadCredentialsException;

    /** @return the hostname to expect of the SSL peer during the SSL handshake. */
    String getHostname();

    /** @param actualPeerCert the peer certificate that the server presented during the SSL handshake. */
    void storeLastSeenPeerCertificate(X509Certificate actualPeerCert);

    /** @return the peer certificate that the server presented during the SSL handshake. */
    X509Certificate getLastSeenPeerCertificate();
}
