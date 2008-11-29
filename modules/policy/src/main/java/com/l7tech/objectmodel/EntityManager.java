/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import java.util.Map;

/**
 * Interface for DAOs that provide CRUD services for {@link PersistentEntity} instances.
 *
 * @param <ET> is the Entity type
 * @param <HT> is the EntityHeader type
 * @author alex
 */
public interface EntityManager<ET extends PersistentEntity, HT extends EntityHeader>
        extends ReadOnlyEntityManager<ET,HT>
{
    long save(ET entity) throws SaveException;

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

    Class<? extends Entity> getImpClass();

    Class<? extends Entity> getInterfaceClass();

    EntityType getEntityType();

    String getTableName();

    /**
     * Find a single entity by its unique name.  This is typically only meaningful
     * for a NamedEntity.
     *
     * @param name the name of the entity to locate.  Required.
     * @return the entity by that name, or null if none was found.
     * @throws FindException in the event of a database problem
     */
    ET findByUniqueName(String name) throws FindException;

    void delete(long oid) throws DeleteException, FindException;

    void update(ET entity) throws UpdateException;

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
