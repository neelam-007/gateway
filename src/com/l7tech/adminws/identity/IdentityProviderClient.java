package com.l7tech.adminws.identity;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class IdentityProviderClient implements com.l7tech.identity.IdentityProvider {

    public void initialize(IdentityProviderConfig config) {
        this.config = config;
        userManager = new UserManagerClient(config);
        groupManager = new GroupManagerClient(config);
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

    public boolean authenticate( PrincipalCredentials pc ) {
        throw new RuntimeException("not supported in this impl");
    }

    public boolean isReadOnly() { return true; }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfig config;
    private UserManagerClient userManager;
    private GroupManagerClient groupManager;
}
