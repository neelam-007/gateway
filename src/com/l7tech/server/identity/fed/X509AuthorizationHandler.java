/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.X509Config;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class X509AuthorizationHandler extends FederatedAuthorizationHandler {
    X509AuthorizationHandler( FederatedIdentityProvider provider, TrustedCertManager trustedCertManager, ClientCertManager clientCertManager, Set certOidSet ) {
        super(provider, trustedCertManager, clientCertManager, certOidSet);
    }

    User authorize( LoginCredentials pc ) throws IOException, AuthenticationException, FindException {
        if ( !providerConfig.isX509Supported() )
            throw new BadCredentialsException("This identity provider is not configured to support X.509 credentials");

        final X509Config x509Config = providerConfig.getX509Config();
        if (x509Config == null) throw new AuthenticationException("X.509 enabled but not configured");

        X509Certificate requestCert = pc.getClientCert();
        if (requestCert == null) {
            logger.info("Can only authorize credentials that include a certificate");
            return null;
        }
        String subjectDn = requestCert.getSubjectDN().getName();
        String issuerDn = requestCert.getIssuerDN().getName();

        if ( !certOidSet.isEmpty() ) {
            // There could be no trusted certs--this means that specific client certs
            // are trusted no matter who signed them

            try {
                TrustedCert trustedCert = trustedCertManager.getCachedCertBySubjectDn( issuerDn, MAX_CACHE_AGE );

                if ( trustedCert == null ) throw new BadCredentialsException("Signer '" + issuerDn + "' is not trusted");

                final String untrusted = "The trusted certificate with DN '" + trustedCert.getSubjectDn() + "' is not trusted";

                if ( !certOidSet.contains(new Long(trustedCert.getOid())) )
                    throw new BadCredentialsException(untrusted + " by this identity provider");

                if ( !trustedCert.isTrustedForSigningClientCerts() )
                    throw new BadCredentialsException(untrusted + " for signing client certificates");

                try {
                    CertificateExpiry exp = CertUtils.checkValidity(trustedCert.getCertificate());
                    if (exp.getDays() <= CertificateExpiry.FINE_DAYS)
                        trustedCertManager.logWillExpire(trustedCert, exp);
                } catch ( CertificateException e ) {
                    final String msg = "Trusted cert for " + trustedCert.getSubjectDn() + " is invalid or corrupted: " + e.getMessage();
                    throw new AuthenticationException(msg, e);
                }

                // Check that cert was signed by CA key
                requestCert.verify(trustedCert.getCertificate().getPublicKey());
            } catch ( CertificateException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
                throw new BadCredentialsException(e.getMessage(), e);
            } catch ( NoSuchAlgorithmException e ) {
                logger.log( Level.SEVERE, e.getMessage(), e );
                throw new BadCredentialsException(e.getMessage(), e);
            } catch ( InvalidKeyException e ) {
                logger.log( Level.SEVERE, e.getMessage(), e );
                throw new BadCredentialsException(e.getMessage(), e);
            } catch ( NoSuchProviderException e ) {
                logger.log( Level.SEVERE, e.getMessage(), e );
                throw new BadCredentialsException(e.getMessage(), e);
            } catch ( SignatureException e ) {
                logger.log( Level.SEVERE, e.getMessage(), e );
                throw new BadCredentialsException(e.getMessage(), e);
            }
        }

        FederatedUser u = userManager.findBySubjectDN(subjectDn);
        if (u == null) {
            if (certOidSet.isEmpty()) {
                logger.fine("No Federated User with DN = '" + subjectDn + "' could be found, and virtual groups" +
                            " are not permitted without trusted certs");
                return null;
            }
        } else {
            checkCertificateMatch( u, requestCert );
        }

        final Class csa = pc.getCredentialSourceAssertion();

        if (csa == null) {
            logger.info("credential source assertion is not set but required by the federated id provider.");
        }

        if ( ( x509Config.isWssBinarySecurityToken() && csa == RequestWssX509Cert.class ) ||
             ( x509Config.isSslClientCert() && csa == HttpClientCert.class )) {
            if ( u == null ) {
                // Make a fake user so that a VirtualGroup can still resolve it
                u = new FederatedUser();
                u.setSubjectDn(subjectDn);
            }
            return u;
        }

        throw new BadCredentialsException("Federated IDP " + providerConfig.getName() + "(" + providerConfig.getOid() +
                                          ") is not configured to trust certificates found with " +
                                          csa.getName() );
    }

    private static final Logger logger = Logger.getLogger(X509AuthorizationHandler.class.getName());
    private static final int MAX_CACHE_AGE = 5 * 1000;
}
