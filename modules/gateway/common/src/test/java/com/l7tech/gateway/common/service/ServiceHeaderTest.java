package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceHeaderTest {
    private static final Long ZONE_OID = 1234L;
    private PublishedService service;
    private ServiceHeader header;
    private SecurityZone zone;

    @Before
    public void setup() {
        service = new PublishedService();
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);
    }

    @Test
    public void constructorFromServiceSetsSecurityZone() {
        service.setSecurityZone(zone);
        header = new ServiceHeader(service);
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
    }

    @Test
    public void constructorFromServiceSetsNullSecurityZone() {
        service.setSecurityZone(null);
        header = new ServiceHeader(service);
        assertNull(header.getSecurityZoneOid());
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        header = new ServiceHeader(false, false, "test", 1234L, "test", "test", 1234L, 1234L, 0, 0, "test", false, false, ZONE_OID);
        final ServiceHeader copy = new ServiceHeader(header);
        assertEquals(ZONE_OID, copy.getSecurityZoneOid());
    }
}
