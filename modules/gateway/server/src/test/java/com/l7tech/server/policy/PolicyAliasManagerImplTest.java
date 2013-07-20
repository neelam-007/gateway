package com.l7tech.server.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyAliasManagerImplTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private PolicyAliasManagerImpl manager;
    private SecurityZone zone;

    @Before
    public void setup() {
        manager = new PolicyAliasManagerImpl();
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);
    }

    @Test
    public void newHeaderSetsSecurityZoneOid() {
        assertEquals(ZONE_GOID, manager.newHeader(createAlias(zone)).getSecurityZoneGoid());
    }

    @Test
    public void newHeaderSetsNullSecurityZoneOid() {
        assertNull(manager.newHeader(createAlias(null)).getSecurityZoneGoid());
    }

    private PolicyAlias createAlias(final SecurityZone zone) {
        return new PolicyAlias(new PolicyHeader(new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false)), null, zone);
    }
}
