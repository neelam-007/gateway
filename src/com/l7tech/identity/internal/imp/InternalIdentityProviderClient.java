package com.l7tech.identity.internal.imp;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 13, 2003
 *
 * Console-side implemenation of the internal identity provider object giving access
 * to both the user manager and the group manager.
 * Authentication in not implemented in this version since this is intended to be used on the console only.
 * 
 */
public class InternalIdentityProviderClient implements com.l7tech.identity.IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        this.config = config;
    }

    public UserManager getUserManager() {
        if (config != null) {
            return new InternalUserManagerClient(config.getOid());
        }
        return null;
    }

    public GroupManager getGroupManager() {
        if (config != null) {
            return new InternalGroupManagerClient(config.getOid());
        }
        return null;
    }

    public boolean authenticate(User user, Object credential) {
        // this doesnt make sense on the client side.
        return false;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    com.l7tech.identity.IdentityProviderConfig config = null;
}
