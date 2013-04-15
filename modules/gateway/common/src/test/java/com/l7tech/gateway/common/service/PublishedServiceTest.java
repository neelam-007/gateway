package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PublishedServiceTest {
    private PublishedService s1;
    private PublishedService s2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        s1 = new PublishedService();
        s2 = new PublishedService();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.equals(s2));
        assertFalse(s2.equals(s1));
    }

    @Test
    public void equalsSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.hashCode() == s2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
