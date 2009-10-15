/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import java.util.Collection;

/**
 * @author alex
 */
public interface ReadOnlyEntityManager<ET extends Entity, HT extends EntityHeader> {
    ET findByPrimaryKey(long oid) throws FindException;

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code> objects for all instances of the entity class corresponding to this Manager.
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
