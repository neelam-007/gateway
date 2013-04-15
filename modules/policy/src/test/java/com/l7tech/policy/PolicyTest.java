package com.l7tech.policy;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyTest {
    private Policy p1;
    private Policy p2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        p1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false);
        p2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false);
    }

    @Test
    public void equalsDifferentSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(null);
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    @Test
    public void equalsSameSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(zone);
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(null);
        assertFalse(p1.hashCode() == p2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(zone);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
