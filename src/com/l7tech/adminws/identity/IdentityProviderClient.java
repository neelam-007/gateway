package com.l7tech.adminws.identity;

import com.l7tech.identity.*;
import java.security.Principal;

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

    public boolean authenticate( User user, byte[] credentials) {
        return false;
    }

    public boolean isReadOnly() { return true; }

    // ************************************************
    // PRIVATES
    // ************************************************

    private IdentityProviderConfig config;
    private UserManagerClient userManager;
    private GroupManagerClient groupManager;
}
