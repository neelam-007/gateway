package com.l7tech.identity.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface OrganizationManager extends EntityManager {
    public Organization findByPrimaryKey( long oid );
    public void delete( Organization organization );
    public long save( Organization organization );
}
