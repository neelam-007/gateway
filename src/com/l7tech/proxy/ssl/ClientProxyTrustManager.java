/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.util.ClientLogger;
import com.l7tech.common.util.CertUtils;
import sun.security.x509.X500Name;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Trust manager for the Client Proxy, which will decide whether a given connection is actually connected
 * to the SSG we expect it to be.
 *
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:49:58 PM
 */
public class ClientProxyTrustManager implements X509TrustManager {
    private static final ClientLogger log = ClientLogger.getInstance(ClientProxyTrustManager.class);

    public X509Certificate[] getAcceptedIssuers() {
        // Find our current ssg
        Ssg ssg = CurrentRequest.getCurrentSsg();
        if (ssg == null)
            throw new IllegalStateException("No current Gateway is available in this thread");

        X509Certificate ssgCert = null;
        try {
            ssgCert = SsgKeyStoreManager.getServerCert(ssg);
            return ssgCert == null ? new X509Certificate[0] : new X509Certificate[] { ssgCert };
        } catch (KeyStoreCorruptException e) {
            return new X509Certificate[0];
        }
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new ClientProxySslException("Server-side SSL sockets not supported by ClientProxyTrustManager");
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        log.info("ClientProxyTrustManager: checkServerTrusted");
        if (x509Certificates == null || x509Certificates.length < 1 || s == null)
            throw new IllegalArgumentException("empty certificate chain, or null auth type");

        // Find our current ssg
        Ssg ssg = CurrentRequest.getCurrentSsg();
        if (ssg == null)
            throw new IllegalStateException("No current Gateway is available in this thread");

        // Verify the hostname
        String expectedHostname = ssg.getSsgAddress();
        String cn = "";
        try {
            X509Certificate cert = x509Certificates[0];
            cn = new X500Name(cert.getSubjectX500Principal().toString()).getCommonName();
        } catch (IOException e) {
            log.error(e);
            // can't happen
        }
        if (!cn.equals(expectedHostname))
            throw new HostnameMismatchException(expectedHostname,
                                                cn,
                                                "Server certificate name (" + cn +
                                                ") did not match the hostname we connected to (" +
                                                expectedHostname + ")");

        // Get the trusted CA key for this SSG.
        X509Certificate trustedCert = null;
        try {
            trustedCert = SsgKeyStoreManager.getServerCert(ssg);
        } catch (KeyStoreCorruptException e) {
            throw new RuntimeException(e);
        }
        if (trustedCert == null)
            throw new ServerCertificateUntrustedException("We have not yet discovered this Gateway's server certificate");

        try {
            CertUtils.verifyCertificateChain( x509Certificates, trustedCert, 1 );
            log.info("Peer certificate was signed by a trusted Gateway.");
        } catch (CertUtils.CertificateUntrustedException e) {
            log.warn(e.getMessage());
            throw new ServerCertificateUntrustedException(e);
        } catch ( CertificateException e ) {
            log.warn(e.getMessage());
            throw e;
        }
    }
}
