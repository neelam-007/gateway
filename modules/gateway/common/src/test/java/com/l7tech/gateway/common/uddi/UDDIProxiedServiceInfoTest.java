package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UDDIProxiedServiceInfoTest {
    private UDDIProxiedServiceInfo info;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        info = new UDDIProxiedServiceInfo();
    }

    @Test
    public void copyConfigModifiablePropertiesCopiesSecurityZone() {
        info.setSecurityZone(zone);
        final UDDIProxiedServiceInfo copy = new UDDIProxiedServiceInfo();
        copy.copyConfigModifiableProperties(info);
        assertEquals(zone, copy.getSecurityZone());
    }
}
