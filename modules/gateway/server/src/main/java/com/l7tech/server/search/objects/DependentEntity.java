package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * The DependentEntity is a reference to an entity. Contains all the identifiers needed to retrieve the entity.
 *
 * @author Victor Kazakov
 */
public class DependentEntity extends DependentObject {
    private final EntityType entityType;
    private final String publicID;
    private final String internalID;

    /**
     * Creates a new DependentEntity.
     *
     * @param name       The name of the entity.
     * @param entityType The {@link EntityType} of the entity
     * @param publicID   The public ID of the entity. In some cases this is the same as the name. But it may also be a
     *                   full path, a GUID, or some other id.
     * @param internalID The internalID of the entity. Usually the entities OID.
     */
    public DependentEntity(String name, @NotNull EntityType entityType, String publicID, String internalID) {
        super(name);
        this.entityType = entityType;
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

        DependentEntity that = (DependentEntity) o;

        if (entityType != that.entityType) return false;
        if (internalID != null ? !internalID.equals(that.internalID) : that.internalID != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (publicID != null ? !publicID.equals(that.publicID) : that.publicID != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entityType != null ? entityType.hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (publicID != null ? publicID.hashCode() : 0);
        result = 31 * result + (internalID != null ? internalID.hashCode() : 0);
        return result;
    }
}
