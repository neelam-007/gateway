package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.AddressManager;
import com.l7tech.identity.internal.Address;
import com.l7tech.objectmodel.Entity;

import java.util.Collection;

/**
 * @author alex
 */
public class AddressManagerImp implements AddressManager {
    public Collection findAll() {
        return null;
    }

    public Collection findAll(int offset, int windowSize) {
        return null;
    }

    public Collection findAllHeaders() {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) {
        return null;
    }

    public Address findByPrimaryKey(long oid) {
        return null;
    }

    public void delete(Address address) {
    }

    public long save(Address address) {
        return Entity.DEFAULT_OID;
    }
}
