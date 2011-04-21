/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.security.rbac;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

public class RoleAssignmentTest {

    /**
     * RoleMangerImpl uses some RoleAssignment properties in HSQL. This test case will capture if these properties
     * are modified and break the HSQL.
     */
    @Test
    public void testHsqlProperties() throws Exception{

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
}
