/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.common.util.CertUtils;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import sun.security.x509.X500Name;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trust manager for the Client Proxy, which will decide whether a given connection is actually connected
 * to the SSG we expect it to be.
 *
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:49:58 PM
 */
public class ClientProxyTrustManager implements X509TrustManager {
    private static final Logger log = Logger.getLogger(ClientProxyTrustManager.class.getName());

    private static class CausedCertificateException extends CertificateException {
        public CausedCertificateException() {
        }

        public CausedCertificateException(String msg) {
            super(msg);
        }

        public CausedCertificateException(Throwable cause) {
            super();
            initCause(cause);
        }

        public CausedCertificateException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }

    public ClientProxyTrustManager() {}

    public X509Certificate[] getAcceptedIssuers() {
        // Find our current ssg
        SslPeer peer = CurrentSslPeer.get();
        if (peer == null)
            throw new IllegalStateException("No SSL peer is available in this thread");

        X509Certificate ssgCert = null;
        ssgCert = peer.getServerCertificate();
        return ssgCert == null ? new X509Certificate[0] : new X509Certificate[] { ssgCert };
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new ClientProxySslException("Server-side SSL sockets not supported by ClientProxyTrustManager");
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        log.log(Level.FINER, "ClientProxyTrustManager: checkServerTrusted");
        if (x509Certificates == null || x509Certificates.length < 1 || s == null)
            throw new IllegalArgumentException("empty certificate chain, or null auth type");

        // Find our current ssg
        SslPeer peer = CurrentSslPeer.get();
        if (peer == null)
            throw new IllegalStateException("No peer Gateway is available in this thread");

        // Remember last cert presented by this peer during the handshake
        peer.storeLastSeenPeerCertificate(x509Certificates[0]);

        // Verify the hostname
        String expectedHostname = peer.getHostname();
        String cn = "";
        try {
            X509Certificate cert = x509Certificates[0];
            cn = new X500Name(cert.getSubjectX500Principal().toString()).getCommonName();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e); // log in case SSL layer obscures our diagnostic info
            throw new CausedCertificateException(e);
        }
        if (!cn.equals(expectedHostname)) {
            final String msg = "Server certificate name (" + cn +
                    ") did not match the hostname we connected to (" + expectedHostname + ")";
            log.log(Level.SEVERE, msg);
            throw new HostnameMismatchException(expectedHostname,
                                                cn,
                                                msg);
        }

        // Get the trusted CA key for this SSG.
        X509Certificate trustedCert = null;
        trustedCert = peer.getServerCertificate();
        if (trustedCert == null) {
            final String msg = "We have not yet discovered this Gateway's server certificate";
            log.log(Level.FINE, msg);
            throw new ServerCertificateUntrustedException(msg);
        }

        try {
            CertUtils.verifyCertificateChain( x509Certificates, trustedCert, 1 );
            log.log(Level.FINE, "Peer certificate was signed by a trusted Gateway.");
        } catch (CertUtils.CertificateUntrustedException e) {
            log.warning(e.getMessage()); // log in case SSL layer obscures our diagnostic info
            throw new ServerCertificateUntrustedException(e);
        } catch ( CertificateException e ) {
            log.warning(e.getMessage()); // log in case SSL layer obscures our diagnostic info
            throw e;
        }
    }
}
