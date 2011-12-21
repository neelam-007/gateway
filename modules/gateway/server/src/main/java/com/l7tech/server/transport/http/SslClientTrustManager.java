package com.l7tech.server.transport.http;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.X509TrustManager;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SslClientTrustManager implements X509TrustManager {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final TrustedCertServices trustedCertServices;
    private final CertValidationProcessor certValidationProcessor;
    private final CertValidationProcessor.Facility facility;

    public SslClientTrustManager(final TrustedCertServices trustedCertServices,
                                 final CertValidationProcessor certValidationProcessor,
                                 final CertValidationProcessor.Facility facility) {
        if (trustedCertServices == null)  throw new IllegalArgumentException("Trusted Cert Services is required");
        if (certValidationProcessor == null)  throw new IllegalArgumentException("Cert Validation Processor is required");
        if (facility == null)  throw new IllegalArgumentException("Facility is required");

        this.trustedCertServices = trustedCertServices;
        this.certValidationProcessor = certValidationProcessor;
        this.facility = facility;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType) throws CertificateException {
        throw new UnsupportedOperationException("This trust manager can only be used for outbound SSL connections");
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] certs, final String authType) throws CertificateException {
        // bug 10751, fail early with a clear message when the servers certificate is invalid
        if ( !CertUtils.isValid( certs[0] ) )
            throw new CertificateException( "Certificate expired or not yet valid: " + certs[0].getSubjectDN().getName() );

        boolean isCartel = false;
        try {
            trustedCertServices.checkSslTrust(certs);
        } catch (TrustedCertManager.UnknownCertificateException uce) {
            // this is ok, as long as it is a cert from a known CA
            isCartel = true;
        }

        try {
            // minimum permissable validation is PATH_VALIDATION, since we're
            // not otherwise validating the certificate.
            CertificateValidationResult result =
                    certValidationProcessor.check(certs, CertificateValidationType.PATH_VALIDATION, null, facility, new LoggingAudit(logger));

            if (certs != null && certs.length > 0)
                KeyUsageChecker.requireActivity(KeyUsageActivity.sslServerRemote, certs[0]);

            if ( result != CertificateValidationResult.OK ) {
                throw new CertificateException("Certificate path validation and/or revocation checking failed");
            }

            if (isCartel && logger.isLoggable(Level.FINE)) {
                final String dn = certs == null || certs.length < 1 ? null : certs[0].getSubjectDN().getName();
                logger.log(Level.FINE, "SSL server cert was issued to ''{0}'' by a globally recognized CA", dn);
            }
        } catch (KeyUsageException e) {
            logger.log(Level.INFO, "Rejecting server certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        } catch (SignatureException se) {
            throw new CertificateException("Certificate path validation and/or revocation checking error", se);
        }
    }
}
