package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceEntryManagerImplTest {
    private ResourceEntryManagerImpl manager;
    private static final Long ZONE_OID = 1234L;
    private SecurityZone zone;

    @Before
    public void setup() {
        manager = new ResourceEntryManagerImpl();
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

    private ResourceEntry create(final SecurityZone zone) {
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setSecurityZone(zone);
        return resourceEntry;
    }
}
