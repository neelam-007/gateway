package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityType;
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

    private boolean readOnly = true;

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

    public void setSolutionKit(@NotNull SolutionKit solutionKit) {
        this.solutionKit = solutionKit;
    }

    @Column(name="entity_id", nullable=false)
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(@NotNull String entityId) {
        this.entityId = entityId;
    }

    @Transient
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(@NotNull EntityType entityType) {
        this.entityType = entityType;
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Deprecated
    @SuppressWarnings("unused")
    @Column(name="entity_type", nullable=false, length=255)
    protected String getEntityTypeName() {
        if (entityType == null) return null;
        return entityType.name();
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Deprecated
    @SuppressWarnings("unused")
    protected void setEntityTypeName(@NotNull String typeName) {
        entityType = EntityType.valueOf(typeName);
    }

    @Column(name="read_only")
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityOwnershipDescriptor)) return false;
        if (!super.equals(o)) return false;

        EntityOwnershipDescriptor that = (EntityOwnershipDescriptor) o;

        return !(solutionKit != null ? !solutionKit.getGoid().equals(that.solutionKit.getGoid()) : that.solutionKit.getGoid() != null)
                && !(entityId != null ? !entityId.equals(that.entityId) : that.entityId != null)
                && entityType == that.entityType
                && readOnly == that.readOnly;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 197 * result + (solutionKit.getGoid() != null ? solutionKit.getGoid().hashCode() : 0);
        result = 197 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 197 * result + (entityType != null ? entityType.name().hashCode() : 0);
        result = 197 * result + (readOnly ? 1 : 0);
        return result;
    }
}
