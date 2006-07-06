/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.imp.EntityImp;

/**
 * A predicate in the "scope expression" of a {@link Permission}.
 *
 * Subclasses *must* implement {@link #equals} and {@link #hashCode}, as instances are added
 * to {@link java.util.Set}s.
 */
public abstract class ScopePredicate extends EntityImp {
    protected Permission permission;

    protected ScopePredicate(Permission permission) {
        this.permission = permission;
    }

    protected ScopePredicate() {
    }

    public Permission getPermission() {
        return permission;
    }

    protected void setPermission(Permission permission) {
        this.permission = permission;
    }

    public abstract boolean matches(Entity entity);
}
