package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.imp.InternalIdentityProviderImp;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Locator;

import java.util.*;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp extends HibernateEntityManager implements IdentityProviderConfigManager {
    static final Class IMPCLASS = IdentityProviderConfigImp.class;

    public IdentityProviderConfigManagerImp( PersistenceContext context ) {
        super(context);
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        _manager.delete( _context, identityProviderConfig );
    }

    public Collection findAllIdentityProviders() throws FindException {
        return IdentityProviderFactory.findAllIdentityProviders(this);
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException {
        return (IdentityProviderConfig)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException {
        return _manager.save( _context, identityProviderConfig );
    }

    public void update( IdentityProviderConfig identityProviderConfig ) throws UpdateException {
        _manager.update( _context, identityProviderConfig );
    }

    public Collection findAll() throws FindException {
        return _manager.find( _context, "from identity_provider in class com.l7tech.identity.imp.IdentityProviderConfigImp" );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }
}
