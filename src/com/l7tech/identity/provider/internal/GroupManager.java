package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface GroupManager extends EntityManager {
    public Group findByPrimaryKey( long oid );
    public void delete( Group group );
    public long save( Group group );
}
