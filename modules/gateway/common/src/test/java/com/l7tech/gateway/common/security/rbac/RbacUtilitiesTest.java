package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import org.junit.Test;

import static org.junit.Assert.*;

public class RbacUtilitiesTest {
    @Test
    public void getSecurityZoneRoleDescription() {
        final Role role = new Role();
        role.setName("Manage Test Zone");
        role.setEntityType(EntityType.SECURITY_ZONE);
        role.setDescription("Users in the {0} role can manage entities in the {1} zone.");
        assertEquals("Users in the Manage Test Zone role can manage entities in the Test zone.", RbacUtilities.getDescriptionString(role, false));
    }
}
