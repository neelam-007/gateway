/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.internal;

import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.server.identity.PersistentIdentityProvider;
import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * @author alex
 */
public interface InternalIdentityProvider
        extends PersistentIdentityProvider<InternalUser, InternalGroup, InternalUserManager, InternalGroupManager> {

    /**
     * Provides a method to verify that for this login credentials from the internal identity provider, verify
     * if the user has client certificate assigned already.
     *
     * @param lc    The login credentials
     * @return      TRUE if user with the login credentials do have client certificate assigned
     * @throws AuthenticationException
     */
    public boolean hasClientCert(LoginCredentials lc) throws AuthenticationException;
}
