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
 */
public class InternalIdentityProviderClient implements com.l7tech.identity.IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
    }

    public UserManager getUserManager() {
        return null;
    }

    public GroupManager getGroupManager() {
        return null;
    }

    public boolean authenticate(User user, Object credential) {
        // this doesnt make sense on the lcient side.
        return false;
    }
}
