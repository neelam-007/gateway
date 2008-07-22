/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.identity.fed;

import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.common.io.CertUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.security.cert.CertValidationProcessor;

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SamlAuthorizationHandler extends FederatedAuthorizationHandler {
    SamlAuthorizationHandler(FederatedIdentityProvider provider,
                             TrustedCertManager trustedCertManager,
                             ClientCertManager clientCertManager,
                             CertValidationProcessor certValidationProcessor,
                             Auditor auditor,
                             Set certOidSet) {
        super(provider, trustedCertManager, clientCertManager, certValidationProcessor, auditor, certOidSet);
    }

    User authorize(LoginCredentials pc) throws AuthenticationException {
        if (!providerConfig.isSamlSupported())
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
        if (signerCertificate == null) {
            final String assertionUnsigned = "SAML assertion for '" + certSubjectDn + "' was not signed by any issuer.";
            throw new BadCredentialsException(assertionUnsigned);
        }

        String samlSignerDn = signerCertificate.getSubjectDN().getName();

        // check if the SAML Assertion signer is trusted
        try {
            TrustedCert samlSignerTrust = trustedCertManager.getCachedCertBySubjectDn(samlSignerDn, MAX_CACHE_AGE);
            final String untrusted = "SAML assertion  was signed by '" + samlSignerDn + "', which is not trusted";
            if (samlSignerTrust == null) {
                throw new BadCredentialsException(untrusted);
            } else if (!CertUtils.certsAreEqual(signerCertificate, samlSignerTrust.getCertificate())) {
                throw new BadCredentialsException(untrusted + " because the cert has changed");
            } else if (!samlSignerTrust.isTrustedAsSamlIssuer()) {
                throw new BadCredentialsException(untrusted + " for signing SAML tokens");
            } else if (!certOidSet.contains(new Long(samlSignerTrust.getOid()))) {
                throw new BadCredentialsException(untrusted + " for this Federated Identity Provider");
            }

            validateCertificate( signerCertificate, false );                    
        } catch (FindException e) {
            final String msg = "Couldn't find TrustedCert entry for assertion signer";
            logger.log(Level.SEVERE, msg, e);
            throw new AuthenticationException(msg, e);
        } catch (CertificateException e) {
            final String msg = "Couldn't decode signing certificate";
            logger.log(Level.WARNING, msg, e);
            throw new AuthenticationException(msg, e);
        }

        // attesting entity in Sender-Vouches
        if (assertion.isSenderVouches()) {
            final X509Certificate attestingEntityCertificate = assertion.getAttestingEntity();
            if (attestingEntityCertificate == null) {
                throw new AuthenticationException("The Attesting Entity Certificate is required, but not presented");
            }
            try {
                // Check only if attesting entity cert and the SAML authority differ
                // SAML authorities are trusted as attesting entities
                if (!CertUtils.certsAreEqual(attestingEntityCertificate, signerCertificate)) {
                    String attestingEntityDN = attestingEntityCertificate.getSubjectDN().getName();
                    TrustedCert attestingEntityCertificateTrust = trustedCertManager.getCachedCertBySubjectDn(attestingEntityDN, MAX_CACHE_AGE);
                    if (attestingEntityCertificateTrust == null) {
                        String msg = "The certificate '" + attestingEntityDN + "', is not trusted as Attesting Entity";
                        logger.log(Level.WARNING, msg);
                        throw new BadCredentialsException(msg);
                    }
                    if (!CertUtils.certsAreEqual(attestingEntityCertificate, attestingEntityCertificateTrust.getCertificate())) {
                        String msg = "Attesting Entity '" + attestingEntityDN + "' is not trusted  because the cert has changed";
                        logger.log(Level.WARNING, msg);
                        throw new BadCredentialsException(msg);
                    }
                    if (!certOidSet.contains(new Long(attestingEntityCertificateTrust.getOid()))) {
                        throw new BadCredentialsException("The certificate '" + attestingEntityDN + " is not trusted as Attesting Entity" + " for this Federated Identity Provider");
                    }

                    validateCertificate( attestingEntityCertificate, false );                    
                }
            } catch (FindException e) {
                final String msg = "Couldn't find TrustedCert entry for Attesting Entity Certificate";
                logger.log(Level.SEVERE, msg, e);
                throw new AuthenticationException(msg, e);
            } catch (CertificateException e) {
                final String msg = "Couldn't decode signing Attesting Entity Certificate";
                logger.log(Level.WARNING, msg, e);
                throw new AuthenticationException(msg, e);
            }
        }

        // if there is a subject cert, check if the CA (cert issuer) is trusted
        if (subjectCertificate != null && certIssuerDn != null) {
            TrustedCert certIssuerTrust;
            try {
                certIssuerTrust = trustedCertManager.getCachedCertBySubjectDn(certIssuerDn, MAX_CACHE_AGE);
                if (certIssuerTrust != null) {
                    // TODO do we care whether the client cert was signed by a trusted CA in this case?
                    if (certOidSet.contains(new Long(certIssuerTrust.getOid()))) {
                        if (!certIssuerTrust.isTrustedForSigningClientCerts())
                            throw new BadCredentialsException("Subject certificate '" + certSubjectDn + "' was signed by '" +
                              certIssuerDn + "', which is not trusted for signing client certificates");
                        X509Certificate certIssuerCert;
                        try {
                            certIssuerCert = certIssuerTrust.getCertificate();
                            CertUtils.cachedVerify(subjectCertificate, certIssuerCert.getPublicKey());
                            validateCertificate( subjectCertificate, true );                    
                        } catch (CertificateException e) {
                            throw new AuthenticationException("Couldn't decode issuer certificate '" + samlSignerDn + "'", e);
                        } catch (GeneralSecurityException e) {
                            throw new AuthenticationException("Couldn't verify subject certificate '" + certSubjectDn + "': " + e.getMessage(), e);
                        }
                    }
                }
            } catch (FindException e) {
                final String msg = "Couldn't find TrustedCert entry for subject certificate signer";
                logger.log(Level.SEVERE, msg, e);
                throw new AuthenticationException(msg, e);
            } catch (Exception e) {
                final String msg = "Couldn't decode signing certificate";
                logger.log(Level.WARNING, msg, e);
                throw new AuthenticationException(msg, e);
            }
        }

        // Look up by cert if there is one
        if (subjectCertificate != null && certSubjectDn != null)
            return lookupSubjectByCert(assertion, certSubjectDn, subjectCertificate);

        // No cert -- check by name identifier value, assuming it will match as a login or email
        final String niFormat = assertion.getNameIdentifierFormat();
        final String niValue = assertion.getNameIdentifierValue();

        try {
            FederatedUser u = null;
            if (SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(niFormat) || niFormat == null) {
                u = getUserManager().findBySubjectDN(niValue);
                if (u == null) u = getUserManager().findByEmail(niValue);
                if (u == null) u = getUserManager().findByLogin(niValue);
            } else if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
                u = getUserManager().findByEmail(niValue);
            } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
                u = getUserManager().findBySubjectDN(niValue);
            } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(niFormat)) {
                u = getUserManager().findByLogin(niValue);
            }
            if (u == null) {
                if (certOidSet.isEmpty()) return null; // Virtual groups not supported with no trusted certs
                if (certSubjectDn == null) { ///no subject cert, but there was a DN
                    // Virtual groups only match email or DN
                    if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
                        u = createFakeUserForVirtualGroup(null, niFormat, niValue);
                    }
                    else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
                        u = createFakeUserForVirtualGroup(niValue, niFormat, niValue);
                    }
                } else {
                    u = createFakeUserForVirtualGroup(certSubjectDn, niFormat, niValue);
                }
            }
            return u;
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't find user");
        }
    }

    private User lookupSubjectByCert(SamlAssertion assertion, String certSubjectDn, final X509Certificate subjectCertificate)
      throws AuthenticationException {
        final String niFormat = assertion.getNameIdentifierFormat();
        final String niValue = assertion.getNameIdentifierValue();
        try {
            FederatedUser u = getUserManager().findBySubjectDN(certSubjectDn);
            if (u == null) {
                if (certOidSet.isEmpty()) return null; // Virtual groups not supported with no trusted certs
                u = createFakeUserForVirtualGroup(certSubjectDn, niFormat, niValue);
            } else {
                // Check if this user is OK
                checkCertificateMatch(u, subjectCertificate);
                // TODO check anything else about the user?
            }

            return u;
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't find user");
        }
    }

    private FederatedUser createFakeUserForVirtualGroup(String certSubjectDn, final String niFormat, final String niValue)
      throws BadCredentialsException {
        if (niValue == null) {
            throw new BadCredentialsException("Subject Name Identifier is required");
        }
        FederatedUser u;
        // Make a fake user for virtual groups
        u = new FederatedUser(provider.getConfig().getOid(), null);
        u.setSubjectDn(certSubjectDn);
        if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
            u.setEmail(niValue);
        } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(niFormat)
          || SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(niFormat)) {
            u.setLogin(niValue);
        } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
            if (certSubjectDn == null) {
                throw new BadCredentialsException("Name Identifier Format is " + SamlConstants.NAMEIDENTIFIER_X509_SUBJECT + " but the value is null");
            }
            if (!niValue.equals(certSubjectDn)) {
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
