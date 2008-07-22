/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.http;

import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.types.CertificateValidationResult;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.SignatureException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class SslClientTrustManager implements X509TrustManager {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final TrustedCertManager trustedCertManager;
    private final CertValidationProcessor certValidationProcessor;
    private final CertValidationProcessor.Facility facility;

    public SslClientTrustManager(final TrustedCertManager trustedCertManager,
                                 final CertValidationProcessor certValidationProcessor,
                                 final CertValidationProcessor.Facility facility) {
        if (trustedCertManager == null)  throw new IllegalArgumentException("Trusted Cert Manager is required");
        if (certValidationProcessor == null)  throw new IllegalArgumentException("Cert Validation Processor is required");
        if (facility == null)  throw new IllegalArgumentException("Facility is required");

        this.trustedCertManager = trustedCertManager;
        this.certValidationProcessor = certValidationProcessor;
        this.facility = facility;
    }

    public X509Certificate[] getAcceptedIssuers() {
        throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
    }

    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType) throws CertificateException {
        throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
    }

    public void checkServerTrusted(final X509Certificate[] certs, final String authType) throws CertificateException {
        boolean isCartel = false;
        try {
            trustedCertManager.checkSslTrust(certs);
        } catch (TrustedCertManager.UnknownCertificateException uce) {
            // this is ok, as long as it is a cert from a known CA
            isCartel = true;
        }

        try {
            // minimum permissable validation is PATH_VALIDATION, since we're
            // not otherwise validating the certificate.
            CertificateValidationResult result =
                    certValidationProcessor.check(certs, CertificateValidationType.PATH_VALIDATION, null, facility, new LogOnlyAuditor(logger));

            if ( result != CertificateValidationResult.OK ) {
                throw new CertificateException("Certificate path validation and/or revocation checking failed");
            }
            
            if (isCartel && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "SSL server cert was issued to ''{0}'' by a globally recognized CA", certs[0].getSubjectDN().getName());
            }
        } catch (SignatureException se) {
            throw new CertificateException("Certificate path validation and/or revocation checking error", se);            
        }
    }
}
