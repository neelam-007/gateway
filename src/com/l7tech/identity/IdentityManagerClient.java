package com.l7tech.identity;

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
    protected IdentityAdmin getStub() throws RemoteException {
        IdentityAdmin svc = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (svc == null) {
            throw new RemoteException("Cannot obtain the identity service");
        }
        return svc;
    }

    protected IdentityProviderConfig config;
}
