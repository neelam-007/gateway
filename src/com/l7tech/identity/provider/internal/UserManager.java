package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.EntityManager;

import java.util.Collection;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    public User findByPrimaryKey( long oid );
    public void delete( User user );
    public long save( User user );
}
