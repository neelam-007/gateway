package com.l7tech.server.bundling;

import org.apache.commons.lang.NotImplementedException;

import java.util.Collections;
import java.util.List;

/**
 * Holds an entity and its dependent entities
 */
public class EntityContainer<E> {
    protected final E entity;

    public EntityContainer(final E entity) {
        this.entity = entity;
    }

    protected EntityContainer() {
        entity = null;
    }

    /**
     * Access the primary entity.
     */
    public E getEntity() {
        return entity;
    }

    public String getId() {
        throw new NotImplementedException("");
    }

    public List getEntities() {
        return Collections.singletonList(entity);
    }
}
