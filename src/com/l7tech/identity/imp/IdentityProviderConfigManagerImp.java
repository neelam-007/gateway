package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.*;
import java.sql.SQLException;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp extends HibernateEntityManager implements IdentityProviderConfigManager {
    /**
     * Constructs a new <code>IdentityProviderConfigManagerImp</code> with a default PersistenceContext.  You get your own context and call the other constructor if you want to execute multiple operations in a transaction.
     * @throws SQLException
     */
    public IdentityProviderConfigManagerImp() throws SQLException {
        _manager = PersistenceManager.getInstance();
        _context = _manager.getContext();
    }

    /**
     * Constructs a new <code>IdentityProviderConfigManagerImp</code> with a specific context.
     * @param context
     */
    public IdentityProviderConfigManagerImp( PersistenceContext context ) {
        _manager = PersistenceManager.getInstance();
        _context = context;
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        _manager.delete( _context, identityProviderConfig );
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException {
        return null;
    }

    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException {
        return _manager.save( _context, identityProviderConfig );
    }

    public Collection findAll() throws FindException {
        return _manager.find( _context, "select * from identity-provider", "hello", String.class );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

    PersistenceManager _manager;
    PersistenceContext _context;
}
