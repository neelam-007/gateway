package com.l7tech.objectmodel;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author alex
 */
public interface ReadOnlyEntityManager<ET extends Entity, HT extends EntityHeader> {

    /**
     * Find an entity by object identifier.
     *
     * @param goid The identifier for the entity
     * @return The entity or null if not found
     * @throws FindException If an error occurs
     * @see ObjectNotFoundException Some implementations may throw this rather than returning null
     */
    @Nullable
    ET findByPrimaryKey(Goid goid) throws FindException;

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    Collection<HT> findAllHeaders() throws FindException;

    Collection<HT> findAllHeaders(int offset, int windowSize) throws FindException;

    /**
     * Returns an unmodifiable collection of <code>Entity</code> objects for all instances of the entity class corresponding to this Manager.
     * @return A <code>Collection</code> of Entity objects.
     */
    Collection<ET> findAll() throws FindException;

    /**
     * Get the implementing class of the Entity
     * @return the Class of the actual implementing class of the Entity
     */
    Class<? extends Entity> getImpClass();
}
