package com.l7tech.identity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.identity.User;

import java.util.Collection;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    public User findByPrimaryKey( long oid );
    public void delete( User user );
    public long save( User user );
    public void setIdentityProviderOid( long oid );
}
