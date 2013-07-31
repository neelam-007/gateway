package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class PermissionGroupTest {
    private Set<Permission> permissions;
    private Role role;

    @Before
    public void setup() {
        role = new Role();
        permissions = new HashSet<>();
    }

    @Test
    public void addPermission() {
        final PermissionGroup group = new PermissionGroup(EntityType.POLICY, new HashSet<ScopePredicate>());
        group.addPermission(new Permission(role, OperationType.READ, EntityType.POLICY));
        assertEquals(1, group.getPermissions().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPermissionInvalidEntityType() {
        final PermissionGroup group = new PermissionGroup(EntityType.POLICY, new HashSet<ScopePredicate>());
        group.addPermission(new Permission(role, OperationType.READ, EntityType.SERVICE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPermissionInvalidScope() {
        final PermissionGroup group = new PermissionGroup(EntityType.POLICY, new HashSet<ScopePredicate>());
        final Permission permission = new Permission(role, OperationType.READ, EntityType.SERVICE);
        permission.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission, "1234")));
        group.addPermission(permission);
    }

    @Test
    public void groupPermissionsNone() {
        assertTrue(PermissionGroup.groupPermissions(permissions).isEmpty());
    }

    @Test
    public void groupPermissionsNoScope() {
        permissions.add(new Permission(role, OperationType.CREATE, EntityType.POLICY));
        permissions.add(new Permission(role, OperationType.CREATE, EntityType.SERVICE));
        permissions.add(new Permission(role, OperationType.READ, EntityType.POLICY));
        permissions.add(new Permission(role, OperationType.READ, EntityType.SERVICE));
        permissions.add(new Permission(role, OperationType.UPDATE, EntityType.POLICY));
        permissions.add(new Permission(role, OperationType.UPDATE, EntityType.SERVICE));
        permissions.add(new Permission(role, OperationType.DELETE, EntityType.POLICY));
        permissions.add(new Permission(role, OperationType.DELETE, EntityType.SERVICE));
        permissions.add(new Permission(role, OperationType.OTHER, EntityType.POLICY));
        permissions.add(new Permission(role, OperationType.NONE, EntityType.POLICY));

        final Set<PermissionGroup> groups = PermissionGroup.groupPermissions(permissions);
        assertEquals(2, groups.size());
        final Map<EntityType, PermissionGroup> groupMap = new HashMap<>();
        for (final PermissionGroup group : groups) {
            groupMap.put(group.getEntityType(), group);
        }
        final PermissionGroup policyGroup = groupMap.get(EntityType.POLICY);
        assertEquals(6, policyGroup.getPermissions().size());
        final PermissionGroup serviceGroup = groupMap.get(EntityType.SERVICE);
        assertEquals(4, serviceGroup.getPermissions().size());
    }

    @Test
    public void groupPermissionsWithScope() {
        final Permission permission1 = new Permission(role, OperationType.CREATE, EntityType.POLICY);
        permission1.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission1, "1111")));

        final Permission permission2 = new Permission(role, OperationType.CREATE, EntityType.SERVICE);
        permission2.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission2, "2222")));

        permissions.add(permission1);
        permissions.add(permission2);

        final Set<PermissionGroup> groups = PermissionGroup.groupPermissions(permissions);
        assertEquals(2, groups.size());
        final Map<EntityType, PermissionGroup> groupMap = new HashMap<>();
        for (final PermissionGroup group : groups) {
            groupMap.put(group.getEntityType(), group);
        }
        final PermissionGroup policyGroup = groupMap.get(EntityType.POLICY);
        assertEquals(1, policyGroup.getPermissions().size());
        final PermissionGroup serviceGroup = groupMap.get(EntityType.SERVICE);
        assertEquals(1, serviceGroup.getPermissions().size());
    }

    @Test
    public void groupPermissionsSameTypeDifferentScope() {
        final Permission permission1 = new Permission(role, OperationType.CREATE, EntityType.POLICY);
        permission1.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission1, "1111")));

        final Permission permission2 = new Permission(role, OperationType.CREATE, EntityType.POLICY);
        permission2.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission2, "2222")));

        permissions.add(permission1);
        permissions.add(permission2);

        final Set<PermissionGroup> groups = PermissionGroup.groupPermissions(permissions);
        assertEquals(2, groups.size());
    }

    @Test
    public void groupPermissionsNullEntityType() {
        permissions.add(new Permission(role, OperationType.CREATE, null));

        final Set<PermissionGroup> groups = PermissionGroup.groupPermissions(permissions);
        assertEquals(1, groups.size());
        final PermissionGroup group = groups.iterator().next();
        assertEquals(1, group.getPermissions().size());
        assertNull(group.getEntityType());
    }

    @Test
    public void groupPermissionsNullScope() {
        final Permission permission = new Permission(role, OperationType.CREATE, EntityType.POLICY);
        permission.setScope(null);
        permissions.add(permission);

        final Set<PermissionGroup> groups = PermissionGroup.groupPermissions(permissions);
        assertEquals(1, groups.size());
        final PermissionGroup group = groups.iterator().next();
        assertEquals(1, group.getPermissions().size());
        assertTrue(group.getScope().isEmpty());
    }
}

