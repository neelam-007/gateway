package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.EntityManager;

/**
 * @author alex
 */
public interface StateManager extends EntityManager {
    public State findByPrimaryKey( long oid );
    public void delete( State state );
    public void save( State state );
}
