package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface CountryManager extends EntityManager {
    public Country findByPrimaryKey( long oid );
    public void save( Country country );
    public void delete( Country country );
}
