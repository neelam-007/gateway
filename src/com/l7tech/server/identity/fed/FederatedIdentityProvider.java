/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProvider implements IdentityProvider {
    public FederatedIdentityProvider( IdentityProviderConfig config ) {
        if ( !(config instanceof FederatedIdentityProviderConfig) )
            throw new IllegalArgumentException("Config must be an instance of FederatedIdentityProviderConfig");
        this.config = (FederatedIdentityProviderConfig)config;
        this.userManager = new FederatedUserManager(this);
        this.groupManager = new FederatedGroupManager(this);
        this.trustedCertManager = (TrustedCertManager) Locator.getDefault().lookup(TrustedCertManager.class);
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException {
        if ( pc.getFormat() == CredentialFormat.CLIENTCERT_X509_ASN1_DER ) {
            String userdn = pc.getLogin();
            TrustedCertManager manager = (TrustedCertManager)Locator.getDefault().lookup(TrustedCertManager.class);
            X509Certificate cert = (X509Certificate)pc.getPayload();
            String sub = cert.getSubjectDN().getName();
            String iss = cert.getIssuerDN().getName();
            // TODO
        } else throw new BadCredentialsException("Can't authenticate without X.509 certificate credentials");
        return null;
    }

    public boolean isReadOnly() {
        return false;
    }

    public Collection search( EntityType[] types, String searchString ) throws FindException {
        // TODO
        return Collections.EMPTY_LIST;
    }

    /**
     * Meaningless - no passwords in FIP anyway
     */
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public void test() {
        // TODO
    }

    private final FederatedIdentityProviderConfig config;
    private final FederatedUserManager userManager;
    private final FederatedGroupManager groupManager;
    private final TrustedCertManager trustedCertManager;
}
