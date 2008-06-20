package com.l7tech.server.transport.http;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;

/**
 * HostnameVerifier that uses the TrustedCertManager.
 *
 * @author Steve Jones
 */
public class SslClientHostnameVerifier implements HostnameVerifier {

    //- PUBLIC

    public SslClientHostnameVerifier(final ServerConfig serverConfig, final TrustedCertManager trustedCertManager) {
        this.serverConfig = serverConfig;
        this.trustedCertManager = trustedCertManager;
    }

    /**
     * Verify that the host name is an acceptable match with the server's authentication scheme.
     *
     * @param hostname The host name to verify
     * @param sslSession The SSL session
     */
    public boolean verify(String hostname, SSLSession sslSession) {
        boolean verified = false;

        if (hostname != null && sslSession != null) {
            try {
                Certificate[] certChain = sslSession.getPeerCertificates();
                if (certChain.length > 0 && certChain[0] instanceof X509Certificate) {
                    X509Certificate certificate = (X509Certificate) certChain[0];

                    // see if if just works ...
                    String expectedHost = CertUtils.getCn(certificate);
                    if (expectedHost != null && expectedHost.equalsIgnoreCase(hostname)) {
                        verified = true;
                    } else {
                        // check name against trusted certs
                        String subjectDn = certificate.getSubjectDN().getName();
                        TrustedCert trustedCert = trustedCertManager.getCachedCertBySubjectDn(subjectDn, 30000);
                        if (trustedCert != null &&
                                CertUtils.certsAreEqual(trustedCert.getCertificate(), certificate) &&
                                !trustedCert.isVerifyHostname()) {
                            verified = true;
                        } else {
                            // see if this is signed by a trusted cert
                            String issuerDn = certificate.getIssuerDN().getName();
                            TrustedCert trustedSignerCert =
                                trustedCertManager.getCachedCertBySubjectDn(issuerDn, 30000);
                            if ( trustedSignerCert != null &&
                                 !trustedSignerCert.isVerifyHostname()) {
                                try {
                                    CertUtils.cachedVerify( certificate, trustedSignerCert.getCertificate().getPublicKey() );
                                    verified = true;
                                } catch (Exception e) {
                                    verified = !isDefaultVerifyHostname();
                                }
                            } else {
                                verified = !isDefaultVerifyHostname();
                            }
                        }
                    }
                } else {
                    verified = !isDefaultVerifyHostname();
                }
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Could not verify hostname '"+hostname+"'.", e);
            }
        } else {
            verified = !isDefaultVerifyHostname();
        }

        return verified;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SslClientHostnameVerifier.class.getName());

    private final ServerConfig serverConfig;
    private final TrustedCertManager trustedCertManager;

    private boolean isDefaultVerifyHostname() {
        boolean verify = true;
        String defaultVerifyHostnameTxt =
                serverConfig.getPropertyCached(ServerConfig.PARAM_IO_BACK_HTTPS_HOST_CHECK, 30000);

        if (defaultVerifyHostnameTxt != null) {
            verify = Boolean.valueOf(defaultVerifyHostnameTxt.trim());
        }

        return verify;
    }
}
