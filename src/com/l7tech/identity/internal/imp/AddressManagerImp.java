package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.sql.SQLException;

/**
 * @author alex
 */
public class AddressManagerImp extends HibernateEntityManager implements AddressManager {
    public static final Class IMPCLASS = AddressImp.class;

    public AddressManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Address findByPrimaryKey(long oid) throws FindException {
        try {
            return (Address)_manager.findByPrimaryKey( getContext(), IMPCLASS, oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Address address ) throws DeleteException {
        try {
            _manager.delete( getContext(), address );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Address address ) throws SaveException {
        try {
            return _manager.save( getContext(), address );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Address address ) throws UpdateException {
        try {
            _manager.update( getContext(), address );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from address in class com.l7tech.identity.imp.AddressImp";
        if ( _identityProviderOid == -1 )
            throw new FindException( "Can't call findAll() without first calling setIdentityProviderOid!" );
        else {
            try {
                return _manager.find( getContext(), query + " where provider = ?", new Long( _identityProviderOid ), Long.TYPE );
            } catch ( SQLException se ) {
                throw new FindException( se.toString(), se );
            }
        }
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public long _identityProviderOid = -1;
}
