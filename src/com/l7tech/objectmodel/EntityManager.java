package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * @author alex
 */
public interface EntityManager {
    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders() throws FindException;

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAllHeaders( int offset, int windowSize ) throws FindException;

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection findAll() throws FindException;

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection findAll( int offset, int windowSize ) throws FindException;

    Integer getVersion( long oid ) throws FindException;

    Map findVersionMap() throws FindException;
}
