package com.l7tech.identity.internal;

import com.l7tech.objectmodel.EntityManager;

import java.util.Collection;

/**
 * @author alex
 */
public interface AddressManager extends EntityManager {
    public Address findByPrimaryKey( long oid );
    public void delete( Address address );
    public long save( Address address );
}
