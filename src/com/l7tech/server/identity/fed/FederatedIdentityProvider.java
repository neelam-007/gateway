/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedIdentityProvider implements IdentityProvider {
    public void initialize( IdentityProviderConfig config ) {
    }

    public IdentityProviderConfig getConfig() {
        return null;
    }

    public UserManager getUserManager() {
        return null;
    }

    public GroupManager getGroupManager() {
        return null;
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
        return null;
    }

    public String getAuthRealm() {
        return null;
    }
}
