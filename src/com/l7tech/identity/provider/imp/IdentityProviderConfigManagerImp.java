package com.l7tech.identity.provider.imp;

import com.l7tech.identity.provider.IdentityProviderConfigManager;
import com.l7tech.identity.provider.IdentityProviderConfig;
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.objectmodel.Entity;

import java.util.Collection;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp implements IdentityProviderConfigManager {
    public IdentityProviderConfigManagerImp() {
        _manager = PersistenceManager.getInstance();
    }

    public void delete(IdentityProviderConfig identityProviderConfig) {
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) {
        return null;
    }

    public long save( IdentityProviderConfig identityProviderConfig ) {
        return Entity.DEFAULT_OID;
    }

    public Collection findAll() {
        return null;
    }

    public Collection findAll(int offset, int windowSize) {
        return null;
    }

    PersistenceManager _manager;
}
