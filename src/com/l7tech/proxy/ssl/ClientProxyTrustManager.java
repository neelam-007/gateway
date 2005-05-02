/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.ibm.util.x500name.X500Name;
import com.ibm.util.x500name.RDNAttribute;
import com.l7tech.common.util.CertUtils;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
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
        X509Certificate cert = x509Certificates[0];
        cn = extractCommonName(cert.getSubjectX500Principal());

        if (!cn.equalsIgnoreCase(expectedHostname)) {
            final String msg = "Server certificate name (" + cn +
                    ") did not match the hostname we connected to (" + expectedHostname + ")";
            log.log(Level.SEVERE, msg);
            throw new HostnameMismatchException(expectedHostname, cn, msg);
        }

        // Get the trusted CA key for this SSG.
        X509Certificate trustedCert = null;
        trustedCert = peer.getServerCertificate();
        if (trustedCert == null) {
            final String msg = "Not configured to trust this server's SSL server certificate";
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

    /** extract the common name */
    private String extractCommonName(X500Principal subjectX500Principal) throws CertificateException {
        String principal = subjectX500Principal.toString();
        X500Name x500Name = new X500Name(principal);
        RDNAttribute attribute = x500Name.attribute("cn");
        if (attribute == null) {
            String msg = "Could not determine the Common Name (CN) value for " + principal;
            log.log(Level.SEVERE, msg);
            throw new CertificateException(msg);
        }
        return attribute.valueToString();
    }
}
