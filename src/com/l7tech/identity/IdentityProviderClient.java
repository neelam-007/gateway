package com.l7tech.identity;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * SSM-side implementation of the identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 *
 */
public class IdentityProviderClient implements IdentityProvider {
    public IdentityProviderClient( IdentityProviderConfig config ) {
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

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException {
        throw new AuthenticationException("not supported in this impl");
    }

    public boolean isReadOnly() {
        return !(config.getOid() == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
    }

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

    public void test() throws InvalidIdProviderCfgException {
        // Meaningless
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityAdmin getStub() throws RemoteException {
        IdentityAdmin svc = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (svc == null) {
            throw new RemoteException("Cannot obtain the identity service");
        }
        return svc;
    }

    private final IdentityProviderConfig config;
    private final UserManagerClient userManager;
    private final GroupManagerClient groupManager;
}
