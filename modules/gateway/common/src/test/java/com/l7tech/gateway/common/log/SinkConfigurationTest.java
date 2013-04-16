package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SinkConfigurationTest {
    private SinkConfiguration s1;
    private SinkConfiguration s2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        s1 = new SinkConfiguration();
        s2 = new SinkConfiguration();
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

    @Test
    public void copyFromSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        s1.setSecurityZone(zone);
        final SinkConfiguration copy = new SinkConfiguration();
        copy.copyFrom(s1);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        s1.setSecurityZone(null);
        final SinkConfiguration copy = new SinkConfiguration();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(s1);
        assertNull(copy.getSecurityZone());
    }
}
