package com.l7tech.identity.ldap;

import com.l7tech.identity.*;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderServer implements com.l7tech.identity.IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        if (!(config.type() == IdentityProviderType.LDAP)) throw new IllegalArgumentException("Expecting Ldap config type");
        cfg = config;
        groupManager = new LdapGroupManagerServer(cfg);
        userManager = new LdapUserManagerServer(cfg);
    }

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public boolean authenticate( User user, byte[] credentials) {
        // basic authentication
        return userManager.authenticateBasic(user.getName(), new String(credentials));
    }

    public boolean isReadOnly() { return true; }


    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityProviderConfig cfg = null;
    private LdapGroupManagerServer groupManager = null;
    private LdapUserManagerServer userManager = null;
}
