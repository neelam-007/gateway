package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;

import java.util.Collection;

/**
 * @author alex
 */
public interface AddressManager extends EntityManager {
    public Address findByPrimaryKey( long oid ) throws FindException;
    public void delete( Address address ) throws DeleteException;
    public long save( Address address ) throws SaveException;
}
