package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceAliasManagerImplTest {
    private ServiceAliasManagerImpl manager;
    private static final Long ZONE_OID = 1234L;
    private SecurityZone zone;

    @Before
    public void setup() {
        manager = new ServiceAliasManagerImpl();
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

    private PublishedServiceAlias create(final SecurityZone zone) {
        final PublishedServiceAlias alias = new PublishedServiceAlias(new PublishedService(), null);
        alias.setSecurityZone(zone);
        return alias;
    }
}
