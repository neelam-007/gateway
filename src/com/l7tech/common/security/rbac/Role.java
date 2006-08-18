/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.identity.User;

import java.util.HashSet;
import java.util.Set;

/**
 * A Role groups zero or more {@link Permission}s so they can be assigned as a whole
 * to individual users using {@link UserRoleAssignment}s.  Roles do not point back to
 * identities, because:
 * <ul>
 * <li>They can be assigned to multiple identites;
 * <li>They may in future be assigned at runtime, rather than statically.
 * </ul>
 * @author alex
 */
public class Role extends NamedEntityImp {
    public static final int ADMIN_ROLE_OID = -3;

    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<UserRoleAssignment> userAssignments = new HashSet<UserRoleAssignment>();

    public Set<Permission> getPermissions() {
        return permissions;
    }

    protected void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<UserRoleAssignment> getUserAssignments() {
        return userAssignments;
    }

    protected void setUserAssignments(Set<UserRoleAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link ObjectIdentityPredicate} for the provided ID.
     */
    public void addPermission(OperationType operation, EntityType etype, String id) {
        Permission perm = new Permission(this, operation, etype);
        perm.getScope().add(new ObjectIdentityPredicate(perm, id));
        permissions.add(perm);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link AttributePredicate} for the provided attribute name and value.
     * @throws IllegalArgumentException if the provided attribute name doesn't match a getter on the class represented 
     *         by the provided EntityType.
     */
    public void addPermission(OperationType operation,
                              EntityType etype,
                              String attr,
                              String name)
                       throws IllegalArgumentException {
        Permission perm = new Permission(this, operation, etype);
        perm.getScope().add(new AttributePredicate(perm, attr, name));
        permissions.add(perm);
    }

    /**
     * Creates and adds a new {@link UserRoleAssignment} assigning the provided {@link User} to this Role.
     */
    public void addAssignedUser(User user) {
        userAssignments.add(new UserRoleAssignment(this, user.getProviderId(), user.getId()));
    }

    public String toString() {
        return _name;
    }
}
