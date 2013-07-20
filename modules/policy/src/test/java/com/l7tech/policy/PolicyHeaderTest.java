package com.l7tech.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyHeaderTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private PolicyHeader header;
    private Policy policy;
    private SecurityZone zone;

    @Before
    public void setup() {
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);
    }

    @Test
    public void constructorFromPolicySetsSecurityZone() {
        policy.setSecurityZone(zone);
        header = new PolicyHeader(policy, 0);
        assertEquals(ZONE_GOID, header.getSecurityZoneGoid());
    }

    @Test
    public void constructorFromPolicySetsNullSecurityZone() {
        policy.setSecurityZone(null);
        header = new PolicyHeader(policy, 0);
        assertNull(header.getSecurityZoneGoid());
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        header = new PolicyHeader(1234L, false, PolicyType.INCLUDE_FRAGMENT, "test", "test", "test", 1234L, 1234L, 0, 0, false, ZONE_GOID);
        final PolicyHeader copy = new PolicyHeader(header);
        assertEquals(ZONE_GOID, copy.getSecurityZoneGoid());
    }
}
