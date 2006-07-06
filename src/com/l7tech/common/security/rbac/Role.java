/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.HashSet;
import java.util.Set;

/**
 * A Role groups zero or more {@link Permission}s so they can be assigned as a whole
 * to individual identities using {@link IdentityRoleAssignment}s.  Roles do not point back to
 * identities, because:
 * <ul>
 * <li>They can be shared by multiple identites;
 * <li>They may be assigned at runtime, rather than statically.
 * </ul>
 * @author alex
 */
public class Role extends NamedEntityImp {
    private Set<Permission> permissions = new HashSet<Permission>();

    public Set<Permission> getPermissions() {
        return permissions;
    }

    protected void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public String toString() {
        return "<role id=\"" + getOid() + "\" name=\"" + getName() + "\" permissions=\"" + getPermissions() + "\"/>";
    }
}
