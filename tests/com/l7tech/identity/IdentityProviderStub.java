package com.l7tech.identity;

import com.l7tech.identity.imp.IdentityProviderConfigImp;

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


    public void setUserManager(UserManager um) {
        this.um = um;
    }

    public void setGroupManager(GroupManager gm) {
        this.gm = gm;
    }

    public boolean authenticate(User user, Object credential) {
        return false;
    }

    private IdentityProviderConfig icf = new IdentityProviderConfigImp();
    private UserManager um = null;


    private GroupManager gm = null;

}
