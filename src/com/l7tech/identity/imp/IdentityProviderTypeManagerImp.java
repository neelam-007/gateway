package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.*;
import java.sql.SQLException;

/**
 * @author alex
 */
public class IdentityProviderTypeManagerImp extends HibernateEntityManager implements IdentityProviderTypeManager {
    public IdentityProviderTypeManagerImp( PersistenceContext context ) {
        super(context);
    }

    public void delete(IdentityProviderType identityProviderType) throws DeleteException {
        _manager.delete( _context, identityProviderType );
    }

    public IdentityProviderType findByPrimaryKey( long oid ) throws FindException {
        return (IdentityProviderType)_manager.findByPrimaryKey( _context, IdentityProviderType.class, oid );
    }

    public long save( IdentityProviderType identityProviderType ) throws SaveException {
        return _manager.save( _context, identityProviderType );
    }

    public void update( IdentityProviderType identityProviderType ) throws UpdateException {
        _manager.update( _context, identityProviderType );
    }

    public Collection findAll() throws FindException {
        return _manager.find( _context, "from identity_provider_type in class com.l7tech.identity.imp.IdentityProviderTypeImp" );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

}
