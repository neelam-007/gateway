package com.l7tech.server.identity.fed;

import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.X509Config;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ConfigFactory;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author alex
 */
class X509AuthorizationHandler extends FederatedAuthorizationHandler {
    private static final boolean ALLOW_SELF_SIGNED_TRUSTED_CERT = ConfigFactory.getBooleanProperty( X509AuthorizationHandler.class.getName() + ".allowSelfSigned", false );

    X509AuthorizationHandler(FederatedIdentityProvider provider,
                             TrustedCertServices trustedCertServices,
                             ClientCertManager clientCertManager,
                             CertValidationProcessor certValidationProcessor,
                             Auditor auditor,
                             Set certOidSet) {
        super(provider, trustedCertServices, clientCertManager, certValidationProcessor, auditor, certOidSet);
    }

    User authorize( LoginCredentials pc ) throws AuthenticationException, FindException {
        if ( !providerConfig.isX509Supported() )
            throw new BadCredentialsException("This identity provider is not configured to support X.509 credentials");

        final X509Config x509Config = providerConfig.getX509Config();
        if (x509Config == null) throw new AuthenticationException("X.509 enabled but not configured");

        X509Certificate requestCert = pc.getClientCert();
        if (requestCert == null) {
            throw new MissingCredentialsException("Can only authorize credentials that include a certificate");
        }
        String subjectDn = CertUtils.getSubjectDN( requestCert );
        String issuerDn = CertUtils.getIssuerDN( requestCert );

        if ( !certOidSet.isEmpty() ) {
            // There could be no trusted certs--this means that specific client certs
            // are trusted no matter who signed them

            verifyAgainstCertOidSet(requestCert, issuerDn);
        }

        FederatedUser u = getUserManager().findBySubjectDN(subjectDn);
        if (u == null) {
            if (certOidSet.isEmpty()) {
                logger.fine("No Federated User with DN = '" + subjectDn + "' could be found, and virtual groups" +
                            " are not permitted without trusted certs");
                return null;
            }
        } else {
            checkCertificateMatch( u, requestCert );
        }

        validateCertificate( requestCert, true );

        if ( u == null ) {
            // Make a fake user so that a VirtualGroup can still resolve it
            u = new FederatedUser(providerConfig.getGoid(), null);
            u.setSubjectDn(subjectDn);
        }
        
        return u;
    }

    private void verifyAgainstCertOidSet(X509Certificate requestCert, String issuerDn) throws FindException, AuthenticationException {
        Collection<TrustedCert> trustedCerts = trustedCertServices.getCertsBySubjectDnFiltered(issuerDn, true, EnumSet.of(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS), certOidSet);
        if (trustedCerts.isEmpty())
            throw new BadCredentialsException("Signer '" + issuerDn + "' is not trusted");

        // This check prevents use of a self signed trusted certificate as a user certificate
        // See bug 7257
        if ( !ALLOW_SELF_SIGNED_TRUSTED_CERT ) {
            for (TrustedCert trustedCert : trustedCerts) {
                final X509Certificate trustedX509 = trustedCert.getCertificate();
                final boolean selfSigned = trustedX509.getSubjectX500Principal().equals(trustedX509.getIssuerX500Principal());
                if ( selfSigned && CertUtils.certsAreEqual( trustedX509, requestCert ) ) {
                    throw new BadCredentialsException("Unable to authenticate certificate: request certificate is (self signed) trusted certificate");
                }
            }
        }

        for (TrustedCert trustedCert : trustedCerts) {
            final X509Certificate trustedX509 = trustedCert.getCertificate();
            if (CertVerifier.isVerified(requestCert, trustedX509))
                return;
        }

        throw new BadCredentialsException("Unable to authenticate certificate: no matching valid trusted certificate that is trusted for signing client certificates");
    }

    private static final Logger logger = Logger.getLogger(X509AuthorizationHandler.class.getName());
}
