package com.l7tech.identity;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test stub for identity manager.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderStub implements IdentityProvider {

    public IdentityProviderStub( IdentityProviderConfig config ) {
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

    public User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException {
        throw new AuthenticationException("not implemented");
    }


    public boolean isReadOnly() { return false; }

    public Collection search(EntityType[] types, String searchString) throws FindException {
        List list = new ArrayList();
        list.addAll(getUserManager().findAllHeaders());
        list.addAll(getGroupManager().findAllHeaders());

        return list;
    }

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public void test() throws InvalidIdProviderCfgException {
        // Meaningless
    }

    private IdentityProviderConfig icf = null;

}
