package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

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

    @Test
    public void getAnonymousNoOidsCopyOfScope() {
        final SecurityZonePredicate pred = new SecurityZonePredicate(new Permission(new Role(), OperationType.CREATE, EntityType.POLICY), new SecurityZone());
        pred.setOid(1234L);

        final Set<ScopePredicate> copy = RbacUtilities.getAnonymousNoOidsCopyOfScope(Collections.<ScopePredicate>singleton(pred));
        assertEquals(1, copy.size());
        final ScopePredicate predCopy = copy.iterator().next();
        assertEquals(ScopePredicate.DEFAULT_OID, predCopy.getOid());
        assertNull(predCopy.getPermission());
    }

    @Test
    public void getAnonymousNoOidsCopyOfScopeNull() {
        assertTrue(RbacUtilities.getAnonymousNoOidsCopyOfScope(null).isEmpty());
    }
}
