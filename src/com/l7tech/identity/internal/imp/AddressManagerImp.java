package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.*;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;

/**
 * @author alex
 */
public class AddressManagerImp extends ProviderSpecificEntityManager implements AddressManager {
    public AddressManagerImp( PersistenceContext context ) {
        super( context );
    }

    public AddressManagerImp() {
        super();
    }

    public Address findByPrimaryKey(long oid) throws FindException {
        try {
            return (Address)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
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

    public Class getImpClass() {
        return AddressImp.class;
    }

    public Class getInterfaceClass() {
        return Address.class;
    }

    public String getTableName() {
        return "address";
    }

}
