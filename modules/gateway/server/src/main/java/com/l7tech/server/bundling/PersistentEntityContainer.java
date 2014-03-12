package com.l7tech.server.bundling;

import com.l7tech.objectmodel.PersistentEntity;

/**
 * Holds an entity and its dependent entities
 */
public class PersistentEntityContainer<E extends PersistentEntity> extends EntityContainer<E> {

    public PersistentEntityContainer(final E entity) {
        super(entity);
    }

    public String getId() {
        return getEntity().getId();
    }
}
