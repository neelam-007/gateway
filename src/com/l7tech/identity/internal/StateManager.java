package com.l7tech.identity.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface StateManager extends EntityManager {
    public State findByPrimaryKey( long oid );
    public void delete( State state );
    public long save( State state );
}
