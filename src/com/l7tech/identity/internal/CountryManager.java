package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface CountryManager extends EntityManager {
    public Country findByPrimaryKey( long oid ) throws FindException;
    public long save( Country country ) throws SaveException;
    public void delete( Country country ) throws DeleteException;
    public void update( Country country ) throws UpdateException;
}
