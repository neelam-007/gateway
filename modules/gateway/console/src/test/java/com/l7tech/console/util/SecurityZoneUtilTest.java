package com.l7tech.console.util;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SecurityZoneUtilTest {
    private SecurityZone zone;
    private Set<EntityType> entityTypes;
    private List<Permission> permissions;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("Test");
        zone.setOid(1234L);
        entityTypes = new HashSet<>();
        permissions = new ArrayList<>();
    }

    @Test
    public void validZoneSpecificSecurityZonePredicate() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, zone));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneMultipleSpecificSecurityZonePredicate() {
        final SecurityZone mismatchZone = new SecurityZone();
        mismatchZone.setName("Mismatch");
        mismatchZone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, mismatchZone, zone));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneMultiplePermissions() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        // mismatching permission
        permissions.add(createPermission(OperationType.CREATE, EntityType.POLICY, zone));
        // matching permission
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, zone));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneMultipleEntityTypes() {
        entityTypes.add(EntityType.POLICY);
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, zone));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneNotAnySecurityZonePredicate() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, (SecurityZone) null));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(SecurityZoneUtil.NULL_ZONE, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneNoScope() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(new Permission(new Role(), OperationType.CREATE, EntityType.SERVICE));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneAnyEntityTypeNoScope() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(new Permission(new Role(), OperationType.CREATE, EntityType.ANY));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void validZoneNullRequiredOperation() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, null, permissions));
    }

    @Test
    public void validZoneNullEntityTypes() {
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, zone));
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, null, OperationType.CREATE, permissions));
    }

    /**
     * If permissions have scope but scope does not contain any SecurityZonePredicate, we can't tell if they are restricted by zone so we give them the benefit of the doubt.
     */
    @BugId("SSG-7061")
    @Test
    public void validZoneNoSecurityZonePredicates() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        // user is allowed to update a specific service
        final Permission permission = new Permission(new Role(), OperationType.UPDATE, EntityType.SERVICE);
        permission.setScope(Collections.<ScopePredicate>singleton(new ObjectIdentityPredicate(permission, 1234L)));
        permissions.add(permission);
        assertTrue(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.UPDATE, permissions));
    }

    @Test
    public void invalidZoneMismatchEntityType() {
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        entityTypes.add(EntityType.POLICY);
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void invalidZoneMismatchMultipleEntityTypes() {
        // zone must support both SERVICE and POLICY entity types
        entityTypes.add(EntityType.SERVICE);
        entityTypes.add(EntityType.POLICY);
        // zone only supports SERVICE entity type
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void invalidZoneNonNullOperationNullPermissions() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, null));
    }

    @Test
    public void invalidZoneMismatchSecurityZonePredicate() {
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        entityTypes.add(EntityType.SERVICE);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, new SecurityZone()));
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void invalidZoneMismatchNullSecurityZonePredicate() {
        entityTypes.add(EntityType.SERVICE);
        zone.setPermittedEntityTypes(entityTypes);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, (SecurityZone) null));
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void invalidNullZoneMismatchSecurityZonePredicate() {
        entityTypes.add(EntityType.SERVICE);
        permissions.add(createPermission(OperationType.CREATE, EntityType.SERVICE, zone));
        assertFalse(SecurityZoneUtil.isZoneValidForOperation(SecurityZoneUtil.NULL_ZONE, entityTypes, OperationType.CREATE, permissions));
    }

    @Test
    public void getAllZoneableEntityTypes() {
        assertFalse(SecurityZoneUtil.getAllZoneableEntityTypes().isEmpty());
    }

    private Permission createPermission(final OperationType operation, final EntityType entityType, final SecurityZone... zones) {
        final Permission p = new Permission(new Role(), operation, entityType);
        final Set<ScopePredicate> scope = new HashSet<>(zones.length);
        for (final SecurityZone securityZone : zones) {
            scope.add(new SecurityZonePredicate(p, securityZone));
        }
        p.setScope(scope);
        return p;
    }
}
