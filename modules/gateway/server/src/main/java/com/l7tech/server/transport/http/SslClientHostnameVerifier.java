package com.l7tech.server.transport.http;

import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.identity.cert.TrustedCertServices;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HostnameVerifier that uses the TrustedCertManager.
 *
 * @author Steve Jones
 */
public class SslClientHostnameVerifier implements HostnameVerifier {

    //- PUBLIC

    public SslClientHostnameVerifier(final ServerConfig serverConfig, final TrustedCertServices trustedCertServices) {
        this.serverConfig = serverConfig;
        this.trustedCertServices = trustedCertServices;
    }

    /**
     * Verify that the host name is an acceptable match with the server's authentication scheme.
     *
     * @param hostname The host name to verify
     * @param sslSession The SSL session
     */
    public boolean verify(String hostname, SSLSession sslSession) {

        if (hostname == null || sslSession == null)
            return isSkipHostnameVerificationByDefault();

        try {
            return doVerify(hostname, sslSession);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not verify hostname '"+hostname+"'.", e);
            return isSkipHostnameVerificationByDefault();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SslClientHostnameVerifier.class.getName());

    private final ServerConfig serverConfig;
    private final TrustedCertServices trustedCertServices;

    private boolean doVerify(String expectedHostname, SSLSession sslSession) throws SSLPeerUnverifiedException, FindException, CertificateException {
        Certificate[] certChain = sslSession.getPeerCertificates();
        if (certChain.length < 1 || !(certChain[0] instanceof X509Certificate))
            return isSkipHostnameVerificationByDefault();

        X509Certificate certificate = (X509Certificate)certChain[0];

        return isHostnameMatch(expectedHostname, certificate) ||
               isCertDirectlyTrustedWithoutHostnameVerification(certificate) ||
               isCertSignerTrustedWithoutHostnameVerification(certificate) ||
               isSkipHostnameVerificationByDefault();
    }

    private boolean isSkipHostnameVerificationByDefault() {
        boolean verify = true;
        String defaultVerifyHostnameTxt = serverConfig.getPropertyCached(ServerConfig.PARAM_IO_BACK_HTTPS_HOST_CHECK, 30000);

        if (defaultVerifyHostnameTxt != null) {
            verify = Boolean.valueOf(defaultVerifyHostnameTxt.trim());
        }

        return !verify;
    }

    private boolean isHostnameMatch(String expectedHostname, X509Certificate certificate) {
        if (expectedHostname == null)
            return false;
        List<String> cnValues = CertUtils.extractCommonNamesFromCertificate(certificate);
        for (String cnValue : cnValues) {
            if (expectedHostname.equalsIgnoreCase(cnValue))
                return true;
        }
        return false;
    }

    private boolean isCertSignerTrustedWithoutHostnameVerification(X509Certificate certificate) throws FindException {
        String issuerDn = certificate.getIssuerDN().getName();
        Collection<TrustedCert> trustedSignerCert = trustedCertServices.getCertsBySubjectDnFiltered(issuerDn, false, null, null);
        for (TrustedCert trustedCert : trustedSignerCert) {
            if (!trustedCert.isVerifyHostname() && isCertSignedByIssuer(certificate, trustedCert))
                return true;
        }
        return false;
    }

    // Return true iff. the specified certificate verifies with the specified issuerCert's public key
    private boolean isCertSignedByIssuer(X509Certificate certificate, TrustedCert issuerCert) {
        try {
            CertUtils.cachedVerify(certificate, issuerCert.getCertificate().getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCertDirectlyTrustedWithoutHostnameVerification(X509Certificate certificate) throws FindException, CertificateException {
        String subjectDn = certificate.getSubjectDN().getName();
        Collection<TrustedCert> trustedCerts = trustedCertServices.getCertsBySubjectDnFiltered(subjectDn, false, null, null);
        for (TrustedCert trustedCert : trustedCerts) {
            if (!trustedCert.isVerifyHostname() && CertUtils.certsAreEqual(trustedCert.getCertificate(), certificate))
                return true;
        }
        return false;
    }
}
