package com.l7tech.objectmodel;

import java.util.Collection;

/**
 * @author alex
 */
public interface StandardManager {
    public Collection findByPrimaryKey( long oid );
    public Collection findAll();
    public Collection findAll( int offset, int windowSize );
}
