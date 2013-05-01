package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * An entity for GUI sugar purposes that represents a group of permissions that share the same scope and
 * entity type.
 */
public final class PermissionGroup {

    public static final EnumSet<OperationType> GROUPABLE_OPERATIONS = EnumSet.of(OperationType.CREATE, OperationType.READ, OperationType.UPDATE, OperationType.DELETE);
    final EnumSet<OperationType> operations = EnumSet.noneOf(OperationType.class);
    final Set<Permission> permissions = new HashSet<Permission>();

    @NotNull
    public Set<OperationType> getOperations() {
        return operations;
    }

    @NotNull
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Set the permissions that belong to this permission group.  All permissions must share the same scope
     * and entity type.
     *
     * @param permissions a permission set with the same scopes and entity types.  Must be non-null and non-empty.
     */
    public void setPermissions(@NotNull Set<Permission> permissions) {
        EnumSet<OperationType> ops = EnumSet.noneOf(OperationType.class);

        if (!permissions.isEmpty()) {
            // Ensure entity types and scopes all match
            Iterator<Permission> it = permissions.iterator();
            Permission first = it.next();
            EntityType needType = notnull("first permission's entity type", first.getEntityType());
            Set<ScopePredicate> needScope = notnull("first permission's scope", first.getScope());
            ops.add(first.getOperation());

            while (it.hasNext()) {
                Permission perm = it.next();
                EntityType gotType = notnull("a subsequent permission's entity type", perm.getEntityType());
                Set<ScopePredicate> gotScope = notnull("a subsequent permission's scope", perm.getScope());

                if (!gotType.equals(needType))
                    throw new IllegalArgumentException("new permissions does not have the same entity type as others already in this permission group (saw " + gotType + ", needed " + needType + ")");
                if (!scopesAreEqual(gotScope, needScope))
                    throw new IllegalArgumentException("new permissions does not have the same scope as others already in this permission group (saw " + gotScope + ", needed " + needScope + ")");

                final OperationType operation = perm.getOperation();
                assertGroupable(operation);
                ops.add(operation);
            }
        }

        this.permissions.clear();
        this.operations.clear();
        this.permissions.addAll(permissions);
        this.operations.addAll(ops);
    }

    /**
     * Add a new permission to this permission group.  The permission added must have the same entity type and scope as
     * existing permissions (if any) within this group.
     *
     * @param perm the permission to add.  Required.
     */
    public void addPermission(@NotNull Permission perm) {
        if (!permissions.isEmpty()) {
            Permission first = permissions.iterator().next();
            EntityType needType = notnull("first permission's entity type", first.getEntityType());
            Set<ScopePredicate> needScope = notnull("first permission's scope", first.getScope());
            EntityType gotType = notnull("a subsequent permission's entity type", perm.getEntityType());
            Set<ScopePredicate> gotScope = notnull("a subsequent permission's scope", perm.getScope());

            if (!gotType.equals(needType))
                throw new IllegalArgumentException("not all permissions have the same entity type (saw " + needType + " and " + gotType + ")");
            if (!scopesAreEqual(needScope, gotScope))
                throw new IllegalArgumentException("not all permissions have the same scope (saw " + needScope + " and " + gotScope + ")");

            final OperationType operation = perm.getOperation();
            assertGroupable(operation);
        }

        permissions.add(perm);
        operations.add(perm.getOperation());
    }

    /**
     * @return a string like "C R   D" showing which operations are enabled in this permission group.
     */
    public String getOperationString() {
        Set<Permission> perms = getPermissions();
        if (perms.isEmpty())
            return "<None>";

        Set<OperationType> ops = getOperations();
        if (ops.contains(OperationType.OTHER))
            return perms.iterator().next().getOtherOperationName();

        StringBuilder sb = new StringBuilder();
        sb.append(ops.contains(OperationType.CREATE) ? "Cr " : "   ");
        sb.append(ops.contains(OperationType.READ)   ? "Rd " : "   ");
        sb.append(ops.contains(OperationType.UPDATE) ? "Up " : "   ");
        sb.append(ops.contains(OperationType.DELETE) ? "De " : "   ");
        return sb.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PermissionGroup that = (PermissionGroup) o;

        if (permissions != null ? !permissions.equals(that.permissions) : that.permissions != null) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        return permissions != null ? permissions.hashCode() : 0;
    }

    @NotNull
    private static <T> T notnull(String what, @Nullable T ref) throws NullPointerException {
        if (ref == null)
            throw new NullPointerException(what + " must not be null");
        return ref;
    }

    /**
     * Group the specified permissions into the minimum number of permission groups.
     *
     * @param permissions set of permissions to examine.
     * @return a set of PermissionGroups that is no larger, but will hopefully be smaller, than the set of permissions.
     *         Multiple permissions in the input set that differ only by operation type (that is, that share the same
     *         entity type and scope) will be grouped into a single permission group.
     *         <p/>
     *         Operation types of OTHER cannot be grouped into a PermissionGroup because they have individual
     *         special operation names.
     */
    @NotNull
    public static Set<PermissionGroup> groupPermissions(@NotNull Set<Permission> permissions) {
        Map<Pair<EntityType, Set<ScopePredicate>>, PermissionGroup> groups = new LinkedHashMap<Pair<EntityType, Set<ScopePredicate>>, PermissionGroup>();

        for (Permission permission : permissions) {
            // Create lookup key (entity type and scope)
            final EntityType entityType = permission.getEntityType();
            final Set<ScopePredicate> scope = getAnonymousNoOidsCopyOfScope(permission.getScope());
            Pair<EntityType, Set<ScopePredicate>> key = new Pair<EntityType, Set<ScopePredicate>>(entityType, scope);

            final boolean groupable = isGroupable(permission.getOperation());
            PermissionGroup group = groupable ? groups.get(key) : null;
            if (group == null) {
                group = new PermissionGroup();
                groups.put(key, group);
            }

            group.addPermission(permission);
        }

        return new HashSet<PermissionGroup>(groups.values());
    }

    /**
     * Scan the specified permission groups and return the group that matches the specified permission.
     * If no group matches, returns null.
     *
     * @param existingGroups permissiong roups to scan.  Required but may be empty.
     * @param permission permission to add.  Required.
     * @return a permission group that has the same entity type and scope as the specified permission, or null if none match.
     */
    public static PermissionGroup findGroup(@NotNull Collection<PermissionGroup> existingGroups, @NotNull Permission permission) {
        for (PermissionGroup group : existingGroups) {
            if (!group.permissions.isEmpty()) {
                Permission existing = group.permissions.iterator().next();
                if (permission.getEntityType() == null || !permission.getEntityType().equals(existing.getEntityType()))
                    continue;
                if (permission.getScope() == null || !permission.getScope().equals(existing.getScope()))
                    continue;
                return group;
            }
        }
        return null;
    }

    private static void assertGroupable(OperationType operation) {
        if (!isGroupable(operation))
            throw new IllegalArgumentException("a permission with an operation type of " + operation + " must be the only permission in a permission group");
    }

    private static boolean isGroupable(OperationType operation) {
        return GROUPABLE_OPERATIONS.contains(operation);
    }

    private static Set<ScopePredicate> getAnonymousNoOidsCopyOfScope(Set<ScopePredicate> in) {
        final Set<ScopePredicate> out = new HashSet<ScopePredicate>();
        for (ScopePredicate scopePredicate : in) {
            final ScopePredicate clone = scopePredicate.createAnonymousClone();
            clone.setOid(-1);
            out.add(clone);
        }
        return out;
    }

    private static boolean scopesAreEqual(Set<ScopePredicate> left, Set<ScopePredicate> right) {
        return getAnonymousNoOidsCopyOfScope(left).equals(getAnonymousNoOidsCopyOfScope(right));
    }
}
