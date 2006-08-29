package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * ET is the Entity type; HT is the EntityHeader type.
 * @author alex
 */
public interface EntityManager<ET extends PersistentEntity, HT extends EntityHeader> {
    ET findByPrimaryKey(long oid) throws FindException;

    long save(ET entity) throws SaveException;

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection<HT> findAllHeaders() throws FindException;

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection<HT> findAllHeaders( int offset, int windowSize ) throws FindException;

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of Entity objects.
     */
    public Collection<ET> findAll() throws FindException;

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for instances of this entity class from a list sorted by <code>oid</code>, selecting only a specific subset of the list.
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public Collection<ET> findAll( int offset, int windowSize ) throws FindException;

    Integer getVersion( long oid ) throws FindException;

    Map<Long, Integer> findVersionMap() throws FindException;

    void delete(ET entity) throws DeleteException;

    /**
     * Returns the {@link PersistentEntity} with the specified OID. If the entity's version was last checked more than
     * <code>maxAge</code> milliseconds ago, check for an updated version in the database.  If the entity has been
     * updated, refresh it in the cache if the implementation doesn't complain.
     *
     * @param o the OID of the Entity to return.
     * @param maxAge the maximum age of a cached Entity to return, in milliseconds.
     * @return the Entity with the specified OID, or <code>null</code> if it does not exist.
     * @throws FindException in the event of a database problem
     * @throws CacheVeto thrown by an implementor
     */
    public ET getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto;

    ET findEntity(long l) throws FindException;

    Class getImpClass();

    Class getInterfaceClass();

    EntityType getEntityType();

    String getTableName();

    ET findByUniqueName(String name) throws FindException;

    void delete(long oid) throws DeleteException, FindException;

    void update(ET entity) throws UpdateException;

    /** Holds information about a cached Entity. */
    static class CacheInfo<ET extends PersistentEntity> {
        ET entity;
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

    public static enum UniqueType {
        NONE, NAME, OTHER
    }

}
