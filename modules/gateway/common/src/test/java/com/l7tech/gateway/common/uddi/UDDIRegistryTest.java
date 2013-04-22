package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UDDIRegistryTest {
    private UDDIRegistry registry;

    @Before
    public void setup() {
        registry = new UDDIRegistry();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        registry.setSecurityZone(zone);
        final UDDIRegistry copy = new UDDIRegistry();
        copy.copyFrom(registry);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        registry.setSecurityZone(null);
        final UDDIRegistry copy = new UDDIRegistry();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(registry);
        assertNull(copy.getSecurityZone());
    }
}
