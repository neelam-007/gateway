package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;

import java.security.Principal;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderClient implements com.l7tech.identity.IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        this.config = (LdapIdentityProviderConfig)config;
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return null;
    }

    public GroupManager getGroupManager() {
        return null;
    }

    public boolean authenticate(Principal user, byte[] credentials) {
        return false;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private LdapIdentityProviderConfig config;
}
