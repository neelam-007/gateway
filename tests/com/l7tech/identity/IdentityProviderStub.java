package com.l7tech.identity;

import com.l7tech.console.util.Registry;

import java.security.Principal;

/**
 * Test stub for identity manager.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderStub implements IdentityProvider {

    public IdentityProviderStub() {
    }

    public void initialize(IdentityProviderConfig config) {
        this.icf = config;
    }

    public IdentityProviderConfig getConfig() {
        return icf;
    }

    public UserManager getUserManager() {
        return Registry.getDefault().getInternalUserManager();
    }

    public GroupManager getGroupManager() {
        return Registry.getDefault().getInternalGroupManager();
    }

    public boolean authenticate(Principal user, byte[] credentials) {
        return false;
    }


    public void setUserManager(UserManager um) {
        this.um = um;
    }

    public void setGroupManager(GroupManager gm) {
        this.gm = gm;
    }

    public boolean isReadOnly() { return false; }

    private IdentityProviderConfig icf = null;
    private UserManager um = null;
    private GroupManager gm = null;

}
