package com.l7tech.identity.imp;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class IdentityProviderConfigManagerClient implements IdentityProviderConfigManager {
    public IdentityProviderConfig findByPrimaryKey(long oid) {
        return null;
    }

    public long save(IdentityProviderConfig identityProviderConfig) {
        return 0;
    }

    public void delete(IdentityProviderConfig identityProviderConfig) {
    }

    public Collection findAllHeaders() {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) {
        return null;
    }

    public Collection findAll() {
        return null;
    }

    public Collection findAll(int offset, int windowSize) {
        return null;
    }
}
