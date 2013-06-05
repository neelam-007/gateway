package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SsgActiveConnectorManagerImplTest {
    private SsgActiveConnectorManagerImpl manager;
    private static final Long ZONE_OID = 1234L;
    private SecurityZone zone;

    @Before
    public void setup() {
        manager = new SsgActiveConnectorManagerImpl();
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);
    }

    @Test
    public void newHeaderSetsSecurityZoneOid() {
        assertEquals(ZONE_OID, manager.newHeader(create(zone)).getSecurityZoneOid());
    }

    @Test
    public void newHeaderSetsNullSecurityZoneOid() {
        assertNull(manager.newHeader(create(null)).getSecurityZoneOid());
    }

    private SsgActiveConnector create(final SecurityZone zone) {
        final SsgActiveConnector connector = new SsgActiveConnector();
        connector.setSecurityZone(zone);
        return connector;
    }
}
