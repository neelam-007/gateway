/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;

import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedAuthorizationHandler {
    FederatedAuthorizationHandler( FederatedIdentityProvider provider, TrustedCertManager trustedCertManager,
                                   ClientCertManager clientCertManager, Set certOidSet ) {
        this.provider = provider;
        this.trustedCertManager = trustedCertManager;
        this.clientCertManager = clientCertManager;
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
                throw new BadCredentialsException("Request certificate for user " + u +
                                                  " does not match previously imported certificate");
        }
    }

    protected FederatedUserManager getUserManager() {
        return (FederatedUserManager)provider.getUserManager();
    }

    protected FederatedGroupManager getGroupManager() {
        return (FederatedGroupManager)provider.getGroupManager();
    }

    protected final FederatedIdentityProvider provider;
    protected final TrustedCertManager trustedCertManager;
    protected final Set certOidSet;
    protected final FederatedIdentityProviderConfig providerConfig;
    protected final ClientCertManager clientCertManager;
}
