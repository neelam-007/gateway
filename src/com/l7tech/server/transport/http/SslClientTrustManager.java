/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.http;

import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.common.util.Locator;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.objectmodel.FindException;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class SslClientTrustManager implements X509TrustManager {
    public SslClientTrustManager(X509TrustManager delegate) {
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
            logger.fine("SSL server cert was issued to '" + serverCert.getSubjectDN().getName() + "' by a recognized CA" );
            return;
        } catch ( CertificateException unused ) {
            // Not trusted by cartel, consult SSG trust store
            TrustedCertManager manager = (TrustedCertManager)Locator.getDefault().lookup(TrustedCertManager.class);

            String subjectDn = serverCert.getSubjectDN().getName();
            String issuerDn = serverCert.getIssuerDN().getName();
            if ( subjectDn.equals(issuerDn) ) {
                // Check if self-signed cert is trusted
                try {
                    TrustedCert selfTrust = manager.findBySubjectDn(subjectDn);
                    if ( selfTrust == null ) throw new CertificateException("Couldn't find self-signed cert with DN '" + subjectDn + "'");
                    if ( !selfTrust.isTrustedForSsl() ) throw new CertificateException("Self-signed cert with DN '" + subjectDn + "' present but not trusted for SSL" );
                    if ( !selfTrust.getCertificate().equals(serverCert) ) throw new CertificateException("Self-signed cert with DN '" + subjectDn + "' present but doesn't match" );
                    return; // OK
                } catch (FindException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    throw new CertificateException(e.getMessage());
                } catch (IOException e) {
                    final String msg = "Couldn't decode stored certificate";
                    logger.log(Level.SEVERE, msg, e);
                    throw new CertificateException(msg);
                }
            } else {
                // Check that signer is trusted
                // TODO more than two levels?
                try {
                    TrustedCert caTrust = manager.findBySubjectDn(issuerDn);

                    if ( caTrust == null )
                        throw new FindException("Couldn't find CA cert with DN '" + issuerDn + "'" );

                    if ( !caTrust.isTrustedForSigningServerCerts() )
                        throw new CertificateException("CA Cert with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");

                    if ( certs.length < 2 ) {
                        // TODO this might conceivably be normal
                        throw new CertificateException("Couldn't find CA Cert in chain");
                    } else if ( certs.length > 2 ) {
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
    }

    private X509TrustManager delegate;
    private final Logger logger = Logger.getLogger(this.getClass().getName());
}
