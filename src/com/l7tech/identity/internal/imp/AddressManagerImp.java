package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.AddressManager;
import com.l7tech.identity.internal.Address;
import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public class AddressManagerImp implements AddressManager {
    public Collection findAll() throws FindException {
        return null;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

    public Collection findAllHeaders() throws FindException {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    public Address findByPrimaryKey(long oid) throws FindException {
        return null;
    }

    public void delete(Address address) throws DeleteException {
    }

    public long save(Address address) throws SaveException {
        return Entity.DEFAULT_OID;
    }
}
