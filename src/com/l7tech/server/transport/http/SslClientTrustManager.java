/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.http;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.cert.TrustedCertManager;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SslClientTrustManager implements X509TrustManager {
    private static class SingletonHolder {
        private static final SslClientTrustManager singleton = new SslClientTrustManager();
    }

    public static SslClientTrustManager getInstance() {
        return SingletonHolder.singleton;
    }

    private SslClientTrustManager() {
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore)null);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.toString(), e);
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            logger.log(Level.SEVERE, e.toString(), e);
            throw new RuntimeException(e);
        }
        TrustManager[] defaultTrustManagers = tmf.getTrustManagers();
        X509TrustManager delegate = null;
        for (int i = 0; i < defaultTrustManagers.length; i++) {
            if (defaultTrustManagers[i] instanceof X509TrustManager) {
                delegate = (X509TrustManager)defaultTrustManagers[i];
                break;
            }
        }

        if (delegate == null) throw new RuntimeException("Couldn't locate an X509TrustManager implementation");

        this.delegate = delegate;
    }

    public X509Certificate[] getAcceptedIssuers() {
        throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
        throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        final X509Certificate serverCert = certs[0];
        try {
            // Try cartel first
            delegate.checkServerTrusted(certs, authType);
            logger.fine("SSL server cert was issued to '" + serverCert.getSubjectDN().getName() + "' by a globally recognized CA" );
            return;
        } catch ( CertificateException unused ) {
            serverCert.checkValidity();

            // Not trusted by cartel, consult SSG trust store
            TrustedCertManager manager = (TrustedCertManager)Locator.getDefault().lookup(TrustedCertManager.class);
            manager.checkSslTrust(certs);
        }
    }

    private final X509TrustManager delegate;
    private final Logger logger = Logger.getLogger(this.getClass().getName());
}
