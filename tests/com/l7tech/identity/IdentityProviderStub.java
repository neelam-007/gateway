package com.l7tech.identity;

import com.l7tech.identity.imp.IdentityProviderConfigImp;

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
        return um;
    }

    public GroupManager getGroupManager() {
        return gm;
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

    private IdentityProviderConfig icf = null;
    private UserManager um = null;
    private GroupManager gm = null;

}
