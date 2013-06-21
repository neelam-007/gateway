package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceHeaderTest {
    private static final Long ZONE_OID = 1234L;
    private static final Long POLICY_OID = 2222L;
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
    public void constructorFromService() {
        final Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "test", "test", false);
        policy.setOid(POLICY_OID);
        service.setPolicy(policy);
        service.setSecurityZone(zone);
        header = new ServiceHeader(service);
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(new Long(2222), header.getPolicyOid());
    }

    @Test
    public void constructorFromServiceSetsNull() {
        service.setSecurityZone(null);
        service.setPolicy(null);
        header = new ServiceHeader(service);
        assertNull(header.getSecurityZoneOid());
        assertNull(header.getPolicyOid());
    }

    @Test
    public void copyConstructor() {
        header = new ServiceHeader(false, false, "test", 1234L, "test", "test", 1234L, 1234L, 0, 0, "test", false, false, ZONE_OID, POLICY_OID);
        final ServiceHeader copy = new ServiceHeader(header);
        assertEquals(ZONE_OID, copy.getSecurityZoneOid());
        assertEquals(POLICY_OID, copy.getPolicyOid());
    }
}
