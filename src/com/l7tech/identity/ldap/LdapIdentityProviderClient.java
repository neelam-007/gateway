package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderType;

import java.security.Principal;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderClient implements com.l7tech.identity.IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        if (!(config.type() == IdentityProviderType.LDAP)) throw new IllegalArgumentException("Expecting a ldap config type");
        this.config = config;
        userManager = new LdapUserManagerClient(config);
        groupManager = new LdapGroupManagerClient(config);
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

    public boolean authenticate(Principal user, byte[] credentials) {
        return false;
    }

    public boolean isReadOnly() { return true; }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfig config;
    private LdapUserManagerClient userManager;
    private LdapGroupManagerClient groupManager;
}
