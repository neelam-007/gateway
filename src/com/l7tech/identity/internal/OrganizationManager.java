package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface OrganizationManager extends EntityManager {
    public Organization findByPrimaryKey( long oid ) throws FindException;
    public void delete( Organization organization ) throws DeleteException;
    public long save( Organization organization ) throws SaveException;
}
