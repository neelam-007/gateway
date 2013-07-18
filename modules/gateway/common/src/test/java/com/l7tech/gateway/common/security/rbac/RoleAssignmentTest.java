/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class RoleAssignmentTest {

    /**
     * RoleMangerImpl uses some RoleAssignment properties in HSQL. This test case will capture if these properties
     * are modified and break the HSQL.
     */
    @Test
    public void testHsqlProperties() throws Exception {

        final Class<?> roleAssignmentClass = Class.forName("com.l7tech.gateway.common.security.rbac.RoleAssignment");
        final Object roleAssignment = roleAssignmentClass.newInstance();
        final String msg = "Property should have been found, if not HSQL needs to be updated";

        final Field identityId = roleAssignment.getClass().getDeclaredField("identityId");
        Assert.assertNotNull(msg, identityId);

        final Field providerId = roleAssignment.getClass().getDeclaredField("providerId");
        Assert.assertNotNull(msg, providerId);

        final Field entityType = roleAssignment.getClass().getDeclaredField("entityType");
        Assert.assertNotNull(msg, entityType);
    }

    @Test
    public void testHashCodeSameRoleOid() {
        final Role r1 = new Role();
        r1.setOid(1L);
        r1.setName("r1");
        final Role r2 = new Role();
        r2.setOid(1L);
        r2.setName("r2");
        final RoleAssignment ra1 = new RoleAssignment(r1, 1L, "1", EntityType.USER);
        final RoleAssignment ra2 = new RoleAssignment(r2, 1L, "1", EntityType.USER);
        assertEquals(ra1.hashCode(), ra2.hashCode());
    }

    @Test
    public void testHashCodeDifferentRoleOid() {
        final Role r1 = new Role();
        r1.setOid(1L);
        final Role r2 = new Role();
        r2.setOid(2L);
        final RoleAssignment ra1 = new RoleAssignment(r1, 1L, "1", EntityType.USER);
        final RoleAssignment ra2 = new RoleAssignment(r2, 1L, "1", EntityType.USER);
        assertFalse(ra1.hashCode() == ra2.hashCode());
    }

    @Test
    public void testHashCodeNullRole() {
        final RoleAssignment ra1 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra1.setRole(null);
        final RoleAssignment ra2 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra2.setRole(null);
        assertEquals(ra1.hashCode(), ra2.hashCode());
    }

    @Test
    public void testEqualsSameRoleOid() {
        final Role r1 = new Role();
        r1.setOid(1L);
        r1.setName("r1");
        final Role r2 = new Role();
        r2.setOid(1L);
        r2.setName("r2");
        final RoleAssignment ra1 = new RoleAssignment(r1, 1L, "1", EntityType.USER);
        final RoleAssignment ra2 = new RoleAssignment(r2, 1L, "1", EntityType.USER);
        assertTrue(ra1.equals(ra2));
        assertTrue(ra2.equals(ra1));
    }

    @Test
    public void testEqualsDifferentRoleOid() {
        final Role r1 = new Role();
        r1.setOid(1L);
        final Role r2 = new Role();
        r2.setOid(2L);
        final RoleAssignment ra1 = new RoleAssignment(r1, 1L, "1", EntityType.USER);
        final RoleAssignment ra2 = new RoleAssignment(r2, 1L, "1", EntityType.USER);
        assertFalse(ra1.equals(ra2));
        assertFalse(ra2.equals(ra1));
    }

    @Test
    public void testEqualsNullRoles() {
        final RoleAssignment ra1 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra1.setRole(null);
        final RoleAssignment ra2 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra2.setRole(null);
        assertTrue(ra1.equals(ra2));
        assertTrue(ra2.equals(ra1));
    }

    @Test
    public void testEqualsOneNullRole() {
        final RoleAssignment ra1 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra1.setRole(null);
        final RoleAssignment ra2 = new RoleAssignment(new Role(), 1L, "1", EntityType.USER);
        ra2.setRole(new Role());
        assertFalse(ra1.equals(ra2));
        assertFalse(ra2.equals(ra1));
    }
}
