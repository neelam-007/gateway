package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

/**
 * Represents the "ownership" of an Entity by a Solution Kit, and whether that Entity may be edited by users.
 *
 * An Entity is considered owned by a Solution Kit if it was created by that Kit. Ownership must not extend to
 * existing Entities that are mapped to a Kit during installation or upgrade.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@Entity
@Proxy(lazy=false)
@Table(name="solution_kit_meta")
public class EntityOwnershipDescriptor extends PersistentEntityImp {

    private SolutionKit solutionKit;
    private String entityId;
    private EntityType entityType;
    private boolean readOnly = false;
    // When adding fields, update copyFrom() method

    @Deprecated
    @SuppressWarnings("unused")
    protected EntityOwnershipDescriptor() {}

    public EntityOwnershipDescriptor(@NotNull SolutionKit solutionKit, @NotNull String entityId,
                                     @NotNull EntityType entityType, boolean readOnly) {
        this.solutionKit = solutionKit;
        this.entityId = entityId;
        this.entityType = entityType;
        this.readOnly = readOnly;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="solution_kit_goid", nullable=false)
    public SolutionKit getSolutionKit() {
        return solutionKit;
    }
    public void setSolutionKit(@NotNull final SolutionKit solutionKit) {
        this.solutionKit = solutionKit;
    }

    @Column(name="entity_id", nullable=false)
    public String getEntityId() {
        return entityId;
    }
    public void setEntityId(@NotNull final String entityId) {
        this.entityId = entityId;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="entity_type", length=255, nullable=false)
    public EntityType getEntityType() {
        return entityType;
    }
    public void setEntityType(@NotNull final EntityType entityType) {
        this.entityType = entityType;
    }

    @Column(name="read_only", nullable = false)
    public boolean isReadOnly() {
        return readOnly;
    }
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Copy data from the specified {@code otherDescriptor} and set the owner to the specified {@code ownerSolutionKit}.
     *
     * @param otherDescriptor    the {@code EntityOwnershipDescriptor} from which to copy properties.  Required and cannot be {@code null}.
     * @param ownerSolutionKit   the owner {@code SolutionKit}.  Required and cannot be {@code null}.
     */
    public void copyFrom(
        @NotNull final EntityOwnershipDescriptor otherDescriptor,
        @NotNull final SolutionKit ownerSolutionKit
    ) {
        if (!PersistentEntity.DEFAULT_GOID.equals(otherDescriptor.getGoid())) {
            setGoid(otherDescriptor.getGoid());
        }
        setSolutionKit(ownerSolutionKit);
        setEntityId(otherDescriptor.getEntityId());
        setEntityType(otherDescriptor.getEntityType());
        setReadOnly(otherDescriptor.isReadOnly());
    }

    /**
     * Create a new {@code EntityOwnershipDescriptor} instance by coping data from the specified {@code otherDescriptor},
     * and set the owner to the specified {@code ownerSolutionKit}.
     *
     * @param otherDescriptor     the {@code EntityOwnershipDescriptor} from which to copy properties.  Required and cannot be {@code null}.
     * @param ownerSolutionKit    the owner {@code SolutionKit}.  Required and cannot be {@code null}.
     * @return a copy of the specified {@code otherDescriptor} with the specified {@code ownerSolutionKit}.
     */
    public static EntityOwnershipDescriptor createFrom(
            @NotNull final EntityOwnershipDescriptor otherDescriptor,
            @NotNull final SolutionKit ownerSolutionKit
    ) {
        //noinspection deprecation
        final EntityOwnershipDescriptor descriptor = new EntityOwnershipDescriptor();
        descriptor.copyFrom(otherDescriptor, ownerSolutionKit);
        return descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityOwnershipDescriptor)) return false;
        if (!super.equals(o)) return false;

        final EntityOwnershipDescriptor that = (EntityOwnershipDescriptor) o;

        // there is no need to check for owner solutionKit equality (pattern from other entities see Role->Permissions)
        return !(entityId != null ? !entityId.equals(that.entityId) : that.entityId != null)
                && entityType == that.entityType
                && readOnly == that.readOnly;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // same as equals, there is no need to calc hash for owner solutionKit (pattern from other entities see Role->Permissions)
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        result = 31 * result + Boolean.hashCode(readOnly);
        return result;
    }
}
