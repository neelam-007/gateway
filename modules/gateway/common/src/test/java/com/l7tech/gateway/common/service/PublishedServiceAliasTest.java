package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class PublishedServiceAliasTest {
    private SecurityZone zone;
    private PublishedService service;

    @Before
    public void setup() {
        zone = new SecurityZone();
        service = new PublishedService();
    }

    @BugId("SSG-7206")
    @Test
    public void securityZoneDoesNotPermitServiceAlias() {
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.SERVICE));
        service.setSecurityZone(zone);
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, null);
        assertNull(alias.getSecurityZone());
    }

    @Test
    public void securityZonePermitsServiceAlias() {
        final Set<EntityType> permitted = new HashSet<>();
        permitted.add(EntityType.SERVICE);
        permitted.add(EntityType.SERVICE_ALIAS);
        zone.setPermittedEntityTypes(permitted);
        service.setSecurityZone(zone);
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, null);
        assertEquals(zone, alias.getSecurityZone());
    }

    @Test
    public void securityZoneNull() {
        service.setSecurityZone(null);
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, null);
        assertNull(alias.getSecurityZone());
    }
}
