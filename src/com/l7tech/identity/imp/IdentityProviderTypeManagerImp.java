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

    public IdentityProviderTypeManagerImp() {
        super();
    }

    public void delete(IdentityProviderType identityProviderType) throws DeleteException {
        try {
            _manager.delete( getContext(), identityProviderType );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public IdentityProviderType findByPrimaryKey( long oid ) throws FindException {
        try {
            return (IdentityProviderType)_manager.findByPrimaryKey( getContext(), IdentityProviderType.class, oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save( IdentityProviderType identityProviderType ) throws SaveException {
        try {
            return _manager.save( getContext(), identityProviderType );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( IdentityProviderType identityProviderType ) throws UpdateException {
        try {
            _manager.update( getContext(), identityProviderType );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public Collection findAll() throws FindException {
        try {
            return _manager.find( getContext(), "from identity_provider_type in class com.l7tech.identity.imp.IdentityProviderTypeImp" );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

}
