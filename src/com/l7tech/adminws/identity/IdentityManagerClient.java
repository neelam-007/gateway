package com.l7tech.adminws.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.common.util.Locator;

import java.rmi.RemoteException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class IdentityManagerClient {

    public IdentityManagerClient(IdentityProviderConfig config) {
        this.config = config;
    }
    // ************************************************
    // PRIVATES
    // ************************************************
    protected IdentityService getStub() throws RemoteException {
        if (localStub == null) {
            localStub = (IdentityService)Locator.getDefault().lookup(IdentityService.class);
            if (localStub == null) {
                throw new RemoteException("Cannot obtain the identity service");
            }
        }
        return localStub;
    }

    protected IdentityService localStub = null;
    protected IdentityProviderConfig config;
}
