package com.l7tech.identity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.identity.Group;

/**
 * @author alex
 */
public interface GroupManager extends EntityManager {
    public Group findByPrimaryKey( long oid );
    public void delete( Group group );
    public long save( Group group );

    public void setIdentityProviderOid( long oid );
}
