package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class AddressManagerImp extends HibernateEntityManager implements AddressManager {
    public static final Class IMPCLASS = AddressImp.class;

    public AddressManagerImp( PersistenceContext context ) {
        super( context );
    }

    public Address findByPrimaryKey(long oid) throws FindException {
        return (Address)_manager.findByPrimaryKey( _context, IMPCLASS, oid );
    }

    public void delete(Address address ) throws DeleteException {
        _manager.delete( _context, address );
    }

    public long save(Address address ) throws SaveException {
        return _manager.save( _context, address );
    }

    public void update( Address address ) throws UpdateException {
        _manager.update( _context, address );
    }

    public void setIdentityProviderOid(long oid) {
        _identityProviderOid = oid;
    }

    public Collection findAll() throws FindException {
        String query ="from address in class com.l7tech.identity.imp.AddressImp";
        if ( _identityProviderOid == -1 )
            throw new FindException( "Can't call findAll() without first calling setIdentityProviderOid!" );
        else
            return _manager.find( _context, query + " where provider = ?", new Long( _identityProviderOid ), Long.TYPE );
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public long _identityProviderOid = -1;
}
