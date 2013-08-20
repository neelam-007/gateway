package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class SecurityZonePredicateTest {
    private static final Permission READ_ANY_PERMISSION = new Permission(new Role(), OperationType.READ, EntityType.ANY);
    private SecurityZonePredicate predicate;

    @Test
    public void testToString() {
        final SecurityZone zone = new SecurityZone();
        zone.setName("Test");
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, zone);
        assertEquals("Objects in security zone Test", predicate.toString());
    }

    @Test
    public void testToStringNullRequiredZone() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        assertEquals("Objects not in any security zone", predicate.toString());
    }

    @Test
    public void testPartiallyZoneableEntity() {
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(EnumSet.of(EntityType.POLICY));
        zone.setName("Test");
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, zone);

        final Policy pze = new Policy(PolicyType.GLOBAL_FRAGMENT, "foo", null, false);
        pze.setSecurityZone(zone);
        assertFalse(predicate.matches(pze));

        pze.setType(PolicyType.INCLUDE_FRAGMENT);
        assertTrue(predicate.matches(pze));
    }

    @Test
    public void matchAnyEntityType() {
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, zone);

        final PublishedService service = new PublishedService();
        service.setSecurityZone(zone);
        assertTrue(predicate.matches(service));
    }

    @Test
    public void matchByEntityType() {
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, zone);

        final PublishedService correctEntityType = new PublishedService();
        correctEntityType.setSecurityZone(zone);
        assertTrue(predicate.matches(correctEntityType));
    }

    @Test
    public void nullEntity() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, new SecurityZone());
        assertFalse(predicate.matches(null));
    }

    @Test
    public void invalidEntityType() {
        final SecurityZone zone = new SecurityZone();
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, zone);

        final PublishedService wrongEntityType = new PublishedService();
        wrongEntityType.setSecurityZone(zone);
        assertFalse(predicate.matches(wrongEntityType));
    }

    @Test
    public void nullRequiredZoneNullEntity() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        assertFalse(predicate.matches(null));
    }

    @Test
    public void nullRequiredZoneMatch() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        final PublishedService withoutSecurityZone = new PublishedService();
        withoutSecurityZone.setSecurityZone(null);
        assertTrue(predicate.matches(withoutSecurityZone));
    }

    @Test
    public void nullRequiredZoneNoMatch() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        final PublishedService withSecurityZone = new PublishedService();
        withSecurityZone.setSecurityZone(new SecurityZone());
        assertFalse(predicate.matches(withSecurityZone));
    }

    @Test
    public void nullRequiredZoneNoMatchPartiallyZoneableEntityNotZoneable() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        // global fragments are not zoneable
        final Policy notZoneable = new Policy(PolicyType.GLOBAL_FRAGMENT, "foo", null, false);
        notZoneable.setSecurityZone(null);
        assertFalse(predicate.matches(notZoneable));
    }

    @Test
    public void nullRequiredZoneMatchPartiallyZoneableEntityNotZoneable() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        // includes are zoneable
        final Policy notZoneable = new Policy(PolicyType.INCLUDE_FRAGMENT, "foo", null, false);
        notZoneable.setSecurityZone(null);
        assertTrue(predicate.matches(notZoneable));
    }

    @Test
    public void nullRequiredZoneNoMatchNonZoneableEntity() {
        predicate = new SecurityZonePredicate(READ_ANY_PERMISSION, null);
        final SecurityZone notZoneable = new SecurityZone();
        assertFalse(predicate.matches(notZoneable));
    }
}
