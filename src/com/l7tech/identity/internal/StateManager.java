package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface StateManager extends EntityManager {
    public State findByPrimaryKey( long oid ) throws FindException;
    public void delete( State state ) throws DeleteException;
    public long save( State state ) throws SaveException;
    public void update( State state ) throws UpdateException;
}
