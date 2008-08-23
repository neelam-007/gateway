/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.Aliasable;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Cascade;

/**
 * A permission that belongs to a {@link Role}.
 *
 * Changes to this class *must* maintain up-to-date {@link #equals} and {@link #hashCode}, as
 * instances are routinely added to {@link java.util.Set}s.
 */
@javax.persistence.Entity
@Table(name="rbac_permission")
public class Permission extends PersistentEntityImp implements Cloneable {
    private Role role;
    private OperationType operation;
    private String otherOperationName;
    private EntityType entityType;
    private Set<ScopePredicate> scope = new HashSet<ScopePredicate>();

    /**
     * Construct a new Permission attached to the given Role.
     * @param role the Role to which this Permission belongs
     * @param operation the Operation to which this Permission applies, or <code>null</code> if not yet known.
     * @param entityType the type of entity to which this Permission applies, {@link EntityType#ANY} if it applies to
     *                   any entity, or <code>null</code> if not yet known.
     */
    public Permission(Role role, OperationType operation, EntityType entityType) {
        if (role == null) throw new NullPointerException();
        this.role = role;
        this.operation = operation;
        this.entityType = entityType;
    }

    protected Permission() { }

    /**
     * Returns true if this permission is applicable to the specified {@link com.l7tech.objectmodel.Entity}.
     *
     * In order to be considered applicable, one of the following conditions must be met:
     * <ul>
     * <li>This Permission's {@link #entityType} is {@link EntityType#ANY}, <strong>or:</strong></li>
     * <li>This Permission's {@link #entityType} matches the specified Entity, and {@link ScopePredicate#matches}
     * returns <code>true</code> for <em>every</em> predicate in {@link #scope}.</li>
     * </ul>
     * @param entity the entity to test for applicability
     * @return true if this Permission applies to the specified Entity; false otherwise.
     */
    public boolean matches(Entity entity) {
        if (entityType == EntityType.ANY) return true; // No scope is relevant for ANY
        Class<? extends Entity> eclass;
        if (entity instanceof AnonymousEntityReference) {
            eclass = ((AnonymousEntityReference)entity).getEntityClass();
        } else if (entity == null) {
            return false;
        } else {
            eclass = entity.getClass();
        }
        if(entity instanceof Aliasable){
            Aliasable aliasable = (Aliasable) entity;
            if(aliasable.isAlias()){
                //todo [Donal] until the behaviour of aliases is defined for rbac return false
                return false;
            }
        }
        if (!entityType.getEntityClass().isAssignableFrom(eclass)) return false;
        for (ScopePredicate predicate : scope) {
            if (!predicate.matches(entity, eclass)) return false;
        }
        return true;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="role_oid", nullable=false)
    public Role getRole() {
        return role;
    }

    protected void setRole(Role role) {
        this.role = role;
    }

    @Transient
    public OperationType getOperation() {
        return operation;
    }

    /**
     * If specified, this set of ScopePredicates specifies how this Permission applies to some subset of
     * the entities of type {@link #entityType}.  If unspecified, this Permission applies to all entities of
     * type {@link #entityType}.
     */
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="permission")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<ScopePredicate> getScope() {
        return scope;
    }

    @Transient
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public void setOperation(OperationType operation) {
        if (operation == OperationType.NONE) throw new IllegalArgumentException("operation must not be " + OperationType.NONE.name());
        this.operation = operation;
    }

    public void setScope(Set<ScopePredicate> scope) {
        this.scope = scope;
    }

    @Column(name="other_operation", length=255)
    public String getOtherOperationName() {
        return otherOperationName;
    }

    public void setOtherOperationName(String otherOperationName) {
        this.otherOperationName = otherOperationName;
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Column(name="entity_type", nullable=false, length=255)
    protected String getEntityTypeName() {
        if (entityType == null) return null;
        return entityType.name();
    }

    /** @deprecated only here to hide enums from Hibernate */
    protected void setEntityTypeName(String typeName) {
        if (typeName == null) return;
        entityType = EntityType.valueOf(typeName);
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Column(name="operation_type", nullable=false, length=16)
    protected String getOperationTypeName() {
        if (operation == null) return null;
        return operation.name();
    }

    /** @deprecated only here to hide enums from Hibernate */
    protected void setOperationTypeName(String name) {
        if (name == null) return;
        operation = OperationType.valueOf(name);
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Permission that = (Permission) o;

        if (entityType != that.entityType) return false;
        if (operation != that.operation) return false;
        if (otherOperationName != null ? !otherOperationName.equals(that.otherOperationName) : that.otherOperationName != null)
            return false;
        if (role != null ? !role.equals(that.role) : that.role != null) return false;

        return true;
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Transient
    public Permission getAnonymousClone() {
        try {
            Permission perm = (Permission) clone();
            perm.setRole(null);
            return perm;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (otherOperationName != null ? otherOperationName.hashCode() : 0);
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "<permission id=\"" + getOid() + "\" op=\"" + operation + "\" type=\"" + entityType + "\" scope=\"" + scope + "\"/>";
    }

    
}
