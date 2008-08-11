/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Table;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Version;
import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * A predicate in the "scope expression" of a {@link Permission}.
 *
 * Subclasses *must* implement {@link #equals(Object)} and {@link #hashCode()}, as instances are added
 * to {@link java.util.Set}s.
 */
@javax.persistence.Entity
@Table(name="rbac_predicate")
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class ScopePredicate extends PersistentEntityImp {
    protected Permission permission;

    protected ScopePredicate(Permission permission) {
        this.permission = permission;
    }

    protected ScopePredicate() {
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="permission_oid", nullable=false)
    public Permission getPermission() {
        return permission;
    }

    protected void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * @param entity the Entity to evaluate against the predicate
     * @param eclass the "real" class of the entity, which may be different from entity.getClass() if entity is an
     *        AnonymousEntityReference
     * @return true if this predicate matches the given entity.
     */
    abstract boolean matches(Entity entity, Class<? extends Entity> eclass);
}
