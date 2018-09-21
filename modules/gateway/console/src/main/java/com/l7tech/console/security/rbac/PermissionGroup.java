package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * An entity for GUI sugar purposes that represents a group of permissions that share the same scope and entity type.
 */
public class PermissionGroup {
    private final EnumSet<OperationType> operations = EnumSet.noneOf(OperationType.class);
    private final Set<Permission> permissions = new HashSet<>();
    private final EntityType entityType;
    private final Set<ScopePredicate> scope;
    private  String scopedDescription;

    /**
     * @param entityType the EntityType which applies to this PermissionGroup (optional because a Permission may not have an entity type).
     * @param scope      the set of ScopePredicate which applies to this PermissionGroup (optional because a Permission may not have a scope).
     */
    public PermissionGroup(@Nullable final EntityType entityType, @Nullable final Set<ScopePredicate> scope) {
        this.entityType = entityType;
        this.scope = scope != null ? scope : Collections.<ScopePredicate>emptySet();
    }

    @Nullable
    public EntityType getEntityType() {
        return entityType;
    }

    @NotNull
    public Set<ScopePredicate> getScope() {
        return scope;
    }

    @NotNull
    public Set<OperationType> getOperations() {
        return operations;
    }

    @NotNull
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Add a new permission to this permission group.  The permission added must have the same entity type and scope as the PermissionGroup.
     *
     * @param perm the permission to add.  Required.
     */
    public void addPermission(@NotNull Permission perm) {
        if (!ObjectUtils.equals(entityType, perm.getEntityType())) {
            throw new IllegalArgumentException("Expected entity type " + entityType + " but received " + perm.getEntityType());
        }
        if (!scopesAreEqual(scope, perm.getScope())) {
            throw new IllegalArgumentException("Expected scope " + scope + " but received " + perm.getScope());
        }
        permissions.add(perm);
        operations.add(perm.getOperation());
    }

    /**
     * @return a set of other operation names if this PermissionGroup has any permissions with OperationType#OTHER.
     */
    public Set<String> getOtherOperations() {
        final Set<String> otherOps = new TreeSet<>();
        for (final Permission permission : permissions) {
            if (permission.getOperation() == OperationType.OTHER) {
                otherOps.add(permission.getOtherOperationName());
            }
        }
        return otherOps;
    }

    /**
     * Group the specified permissions into the minimum number of permission groups.
     *
     * @param permissions set of permissions to examine.
     * @return a set of PermissionGroups that is no larger, but will hopefully be smaller, than the set of permissions.
     *         Multiple permissions in the input set that differ only by operation type (that is, that share the same
     *         entity type and scope) will be grouped into a single permission group.
     */
    @NotNull
    public static Set<PermissionGroup> groupPermissions(@NotNull final Set<Permission> permissions) {
        Map<Pair<EntityType, Set<ScopePredicate>>, PermissionGroup> groups = groupPermissionScopes(permissions);
        return new HashSet<>(groups.values());
    }
    @NotNull
    public static Map<Pair<EntityType, Set<ScopePredicate>>, PermissionGroup> groupPermissionScopes(@NotNull final Set<Permission> permissions) {
        final Map<Pair<EntityType, Set<ScopePredicate>>, PermissionGroup> groups = new LinkedHashMap<>();
        for (final Permission permission : permissions) {
            final EntityType entityType = permission.getEntityType();
            final Set<ScopePredicate> scope = RbacUtilities.getAnonymousNoOidsCopyOfScope(permission.getScope());
            // Create lookup key (entity type and scope)
            final Pair<EntityType, Set<ScopePredicate>> key = new Pair<>(entityType, scope);
            PermissionGroup group = groups.get(key);
            if (group == null) {
                group = new PermissionGroup(entityType, scope);
                groups.put(key, group);
            }
            group.addPermission(permission);
        }
        return groups;
    }

    private static boolean scopesAreEqual(final Set<ScopePredicate> left, final Set<ScopePredicate> right) {
        return RbacUtilities.getAnonymousNoOidsCopyOfScope(left).equals(RbacUtilities.getAnonymousNoOidsCopyOfScope(right));
    }

    public String getScopedDescription() {
        return scopedDescription;
    }

    public void setScopedDescription(final String scopedDescription) {
        this.scopedDescription = scopedDescription;
    }
}
