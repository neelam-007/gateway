/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.SamlConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.saml.SamlHolderOfKeyAssertion;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SamlAuthorizationHandler extends FederatedAuthorizationHandler {
    SamlAuthorizationHandler( FederatedIdentityProvider provider, TrustedCertManager trustedCertManager, ClientCertManager clientCertManager, Set certOidSet ) {
        super(provider, trustedCertManager, clientCertManager, certOidSet);
    }

    User authorize(LoginCredentials pc) throws AuthenticationException {
        if ( !providerConfig.isSamlSupported() )
            throw new BadCredentialsException("This identity provider is not configured to support SAML credentials");
        final SamlConfig samlConfig = providerConfig.getSamlConfig();
        if (samlConfig == null) throw new AuthenticationException("SAML enabled but not configured");
        Object maybeAssertion = pc.getPayload();

        if (maybeAssertion instanceof SamlHolderOfKeyAssertion) {
            SamlHolderOfKeyAssertion assertion = (SamlHolderOfKeyAssertion)maybeAssertion;

            final X509Certificate subjectCertificate = assertion.getSubjectCertificate();
            String subjectDn = subjectCertificate.getSubjectDN().getName();
            String issuerDn = assertion.getSubjectCertificate().getIssuerDN().getName();

            TrustedCert issuerTrust = null;
            try {
                issuerTrust = trustedCertManager.getCachedCertBySubjectDn(issuerDn, MAX_CACHE_AGE);
                final String untrusted = "Subject certificate '" + subjectDn + "' was signed by '" +
                                         issuerDn + "', which is not trusted";
                if (issuerTrust == null) {
                    throw new BadCredentialsException(untrusted);
                } else if (!issuerTrust.isTrustedForSigningSamlTokens()) {
                    throw new BadCredentialsException(untrusted + " for signing SAML tokens");
                } else if (!certOidSet.contains(new Long(issuerTrust.getOid()))) {
                    throw new BadCredentialsException(untrusted + " for this Federated Identity Provider");
                }
            } catch ( FindException e ) {
                logger.log( Level.INFO, e.getMessage(), e );
            } catch ( IOException e ) {
                logger.log( Level.INFO, e.getMessage(), e );
            } catch ( CertificateException e ) {
                logger.log( Level.INFO, e.getMessage(), e );
            }

            X509Certificate issuerCert = null;
            try {
                issuerCert = issuerTrust.getCertificate();
                subjectCertificate.verify(issuerCert.getPublicKey());
            } catch ( CertificateException e ) {
                throw new AuthenticationException("Couldn't decode issuer certificate '" + issuerDn + "'", e);
            } catch ( IOException e ) {
                throw new AuthenticationException("Couldn't decode issuer certificate '" + issuerDn + "'", e);
            } catch ( GeneralSecurityException e ) {
                throw new AuthenticationException("Couldn't verify subject certificate '" + issuerDn + "'", e);
            }

            final String niFormat = assertion.getNameIdentifierFormat();
            final String niValue = assertion.getNameIdentifierValue();
            final String niQualifier = assertion.getNameQualifier();
            final String configNameQualifier = samlConfig.getNameQualifier();

            if (configNameQualifier != null) {
                // Make sure NameQualifier matches, if specified
                if (niQualifier == null) {
                    throw new BadCredentialsException("NameQualifier '" + configNameQualifier +
                                                      "' is required but not present");
                } else if (!niQualifier.equals(configNameQualifier)) {
                    throw new BadCredentialsException("SAML Assertion's NameQualifier '" + niQualifier +
                                                      "' does not match configured value '" + configNameQualifier + "'");
                }
            }

            final String configDomain = samlConfig.getNameIdWindowsDomainName();
            if (configDomain != null) {
                // TODO
                throw new BadCredentialsException("Domain '" + configDomain + "' required but not present");
            }

            try {
                FederatedUser u = userManager.findBySubjectDN(subjectDn);
                if (u == null) {
                    if (certOidSet.isEmpty()) return null; // Virtual groups not supported with no trusted certs

                    // Make a fake user for virtual groups
                    u = new FederatedUser();
                    u.setProviderId(provider.getConfig().getOid());
                    u.setSubjectDn(subjectDn);
                    if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
                        u.setEmail(niValue);
                    } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(niFormat)) {
                        u.setLogin(niValue);
                    } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
                        if (!subjectDn.equals(niValue)) {
                            throw new BadCredentialsException("NameIdentifier '" + niValue +
                                                              "' was an X.509 SubjectName but did not match certificate's DN '" +
                                                              subjectDn + "'");
                        }
                    }
                } else {
                    // Check if this user is OK
                    checkCertificateMatch(u, subjectCertificate);
                    // TODO check anything else about the user?
                }

                return u;
            } catch ( FindException e ) {
                throw new AuthenticationException("Couldn't find user");
            }
        } else {
            throw new BadCredentialsException("SAML Assertion contained unsupported Subject/ConfirmationMethod");
        }
    }

    private static final Logger logger = Logger.getLogger(X509AuthorizationHandler.class.getName());
    private static final int MAX_CACHE_AGE = 5 * 1000;
}
