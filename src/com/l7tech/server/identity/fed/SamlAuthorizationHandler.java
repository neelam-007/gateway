/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
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
public class SamlAuthorizationHandler extends FederatedAuthorizationHandler {
    SamlAuthorizationHandler( FederatedIdentityProvider provider, TrustedCertManager trustedCertManager, ClientCertManager clientCertManager, Set certOidSet ) {
        super(provider, trustedCertManager, clientCertManager, certOidSet);
    }

    User authorize(LoginCredentials pc) throws AuthenticationException {
        if ( !providerConfig.isSamlSupported() )
            throw new BadCredentialsException("This identity provider is not configured to support SAML credentials");
        Object maybeAssertion = pc.getPayload();

        if (!(maybeAssertion instanceof SamlAssertion))
            throw new BadCredentialsException("SAML Assertion contained unsupported Subject/ConfirmationMethod");

        SamlAssertion assertion = (SamlAssertion)maybeAssertion;

        String certSubjectDn = null;
        String certIssuerDn = null;
        final X509Certificate subjectCertificate = assertion.getSubjectCertificate();
        if (subjectCertificate != null) {
            certSubjectDn = subjectCertificate.getSubjectDN().getName();
            certIssuerDn = subjectCertificate.getIssuerDN().getName();
        }
        final X509Certificate signerCertificate = assertion.getIssuerCertificate();
        String samlSignerDn = signerCertificate.getSubjectDN().getName();

        // TODO decide what to do if there is no subject cert
        // TODO decide what to do if there is no subject cert
        // TODO decide what to do if there is no subject cert
        // TODO decide what to do if there is no subject cert
        if (subjectCertificate != null && certSubjectDn != null) {
            TrustedCert samlSignerTrust = null;
            try {
                samlSignerTrust = trustedCertManager.getCachedCertBySubjectDn(samlSignerDn, MAX_CACHE_AGE);
                final String untrusted = "SAML assertion for '" + certSubjectDn + "' was signed by '" +
                        samlSignerDn + "', which is not trusted";
                if (samlSignerTrust == null) {
                    throw new BadCredentialsException(untrusted);
                } else if (!samlSignerTrust.isTrustedForSigningSamlTokens()) {
                    throw new BadCredentialsException(untrusted + " for signing SAML tokens");
                } else if (!certOidSet.contains(new Long(samlSignerTrust.getOid()))) {
                    throw new BadCredentialsException(untrusted + " for this Federated Identity Provider");
                }
            } catch ( FindException e ) {
                final String msg = "Couldn't find TrustedCert entry for assertion signer";
                logger.log( Level.SEVERE, msg, e );
                throw new AuthenticationException(msg, e);
            } catch ( Exception e ) {
                final String msg = "Couldn't decode signing certificate";
                logger.log( Level.WARNING, msg, e );
                throw new AuthenticationException(msg, e);
            }
        }

        // TODO decide what to do if there is no subject cert (and hence no issuer cert)
        // TODO decide what to do if there is no subject cert (and hence no issuer cert)
        // TODO decide what to do if there is no subject cert (and hence no issuer cert)
        // TODO decide what to do if there is no subject cert (and hence no issuer cert)
        if (subjectCertificate != null && certIssuerDn != null) {
            TrustedCert certIssuerTrust = null;
            try {
                certIssuerTrust = trustedCertManager.getCachedCertBySubjectDn(certIssuerDn, MAX_CACHE_AGE);
                if (certIssuerTrust != null) {
                    // TODO do we care whether the client cert was signed by a trusted CA in this case?
                    if (certOidSet.contains(new Long(certIssuerTrust.getOid()))) {
                        if (!certIssuerTrust.isTrustedForSigningClientCerts())
                            throw new BadCredentialsException("Subject certificate '" + certSubjectDn + "' was signed by '" +
                                                              certIssuerDn + "', which is not trusted for signing client certificates");
                        X509Certificate certIssuerCert = null;
                        try {
                            certIssuerCert = certIssuerTrust.getCertificate();
                            subjectCertificate.verify(certIssuerCert.getPublicKey());
                        } catch ( CertificateException e ) {
                            throw new AuthenticationException("Couldn't decode issuer certificate '" + samlSignerDn + "'", e);
                        } catch ( IOException e ) {
                            throw new AuthenticationException("Couldn't decode issuer certificate '" + samlSignerDn + "'", e);
                        } catch ( GeneralSecurityException e ) {
                            throw new AuthenticationException("Couldn't verify subject certificate '" + certSubjectDn + "': " + e.getMessage(), e);
                        }
                    }
                }
            } catch ( FindException e ) {
                final String msg = "Couldn't find TrustedCert entry for subject certificate signer";
                logger.log( Level.SEVERE, msg, e );
                throw new AuthenticationException(msg, e);
            } catch ( Exception e ) {
                final String msg = "Couldn't decode signing certificate";
                logger.log( Level.WARNING, msg, e );
                throw new AuthenticationException(msg, e);
            }
        }


        try {
            assertion.verifyIssuerSignature();
        } catch ( SignatureException e ) {
            String msg = "SAML Assertion Signature verification failed";
            logger.log( Level.WARNING, msg, e );
            throw new BadCredentialsException(msg,e);
        }

        // Look up by cert if there is one
        if (subjectCertificate != null && certSubjectDn != null)
            return lookupSubjectByCert(assertion, certSubjectDn, subjectCertificate);

        // No cert -- check by name identifier value, assuming it will match as a login
        // TODO fix this to work with email addresses and all the other random crap
        // TODO fix this to work with email addresses and all the other random crap
        // TODO fix this to work with email addresses and all the other random crap
        // TODO fix this to work with email addresses and all the other random crap
        final String niFormat = assertion.getNameIdentifierFormat();
        final String niValue = assertion.getNameIdentifierValue();
        final String niQualifier = assertion.getNameQualifier();

        try {
            FederatedUser u = (FederatedUser)getUserManager().findByLogin(niValue);
            if (u == null) {
                if (certOidSet.isEmpty()) return null; // Virtual groups not supported with no trusted certs
                u = createFakeUserForVirtualGroup(certSubjectDn, niFormat, niValue, niQualifier);
            }
            return u;
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't find user");
        }
    }

    private User lookupSubjectByCert(SamlAssertion assertion, String certSubjectDn, final X509Certificate subjectCertificate)
            throws AuthenticationException
    {
        final String niFormat = assertion.getNameIdentifierFormat();
        final String niValue = assertion.getNameIdentifierValue();
        final String niQualifier = assertion.getNameQualifier();
        try {
            FederatedUser u = getUserManager().findBySubjectDN(certSubjectDn);
            if (u == null) {
                if (certOidSet.isEmpty()) return null; // Virtual groups not supported with no trusted certs
                u = createFakeUserForVirtualGroup(certSubjectDn, niFormat, niValue, niQualifier);
            } else {
                // Check if this user is OK
                checkCertificateMatch(u, subjectCertificate);
                // TODO check anything else about the user?
            }

            return u;
        } catch ( FindException e ) {
            throw new AuthenticationException("Couldn't find user");
        }
    }

    private FederatedUser createFakeUserForVirtualGroup(String certSubjectDn, final String niFormat, final String niValue, final String niQualifier) throws BadCredentialsException {
        FederatedUser u;
        // Make a fake user for virtual groups
        u = new FederatedUser();
        u.setProviderId(provider.getConfig().getOid());
        u.setSubjectDn(certSubjectDn);
        if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
            u.setEmail(niValue);
        } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(niFormat)
                || SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(niFormat)) {
            u.setLogin(niValue + niQualifier !=null ? niQualifier : "");
        } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
            if (!certSubjectDn.equals(niValue)) {
                throw new BadCredentialsException("NameIdentifier '" + niValue +
                                                  "' was an X.509 SubjectName but did not match certificate's DN '" +
                                                  certSubjectDn + "'");
            }
        }
        return u;
    }

    private static final Logger logger = Logger.getLogger(X509AuthorizationHandler.class.getName());
    private static final int MAX_CACHE_AGE = 5 * 1000;
}
