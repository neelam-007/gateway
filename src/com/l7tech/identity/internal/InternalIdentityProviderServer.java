package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;

import java.security.Principal;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class InternalIdentityProviderServer implements IdentityProvider {
    public InternalIdentityProviderServer() {
    }

    public void initialize( IdentityProviderConfig config ) {
        cfg = config;
        userManager = new InternalUserManagerServer();
        groupManager = new InternalGroupManagerServer();
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

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public boolean isReadOnly() { return false; }

    private IdentityProviderConfig cfg;
    private InternalUserManagerServer userManager;
    private InternalGroupManagerServer groupManager;
}
