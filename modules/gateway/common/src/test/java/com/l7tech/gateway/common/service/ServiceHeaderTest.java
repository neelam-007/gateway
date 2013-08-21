package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceHeaderTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private static final Goid POLICY_GOID = new Goid(0,2222L);
    private PublishedService service;
    private ServiceHeader header;
    private SecurityZone zone;

    @Before
    public void setup() {
        service = new PublishedService();
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);
    }

    @Test
    public void constructorFromService() {
        final Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "test", "test", false);
        policy.setGoid(POLICY_GOID);
        service.setPolicy(policy);
        service.setSecurityZone(zone);
        header = new ServiceHeader(service);
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
        assertEquals(new Goid(0,2222L), header.getPolicyGoid());
    }

    @Test
    public void constructorFromServiceSetsNull() {
        service.setSecurityZone(null);
        service.setPolicy(null);
        header = new ServiceHeader(service);
        assertNull(header.getSecurityZoneId());
        assertNull(header.getPolicyGoid());
    }

    @Test
    public void copyConstructor() {
        header = new ServiceHeader(false, false, "test", new Goid(0,1234L), "test", "test", new Goid(0,1234L), new Goid(0,1234L), 0, 0, "test", false, false, ZONE_GOID, POLICY_GOID);
        final ServiceHeader copy = new ServiceHeader(header);
        assertEquals(ZONE_GOID, copy.getSecurityZoneId());
        assertEquals(POLICY_GOID, copy.getPolicyGoid());
    }
}
