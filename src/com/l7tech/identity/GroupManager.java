package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.Group;

import java.util.Set;
import java.util.Map;

/**
 * @author alex
 */
public interface GroupManager extends EntityManager {
    public Group findByPrimaryKey( String oid ) throws FindException;
    public void delete( Group group ) throws DeleteException;
    public long save( Group group ) throws SaveException;
    public void update( Group group ) throws UpdateException;

    public EntityHeader groupToHeader( Group group );
    public Group headerToGroup( EntityHeader header ) throws FindException;
}
