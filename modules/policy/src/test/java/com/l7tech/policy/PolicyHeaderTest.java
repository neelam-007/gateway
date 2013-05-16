package com.l7tech.policy;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyHeaderTest {
    private static final Long ZONE_OID = 1234L;
    private PolicyHeader header;
    private Policy policy;
    private SecurityZone zone;

    @Before
    public void setup() {
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);
    }

    @Test
    public void constructorFromPolicySetsSecurityZone() {
        policy.setSecurityZone(zone);
        header = new PolicyHeader(policy, 0);
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
    }

    @Test
    public void constructorFromPolicySetsNullSecurityZone() {
        policy.setSecurityZone(null);
        header = new PolicyHeader(policy, 0);
        assertNull(header.getSecurityZoneOid());
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        header = new PolicyHeader(1234L, false, PolicyType.INCLUDE_FRAGMENT, "test", "test", "test", 1234L, 1234L, 0, 0, false, ZONE_OID);
        final PolicyHeader copy = new PolicyHeader(header);
        assertEquals(ZONE_OID, copy.getSecurityZoneOid());
    }
}
