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
        if (!(config instanceof LdapIdentityProviderConfig)) throw new IllegalArgumentException("Expecting LdapIdentityProviderConfig in LdapIdentityProviderServer.initialize");
        this.config = (LdapIdentityProviderConfig)config;
        userManager = new LdapUserManagerClient(this.config);
        groupManager = new LdapGroupManagerClient(this.config);
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

    private LdapIdentityProviderConfig config;
    private LdapUserManagerClient userManager;
    private LdapGroupManagerClient groupManager;
}
