package com.l7tech.identity.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.*;
import java.sql.SQLException;

/**
 * @author alex
 */
public class IdentityProviderConfigManagerImp extends HibernateEntityManager implements IdentityProviderConfigManager {
    static final Class IMPCLASS = IdentityProviderConfigImp.class;

    public IdentityProviderConfigManagerImp( PersistenceContext context ) {
        super( context );
    }

    public IdentityProviderConfigManagerImp() {
        super();
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        try {
            _manager.delete( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public Collection findAllIdentityProviders() throws FindException {
        return IdentityProviderFactory.findAllIdentityProviders(this);
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException {
        try {
            return (IdentityProviderConfig)_manager.findByPrimaryKey( getContext(), IMPCLASS, oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException {
        try {
            return _manager.save( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( IdentityProviderConfig identityProviderConfig ) throws UpdateException {
        try {
            _manager.update( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public Collection findAll() throws FindException {
        try {
            return _manager.find( getContext(), "from identity_provider in class com.l7tech.identity.imp.IdentityProviderConfigImp" );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }
}
