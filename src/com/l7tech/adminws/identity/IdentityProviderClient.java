package com.l7tech.adminws.identity;

import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.common.util.Locator;

import java.util.Collection;
import java.rmi.RemoteException;

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

    public void authenticate( PrincipalCredentials pc ) throws AuthenticationException {
        throw new AuthenticationException("not supported in this impl");
    }

    public boolean isReadOnly() { return true; }

    public Collection search(EntityType[] types, String searchString) throws FindException {
        EntityHeader[] array = null;
        try {
            array = getStub().searchIdentities(config.getOid(), types, searchString);
        } catch (RemoteException e) {
            throw new FindException(e.getMessage(), e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityService getStub() throws RemoteException {
        if (localStub == null) {
            localStub = (IdentityService)Locator.getDefault().lookup(IdentityService.class);
            if (localStub == null) {
                throw new RemoteException("Cannot obtain the identity service");
            }
        }
        return localStub;
    }

    protected IdentityService localStub = null;
    private IdentityProviderConfig config;
    private UserManagerClient userManager;
    private GroupManagerClient groupManager;
}
