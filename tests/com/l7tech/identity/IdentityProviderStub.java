package com.l7tech.identity;

import com.l7tech.console.util.Registry;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

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

    public boolean authenticate( PrincipalCredentials pc ) {
        return false;
    }


    public boolean isReadOnly() { return false; }

    public Collection search(EntityType[] types, String searchString) throws FindException {
        List list = new ArrayList();
        list.addAll(getUserManager().findAllHeaders());
        list.addAll(getGroupManager().findAllHeaders());

        return list;
    }
    private IdentityProviderConfig icf = null;

}
