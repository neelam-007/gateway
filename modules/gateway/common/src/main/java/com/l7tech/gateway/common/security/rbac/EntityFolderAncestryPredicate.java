package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.Size;

@javax.persistence.Entity
@Proxy(lazy=false)
@Table(name="rbac_predicate_entityfolder")
public class EntityFolderAncestryPredicate extends ScopePredicate {
    private String entityId;
    private EntityType entityType;

    public EntityFolderAncestryPredicate(Permission permission, EntityType entityType, String entityId) {
        super(permission);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityFolderAncestryPredicate(Permission permission, EntityType entityType, Goid targetEntityGoid) {
        super(permission);
        this.entityType = entityType;
        this.entityId = Goid.toString(targetEntityGoid);
    }

    @Deprecated
    protected EntityFolderAncestryPredicate() { }

    @Override
    public ScopePredicate createAnonymousClone() {
        EntityFolderAncestryPredicate copy = new EntityFolderAncestryPredicate(null, entityType, new Goid(entityId));
        copy.setGoid(this.getGoid());
        return copy;
    }

    @Size(max = 255)
    @Column(name="entity_id", length=255)
    public String getEntityId() {
        return entityId;
    }

    @Column(name="entity_type", length=64)
    @Enumerated(value=EnumType.STRING)
    public EntityType getEntityType() {
        return entityType;
    }

    @Deprecated
    protected void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    @Deprecated
    protected void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityFolderAncestryPredicate that = (EntityFolderAncestryPredicate) o;

        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        if (entityType != that.entityType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        return result;
    }
}
