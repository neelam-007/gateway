package com.l7tech.server.policy;

import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyAliasManagerImplTest {
    private static final Long ZONE_OID = 1234L;
    private PolicyAliasManagerImpl manager;
    private SecurityZone zone;

    @Before
    public void setup() {
        manager = new PolicyAliasManagerImpl();
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);
    }

    @Test
    public void newHeaderSetsSecurityZoneOid() {
        assertEquals(ZONE_OID, manager.newHeader(createAlias(zone)).getSecurityZoneOid());
    }

    @Test
    public void newHeaderSetsNullSecurityZoneOid() {
        assertNull(manager.newHeader(createAlias(null)).getSecurityZoneOid());
    }

    private PolicyAlias createAlias(final SecurityZone zone) {
        return new PolicyAlias(new PolicyHeader(new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false)), null, zone);
    }
}
