package com.l7tech.identity.provider.imp;

import com.l7tech.identity.provider.IdentityProviderConfig;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 */
public class IdentityProviderConfigManagerClient implements com.l7tech.identity.provider.IdentityProviderConfigManager {
    public void delete(IdentityProviderConfig identityProviderConfig) {
        // todo (fla) implementation
    }

    public IdentityProviderConfig create() {
        // todo (fla) implementation
        return null;
    }

    public Collection findByPrimaryKey(long oid) {
        // todo (fla) implementation
        return null;
    }

    public Collection findAll() {
        // todo (fla) implementation
        return null;
    }

    public Collection findAll(int offset, int windowSize) {
        // todo (fla) implementation
        return null;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    // todo, manage the stub to the client-side of the web service
}
