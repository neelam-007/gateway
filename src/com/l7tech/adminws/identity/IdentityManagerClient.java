package com.l7tech.adminws.identity;

import com.l7tech.identity.IdentityProviderConfig;
import java.io.IOException;

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
    protected Client getStub() {
        if (localStub == null) {
            localStub = new Client();
        }
        return localStub;
    }

    protected Client localStub = null;
    protected IdentityProviderConfig config;
}
