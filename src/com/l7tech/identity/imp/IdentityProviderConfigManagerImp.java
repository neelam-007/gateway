package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalIdentityProvider;
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.misc.Locator;

import java.util.*;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp extends com.l7tech.objectmodel.HibernateEntityManager implements IdentityProviderConfigManager {
    public IdentityProviderConfigManagerImp() {
        _manager = PersistenceManager.getInstance();
    }

    public void delete(IdentityProviderConfig identityProviderConfig) {
        _manager.delete( identityProviderConfig );
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) {
        return null;
    }

    public long save( IdentityProviderConfig identityProviderConfig ) {
        return _manager.save( identityProviderConfig );
    }

    public Collection findAll() {
        return _manager.find( "select * from identity-provider", "hello", String.class );
    }

    public Collection findAll(int offset, int windowSize) {
        return null;
    }

    PersistenceManager _manager;
}
