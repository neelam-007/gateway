package com.l7tech.server.search;

import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * The DependencyEntity is a reference to an entity. Contains all the identifiers needed to retrieve the entity.
 *
 * @author Victor Kazakov
 */
public class DependencyEntity {
    private final EntityType entityType;
    private final String name;
    private final String publicID;
    private final String internalID;

    /**
     * Creates a new DependencyEntity.
     *
     * @param name       The name of the entity.
     * @param entityType The {@link EntityType} of the entity
     * @param publicID   The public ID of the entity. In some cases this is the same as the name. But it may also be a
     *                   full path, a GUID, or some other id.
     */
    protected DependencyEntity(String name, @NotNull EntityType entityType, String publicID, String internalID) {
        this.entityType = entityType;
        this.name = name;
        this.publicID = publicID;
        this.internalID = internalID;
    }

    /**
     * @return The {@link EntityType} of the entity
     */
    @NotNull
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * @return The name of the entity. Not all entities have names so this may be null.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The public id of the entity. This cannot be null.
     */
    public String getPublicID() {
        return publicID;
    }

    /**
     * @return The internal id of the entity.
     */
    public String getInternalID() {
        return internalID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyEntity that = (DependencyEntity) o;

        if (entityType != that.entityType) return false;
        if (internalID != null ? !internalID.equals(that.internalID) : that.internalID != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (publicID != null ? !publicID.equals(that.publicID) : that.publicID != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entityType != null ? entityType.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (publicID != null ? publicID.hashCode() : 0);
        result = 31 * result + (internalID != null ? internalID.hashCode() : 0);
        return result;
    }
}
