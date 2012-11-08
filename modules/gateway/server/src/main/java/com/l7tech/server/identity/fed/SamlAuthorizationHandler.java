/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.xml.saml.SamlAssertion;

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
class SamlAuthorizationHandler extends FederatedAuthorizationHandler {
    SamlAuthorizationHandler(FederatedIdentityProvider provider,
                             TrustedCertServices trustedCertServices,
                             ClientCertManager clientCertManager,
                             CertValidationProcessor certValidationProcessor,
                             Auditor auditor,
                             Set certOidSet) {
        super(provider, trustedCertServices, clientCertManager, certValidationProcessor, auditor, certOidSet);
    }

    User authorize(LoginCredentials pc) throws AuthenticationException {
        if (!providerConfig.isSamlSupported())
            throw new BadCredentialsException("This identity provider is not configured to support SAML credentials");
        Object maybeAssertion = pc.getSecurityToken();

        if (!(maybeAssertion instanceof SamlAssertion))
            throw new BadCredentialsException("SAML Assertion contained unsupported Subject/ConfirmationMethod");

        SamlAssertion assertion = (SamlAssertion)maybeAssertion;

        String certSubjectDn = null;
        String certIssuerDn = null;
        final X509Certificate subjectCertificate = assertion.getSubjectCertificate();
        if (subjectCertificate != null) {
            certSubjectDn = CertUtils.getSubjectDN( subjectCertificate );
            certIssuerDn = CertUtils.getIssuerDN( subjectCertificate );
        }

        final X509Certificate signerCertificate = assertion.getIssuerCertificate();
        if (signerCertificate == null) {
            final String assertionUnsigned = "SAML assertion for '" + certSubjectDn + "' was not signed by any issuer.";
            throw new BadCredentialsException(assertionUnsigned);
        }

        String samlSignerDn = CertUtils.getSubjectDN( signerCertificate );

        // check if the SAML Assertion signer is trusted
        checkSamlAssertionSignerTrusted(signerCertificate, samlSignerDn);

        // attesting entity in Sender-Vouches
        if (assertion.isSenderVouches()) {
            final X509Certificate attestingEntityCertificate = assertion.getAttestingEntity();
            if (attestingEntityCertificate == null) {
                throw new AuthenticationException("The Attesting Entity Certificate is required, but not presented");
            }
            // Check only if attesting entity cert and the SAML authority differ
            // SAML authorities are trusted as attesting entities
            if (!CertUtils.certsAreEqual(attestingEntityCertificate, signerCertificate)) {
                checkAttestingEntityTrusted(attestingEntityCertificate);
            }
        }

        // if there is a subject cert, check if the CA (cert issuer) is trusted
        if (subjectCertificate != null && certIssuerDn != null) {
            try {
                Collection<TrustedCert> certIssuerTrusts = trustedCertServices.getCertsBySubjectDnFiltered(certIssuerDn, true, EnumSet.of(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS), certOidSet);
                for (TrustedCert certIssuerTrust : certIssuerTrusts) {
                    // If we happen to recognize this issuer cert, ensure that its marked as trusted for signing client certs
                    // TODO do we care whether the client cert was signed by a trusted CA in this case?
                    X509Certificate certIssuerCert;
                    try {
                        certIssuerCert = certIssuerTrust.getCertificate();
                        CertVerifier.cachedVerify(subjectCertificate, certIssuerCert);
                        validateCertificate( subjectCertificate, true );
                    } catch (CertificateException e) {
                        throw new AuthenticationException("Couldn't decode issuer certificate '" + samlSignerDn + "'", e);
                    } catch (GeneralSecurityException e) {
                        throw new AuthenticationException("Couldn't verify subject certificate '" + certSubjectDn + "': " + e.getMessage(), e);
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
            FederatedUser u;
            if (SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(niFormat) || niFormat == null) {
                u = getUserManager().findBySubjectDN(CertUtils.formatDN(niValue));
                if (u == null) u = getUserManager().findByEmail(niValue);
                if (u == null) u = getUserManager().findByLogin(niValue);
            } else if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat)) {
                u = getUserManager().findByEmail(niValue);
            } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat)) {
                u = getUserManager().findBySubjectDN(CertUtils.formatDN(niValue));
            } else {
                u = getUserManager().findByLogin(niValue);
            }

            // Virtual users are not supported unless there are trusted certs
            if ( u == null && !certOidSet.isEmpty() ) {
                if ( certSubjectDn == null ) { ///no subject cert, but there was a DN
                    if ( SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat) ) {
                        u = createVirtualUser(niValue, niFormat, niValue);
                    } else {
                        u = createVirtualUser(null, niFormat, niValue);
                    }
                } else {
                    u = createVirtualUser(certSubjectDn, niFormat, niValue);
                }
            }
            
            return u;
        } catch (FindException e) {
            throw new AuthenticationException("Couldn't find user");
        }
    }

    private void checkSamlAssertionSignerTrusted(X509Certificate signerCertificate, String samlSignerDn) throws AuthenticationException {
        try {
            Collection<TrustedCert> samlSignerTrusts = trustedCertServices.getCertsBySubjectDnFiltered(samlSignerDn, true, EnumSet.of(TrustedCert.TrustedFor.SAML_ISSUER), certOidSet);
            for (TrustedCert samlSignerTrust : samlSignerTrusts) {
                if (CertUtils.certsAreEqual(signerCertificate, samlSignerTrust.getCertificate())) {
                    validateCertificate(signerCertificate, false );
                    return;
                }
            }

            throw new BadCredentialsException("SAML assertion  was signed by '" + samlSignerDn +
                    "', which is unrecognized or not trusted" + " for signing SAML tokens for this Federated Identity Provider");
        } catch (FindException e) {
            final String msg = "Couldn't find TrustedCert entry for assertion signer";
            logger.log(Level.SEVERE, msg, e);
            throw new AuthenticationException(msg, e);
        }
    }

    private void checkAttestingEntityTrusted(X509Certificate attestingEntityCertificate) throws AuthenticationException {
        try {
            String attestingEntityDN = CertUtils.getSubjectDN(attestingEntityCertificate);
            Collection<TrustedCert> attestingEntityCertificateTrusts = trustedCertServices.getCertsBySubjectDnFiltered(attestingEntityDN, true, EnumSet.of(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY), certOidSet);
            for (TrustedCert certificateTrust : attestingEntityCertificateTrusts) {
                if (CertUtils.certsAreEqual(attestingEntityCertificate, certificateTrust.getCertificate()))
                {
                    validateCertificate(attestingEntityCertificate, false );
                    return;
                }
            }

            throw new BadCredentialsException("The certificate '" + attestingEntityDN + " is not recognized or not trusted as Attesting Entity" +
                    " for this Federated Identity Provider");
        } catch (FindException e) {
            final String msg = "Couldn't find TrustedCert entry for Attesting Entity Certificate";
            logger.log(Level.SEVERE, msg, e);
            throw new AuthenticationException(msg, e);
        }
    }

    private User lookupSubjectByCert(SamlAssertion assertion, String certSubjectDn, final X509Certificate subjectCertificate)
      throws AuthenticationException {
        final String niFormat = assertion.getNameIdentifierFormat();
        final String niValue = assertion.getNameIdentifierValue();
        try {
            FederatedUser u = getUserManager().findBySubjectDN(CertUtils.formatDN(certSubjectDn));
            if (u == null) {
                if (certOidSet.isEmpty()) return null; // Virtual users not supported with no trusted certs
                u = createVirtualUser(certSubjectDn, niFormat, niValue);
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

    /**
     * Create a virtual user for use with virtual groups or authentication against this provider.
     *
     * <p>For virtual groups, the user must have an email address or subject DN,
     * the name identifier format is not relevant for authentication against the
     * provider.</p>
     */
    private FederatedUser createVirtualUser( final String certSubjectDn,
                                             final String niFormat,
                                             final String niValue ) throws BadCredentialsException {
        if ( niValue == null ) {
            throw new BadCredentialsException("Subject Name Identifier is required");
        }

        final FederatedUser u = new FederatedUser(provider.getConfig().getOid(), null);
        u.setSubjectDn(certSubjectDn);

        if ( SamlConstants.NAMEIDENTIFIER_EMAIL.equals(niFormat) ) {
            u.setEmail(niValue);
        } else if ( SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(niFormat) ) {
            if (certSubjectDn == null) {
                throw new BadCredentialsException("Name Identifier Format is " + SamlConstants.NAMEIDENTIFIER_X509_SUBJECT + " but the value is null");
            }
            if (!niValue.equals(certSubjectDn)) {
                throw new BadCredentialsException("NameIdentifier '" + niValue +
                  "' was an X.509 SubjectName but did not match certificate's DN '" +
                  certSubjectDn + "'");
            }
        } else {
            u.setLogin(niValue);
        }

        return u;
    }

    private static final Logger logger = Logger.getLogger(X509AuthorizationHandler.class.getName());
}
