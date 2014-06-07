package com.l7tech.server.bundling;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Holds an entity and its related (grouped) entities
 */
public class EntityContainer<E extends Entity> {
    //The main entity that this container represents.
    @NotNull
    private final E entity;

    @NotNull
    private final Pair<String, EntityType> entityContainerId;

    /**
     * Create a new entity container with the given entity.
     *
     * @param entity The entity to create the container for
     */
    public EntityContainer(@NotNull final E entity) {
        this.entity = entity;
        entityContainerId = new Pair<>(entity.getId(), EntityType.findTypeByEntity(entity.getClass()));
    }

    /**
     * Returns the primary entity.
     *
     * @return The primary entity for this container
     */
    @NotNull
    public E getEntity() {
        return entity;
    }

    /**
     * Returns the id for this entity container. It is a pair that contains the id and type of the primary entity in
     * this entity container
     *
     * @return The id of this entity container
     */
    @NotNull
    public Pair<String, EntityType> getId() {
        return entityContainerId;
    }

    /**
     * The list of entities contained in this entity container.
     *
     * @return The list of entities contained in this entity container
     */
    @NotNull
    public List<Entity> getEntities() {
        return Arrays.<Entity>asList(entity);
    }
}
