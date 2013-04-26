package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SsgKeyMetadataTest {
    private SsgKeyMetadata key1;
    private SsgKeyMetadata key2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        key1 = new SsgKeyMetadata();
        key2 = new SsgKeyMetadata();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        key1.setSecurityZone(zone);
        key2.setSecurityZone(null);
        assertFalse(key1.equals(key2));
        assertFalse(key2.equals(key1));
    }

    @Test
    public void equalsSameSecurityZone() {
        key1.setSecurityZone(zone);
        key2.setSecurityZone(zone);
        assertTrue(key1.equals(key2));
        assertTrue(key2.equals(key1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        key1.setSecurityZone(zone);
        key2.setSecurityZone(null);
        assertFalse(key1.hashCode() == key2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        key1.setSecurityZone(zone);
        key2.setSecurityZone(zone);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void copyPayloadValues() {
        key1.setSecurityZone(zone);
        key2.setSecurityZone(null);
        SsgKeyMetadata.copyPayloadValues(key1, key2);
        assertEquals(zone, key2.getSecurityZone());
    }
}
