package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.*;
import java.sql.SQLException;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp extends HibernateEntityManager implements IdentityProviderConfigManager {
    public IdentityProviderConfigManagerImp() {
        _manager = PersistenceManager.getInstance();
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        _manager.delete( identityProviderConfig );
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException {
        return null;
    }

    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException {
        try {
            return _manager.save( identityProviderConfig );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public Collection findAll() throws FindException {
        return _manager.find( "select * from identity-provider", "hello", String.class );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

    PersistenceManager _manager;
}
