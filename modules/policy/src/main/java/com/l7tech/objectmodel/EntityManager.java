package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for DAOs that provide CRUD services for {@link PersistentEntity} instances.
 *
 * @param <ET> is the PersistentEntity type
 * @param <HT> is the EntityHeader type
 * @author Victor Kazakov
 */
public interface EntityManager<ET extends PersistentEntity, HT extends EntityHeader>
        extends ReadOnlyEntityManager<ET,HT>
{
    @Override
    @Nullable
    ET findByPrimaryKey(Goid goid) throws FindException;

    @Override
    Collection<HT> findAllHeaders() throws FindException;

    @Override
    Collection<HT> findAllHeaders(int offset, int windowSize) throws FindException;

    @Override
    Collection<ET> findAll() throws FindException;

    Goid save(ET entity) throws SaveException;

    void save(Goid id, ET entity) throws SaveException;

    Integer getVersion(Goid goid) throws FindException;

    Map<Goid, Integer> findVersionMap() throws FindException;

    void delete(ET entity) throws DeleteException;

    /**
     * Returns the {@link PersistentEntity} with the specified OID. If the entity's version was last checked more than
     * <code>maxAge</code> milliseconds ago, check for an updated version in the database.  If the entity has been
     * updated, refresh it in the cache if the implementation doesn't complain.
     *
     * @param goid the OID of the Entity to return.
     * @param maxAge the maximum age of a cached Entity to return, in milliseconds.
     * @return the Entity with the specified OID, or <code>null</code> if it does not exist.
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    @Nullable
    ET getCachedEntity(Goid goid, int maxAge) throws FindException;

    /**
     * The Entity this manager manages may implement an Interface representing the Entity. If so this method should
     * return the class of the Interface implemented.
     * @return the Class of the interface this Entity implements, otherwise the Entity class itself
     */
    Class<? extends Entity> getInterfaceClass();

    EntityType getEntityType();

    String getTableName();

    /**
     * Find a single entity by its unique name.  This is typically only meaningful
     * for a NamedEntity.
     *
     * @param name the name of the entity to locate.  Required.
     * @return the entity by that name, or null if none was found.
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    @Nullable
    ET findByUniqueName(String name) throws FindException;

    void delete(Goid goid) throws DeleteException, FindException;

    void update(ET entity) throws UpdateException;

    @Nullable
    ET findByHeader(EntityHeader header) throws FindException;

    static enum UniqueType {
        NONE, NAME, OTHER
    }
}
