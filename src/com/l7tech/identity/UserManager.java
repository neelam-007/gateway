package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.User;

import java.util.Collection;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    public User findByPrimaryKey( long oid ) throws FindException;
    public void delete( User user ) throws DeleteException;
    public long save( User user ) throws SaveException;
    public void setIdentityProviderOid( long oid );
}
