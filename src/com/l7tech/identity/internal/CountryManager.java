package com.l7tech.identity.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface CountryManager extends EntityManager {
    public Country findByPrimaryKey( long oid );
    public long save( Country country );
    public void delete( Country country );
}
