package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Test;

import static org.junit.Assert.*;

public class SecurityZonePredicateTest {
    @Test
    public void testToString() {
        final SecurityZone zone = new SecurityZone();
        zone.setName("Test");
        final SecurityZonePredicate predicate = new SecurityZonePredicate(new Permission(new Role(), OperationType.READ, EntityType.ANY), zone);

        assertEquals("Objects in security zone Test", predicate.toString());
    }
}
