/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.http;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO clear SSL Context when any TrustedCert changes
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
            if ( defaultTrustManagers[i] instanceof X509TrustManager ) {
                delegate = (X509TrustManager)defaultTrustManagers[i];
                break;
            }
        }

        if ( delegate == null ) throw new RuntimeException("Couldn't locate an X509TrustManager implementation");

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

            String subjectDn = serverCert.getSubjectDN().getName();
            String issuerDn = serverCert.getIssuerDN().getName();

            // Check if this cert is trusted as-is
            try {
                TrustedCert selfTrust = manager.getCachedCertBySubjectDn(subjectDn, MAX_CACHE_AGE);
                if ( selfTrust != null ) {
                    if ( !selfTrust.isTrustedForSsl() ) throw new CertificateException("Self-signed cert with DN '" + subjectDn + "' present but not trusted for SSL" );
                    if ( !selfTrust.getCertificate().equals(serverCert) ) throw new CertificateException("Self-signed cert with DN '" + subjectDn + "' present but doesn't match" );
                    return; // OK
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new CertificateException(e.getMessage());
            } catch (IOException e) {
                final String msg = "Couldn't decode stored certificate";
                logger.log(Level.SEVERE, msg, e);
                throw new CertificateException(msg);
            }

            // Check that signer is trusted
            try {
                TrustedCert caTrust = manager.getCachedCertBySubjectDn(issuerDn, MAX_CACHE_AGE);

                if ( caTrust == null )
                    throw new FindException("Couldn't find CA cert with DN '" + issuerDn + "'" );

                if ( !caTrust.isTrustedForSigningServerCerts() )
                    throw new CertificateException("CA Cert with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");

                if ( certs.length < 2 ) {
                    // TODO this might conceivably be normal
                    throw new CertificateException("Couldn't find CA Cert in chain");
                } else if ( certs.length > 2 ) {
                    // TODO support more than two levels?
                    throw new CertificateException("Certificate chains with more than two levels are not supported");
                }

                X509Certificate caCert = certs[1];
                X509Certificate caTrustCert = caTrust.getCertificate();

                if ( !caCert.equals(caTrustCert) )
                    throw new CertificateException("CA cert from server didn't match stored version");

                serverCert.verify(caTrustCert.getPublicKey());
            } catch (IOException e) {
                final String msg = "Couldn't decode stored CA certificate with DN '" + issuerDn + "'";
                logger.log(Level.SEVERE, msg, e);
                throw new CertificateException(msg);
            } catch (NoSuchProviderException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new CertificateException(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new CertificateException(e.getMessage());
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new CertificateException(e.getMessage());
            }
        }
    }

    private final X509TrustManager delegate;
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private static final int MAX_CACHE_AGE = 30 * 1000;
}
