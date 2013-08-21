/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;

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

    @Deprecated
    protected ScopePredicate() { }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="permission_goid", nullable=false)
    public Permission getPermission() {
        return permission;
    }

    protected void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * Get a copy of this scope predicate that has the same OID but whose @{link #getPermission} method
     * will return null.
     * <p/>
     * The general contract is that sp.createAnonymousClone().equals(sp) and sp.createAnonymousClone().hashCode() == sp.hashCode().
     *
     * @return a new copy of this scope predicate.  Never null.
     */
    public abstract ScopePredicate createAnonymousClone();
}
