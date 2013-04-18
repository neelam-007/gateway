package com.l7tech.gateway.common.security;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RevocationCheckPolicyTest {
    private RevocationCheckPolicy p1;
    private RevocationCheckPolicy p2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        p1 = new RevocationCheckPolicy();
        p2 = new RevocationCheckPolicy();
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
