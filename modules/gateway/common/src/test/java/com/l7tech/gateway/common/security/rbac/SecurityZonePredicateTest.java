package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.*;

public class SecurityZonePredicateTest {
    @Test
    public void testToString() {
        final SecurityZone zone = new SecurityZone();
        zone.setName("Test");
        final SecurityZonePredicate predicate = new SecurityZonePredicate(new Permission(new Role(), OperationType.READ, EntityType.ANY), zone);

        assertEquals("Objects in security zone Test", predicate.toString());
    }

    @Test
    public void testPartiallyZoneableEntity() {
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(EnumSet.of(EntityType.POLICY));
        zone.setName("Test");
        final SecurityZonePredicate predicate = new SecurityZonePredicate(new Permission(new Role(), OperationType.READ, EntityType.ANY), zone);

        final Policy pze = new Policy(PolicyType.GLOBAL_FRAGMENT, "foo", null, false);
        pze.setSecurityZone(zone);
        assertFalse(predicate.matches(pze));

        pze.setType(PolicyType.INCLUDE_FRAGMENT);
        assertTrue(predicate.matches(pze));
    }
}
