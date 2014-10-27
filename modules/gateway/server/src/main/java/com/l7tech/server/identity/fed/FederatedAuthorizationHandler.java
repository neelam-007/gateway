/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.security.cert.CertValidationProcessor;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
class FederatedAuthorizationHandler {
    
    FederatedAuthorizationHandler(final FederatedIdentityProvider provider,
                                  final TrustedCertServices trustedCertServices,
                                  final ClientCertManager clientCertManager,
                                  final CertValidationProcessor certValidationProcessor,
                                  final Auditor auditor,
                                  final Set certOidSet) {
        this.provider = provider;
        this.trustedCertServices = trustedCertServices;
        this.clientCertManager = clientCertManager;
        this.certValidationProcessor = certValidationProcessor;
        this.auditor = auditor;
        this.certOidSet = certOidSet;
        this.providerConfig = (FederatedIdentityProviderConfig) provider.getConfig();
    }

    protected void checkCertificateMatch( FederatedUser u, X509Certificate requestCert ) throws FindException, BadCredentialsException {
        X509Certificate importedCert = (X509Certificate)clientCertManager.getUserCert(u);
        if ( importedCert == null ) {
            // This is OK as long as it was signed by a trusted cert
            if (certOidSet.isEmpty() ) {
                // No trusted certs means the request cert must match a previously imported client cert
                throw new BadCredentialsException("User " + u + " has no client certificate imported, " +
                                                  "and this Federated Identity Provider has no CA certs " +
                                                  "that are trusted");
            }
        } else if ( !CertUtils.certsAreEqual(importedCert, requestCert) ) {
                throw new BadCredentialsException("Request certificate [" +
                        CertUtils.getCertIdentifyingInformation(requestCert) + "] for user " + u + "] does not match " +
                        "previously imported certificate [" + CertUtils.getCertIdentifyingInformation(importedCert) + "]");
        }
    }

    protected void validateCertificate(final X509Certificate certificate, boolean isClient) throws AuthenticationException {
        try {
            CertificateValidationResult cvr = certValidationProcessor.check(
                            new X509Certificate[]{certificate},
                            null,
                            providerConfig.getCertificateValidationType(),
                            CertValidationProcessor.Facility.IDENTITY,
                            auditor);

            if ( cvr != CertificateValidationResult.OK ) {
                exceptionForType("Certificate [" + CertUtils.getCertIdentifyingInformation(certificate) +
                        "] path validation and/or revocation checking failed", null, isClient);
            }
        } catch (CertificateException ce) {
            exceptionForType("Certificate [" + CertUtils.getCertIdentifyingInformation(certificate) +
                    "] path validation and/or revocation checking error", ce, isClient);
        }
    }

    protected void exceptionForType(String message, Throwable thrown, boolean isClient) throws AuthenticationException {
        if (isClient)
            throw new InvalidClientCertificateException(message, thrown);
        else
            throw new AuthenticationException(message, thrown);        
    }

    protected FederatedUserManager getUserManager() {
        return provider.getUserManager();
    }

    protected FederatedGroupManager getGroupManager() {
        return provider.getGroupManager();
    }

    protected final FederatedIdentityProvider provider;
    protected final TrustedCertServices trustedCertServices;
    protected final CertValidationProcessor certValidationProcessor;
    protected final Auditor auditor;
    protected final Set<Goid> certOidSet;
    protected final FederatedIdentityProviderConfig providerConfig;
    protected final ClientCertManager clientCertManager;
}
