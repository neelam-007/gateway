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

    /**
     * Returns the {@link Entity} with the specified OID. If the entity's version was last checked more than
     * <code>maxAge</code> milliseconds ago, check for an updated version in the database.  If the entity has been
     * updated, refresh it in the cache if the implementation doesn't complain.
     *
     * @param o the OID of the Entity to return.
     * @param maxAge the maximum age of a cached Entity to return, in milliseconds.
     * @return the Entity with the specified OID, or <code>null</code> if it does not exist.
     * @throws FindException in the event of a database problem
     * @throws CacheVeto thrown by an implementor
     */
    public Entity getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto;

    /** Holds information about a cached Entity. */
    static class CacheInfo {
        Entity entity;
        long timestamp;
        int version;
    }

    /**
     * Thrown by EntityManagers who override <code>checkCachable</code>
     * to prevent an Entity from being cached.
     */
    public static class CacheVeto extends Exception {
        public CacheVeto(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

}
