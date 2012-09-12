package com.l7tech.server.transport.http;

import com.l7tech.common.io.CertUtils;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.server.identity.cert.TrustedCertServices;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateParsingException;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HostnameVerifier that uses the TrustedCertManager.
 *
 * @author Steve Jones
 */
public class SslClientHostnameVerifier implements HostnameVerifier {

    //- PUBLIC

    public SslClientHostnameVerifier( final Config config,
                                      final TrustedCertServices trustedCertServices ) {
        this( config, trustedCertServices, false );
    }

    public SslClientHostnameVerifier( final Config config,
                                      final TrustedCertServices trustedCertServices,
                                      final boolean hostnameWildcardsOnly ) {
        this.config = config;
        this.trustedCertServices = trustedCertServices;
        this.hostnameWildcardsOnly = hostnameWildcardsOnly;
    }

    /**
     * Verify that the host name is an acceptable match with the server's authentication scheme.
     *
     * @param hostname The host name to verify
     * @param sslSession The SSL session
     */
    @Override
    public boolean verify( final String hostname, final SSLSession sslSession ) {
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

    private final Config config;
    private final TrustedCertServices trustedCertServices;
    private final boolean hostnameWildcardsOnly; // When wildcards are enabled, are they only allowed for hostnames?

    private boolean doVerify( final String expectedHostname, final SSLSession sslSession ) throws SSLPeerUnverifiedException, FindException, CertificateException {
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
        String defaultVerifyHostnameTxt = config.getProperty( ServerConfigParams.PARAM_IO_BACK_HTTPS_HOST_CHECK );

        if (defaultVerifyHostnameTxt != null) {
            verify = Boolean.valueOf(defaultVerifyHostnameTxt.trim());
        }

        return !verify;
    }

    private boolean isAllowHostnameWildcards() {
        boolean allowWildcards = false;
        String allowHostnameWildcardsTxt = config.getProperty( ServerConfigParams.PARAM_IO_HOST_ALLOW_WILDCARD );

        if (allowHostnameWildcardsTxt != null) {
            allowWildcards = Boolean.valueOf(allowHostnameWildcardsTxt.trim());
        }

        return allowWildcards;
    }

    private boolean isHostnameMatch( final String expectedHostname, final X509Certificate certificate ) throws CertificateParsingException {
        if ( expectedHostname == null )
            return false;

        boolean isIPaddress = InetAddressUtil.looksLikeIpAddressV4OrV6(expectedHostname);
        Collection<String> certificateNames = CertUtils.getSubjectAlternativeNames( certificate, isIPaddress ? CertUtils.SUBJALT_NAME_TYPE_IPADDRESS : CertUtils.SUBJALT_NAME_TYPE_DNS );

        if ( certificateNames.isEmpty() ) {
            String cnValue = CertUtils.extractFirstCommonNameFromCertificate(certificate);
            if ( cnValue != null ) {
                certificateNames = Collections.singletonList( cnValue );
            }
        }

        boolean match = false;
        for ( String namePattern : certificateNames ) {
            if ( !isIPaddress && isAllowHostnameWildcards() ) {
                match = CertUtils.domainNameMatchesPattern( expectedHostname, namePattern, hostnameWildcardsOnly );
            } else {
                match = expectedHostname.equalsIgnoreCase(namePattern);
            }

            if ( match ) break;
        }

        return match;
    }

    private boolean isCertSignerTrustedWithoutHostnameVerification( final X509Certificate certificate ) throws FindException {
        String issuerDn = CertUtils.getIssuerDN(certificate);
        Collection<TrustedCert> trustedSignerCert = trustedCertServices.getCertsBySubjectDnFiltered(issuerDn, false, null, null);
        for (TrustedCert trustedCert : trustedSignerCert) {
            if (!trustedCert.isVerifyHostname() && isCertSignedByIssuer(certificate, trustedCert))
                return true;
        }
        return false;
    }

    // Return true iff. the specified certificate verifies with the specified issuerCert's public key
    private boolean isCertSignedByIssuer( final X509Certificate certificate, final TrustedCert issuerCert) {
        try {
            CertVerifier.cachedVerify(certificate, issuerCert.getCertificate());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCertDirectlyTrustedWithoutHostnameVerification( final X509Certificate certificate ) throws FindException, CertificateException {
        String subjectDn = CertUtils.getSubjectDN(certificate);
        Collection<TrustedCert> trustedCerts = trustedCertServices.getCertsBySubjectDnFiltered(subjectDn, false, null, null);
        for (TrustedCert trustedCert : trustedCerts) {
            if (!trustedCert.isVerifyHostname() && CertUtils.certsAreEqual(trustedCert.getCertificate(), certificate))
                return true;
        }
        return false;
    }
}
