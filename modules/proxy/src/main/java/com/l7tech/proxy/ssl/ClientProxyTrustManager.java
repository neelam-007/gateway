/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.common.io.CertUtils;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;
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
        X509Certificate cert = x509Certificates[0];

        Set<String> cns = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        cns.addAll(CertUtils.extractCommonNamesFromCertificate(cert));
        if (!cns.contains(expectedHostname)) {
            final String peerdn = cert.getSubjectDN().getName();
            final String msg = "Server certificate name (" + peerdn +
                    ") did not match the hostname we connected to (" + expectedHostname + ")";
            log.log(Level.SEVERE, msg);
            throw new HostnameMismatchException(expectedHostname, peerdn, msg);
        }

        // Get the trusted CA key for this SSG.
        X509Certificate trustedCert = null;
        trustedCert = peer.getServerCertificate();
        if (trustedCert == null) {
            final String msg = "Not configured to trust this server's SSL server certificate";
            log.log(Level.FINE, msg);
            throw new ServerCertificateUntrustedException(msg);       // sslPeer already guaranteed to be set
        }

        try {
            CertVerifier.verifyCertificateChain(x509Certificates, trustedCert);
            log.log(Level.FINE, "Peer certificate was signed by a trusted Gateway.");

            KeyUsageChecker.requireActivity(KeyUsageActivity.sslServerRemote, cert);
        } catch (KeyUsageException e) {
            log.warning(e.getMessage()); // JSSE won't log it for us
            throw new KeyUsageException(e);
        } catch (CertUtils.CertificateUntrustedException e) {
            log.warning(e.getMessage()); // JSSE won't log it for us
            throw new ServerCertificateUntrustedException(e); // ssl peer already guaranteed to be set
        } catch ( CertificateException e ) {
            log.warning(e.getMessage()); // JSSE won't log it for us
            throw e;
        }
    }
}
