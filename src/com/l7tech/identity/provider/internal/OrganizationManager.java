package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface OrganizationManager extends EntityManager {
    public Organization findByPrimaryKey( long oid );
    public void delete( Organization organization );
    public void save( Organization organization );
}
