package com.l7tech.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class PolicyAliasTest {
    private SecurityZone zone;
    private Policy policy;

    @Before
    public void setup() {
        zone = new SecurityZone();
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    }

    @BugId("SSG-7206")
    @Test
    public void securityZoneDoesNotPermitPolicyAlias() {
        zone.setPermittedEntityTypes(Collections.singleton(EntityType.POLICY));
        policy.setSecurityZone(zone);
        final PolicyAlias alias = new PolicyAlias(policy, null);
        assertNull(alias.getSecurityZone());
    }

    @Test
    public void securityZonePermitsPolicyAlias() {
        final Set<EntityType> permitted = new HashSet<>();
        permitted.add(EntityType.POLICY);
        permitted.add(EntityType.POLICY_ALIAS);
        zone.setPermittedEntityTypes(permitted);
        policy.setSecurityZone(zone);
        final PolicyAlias alias = new PolicyAlias(policy, null);
        assertEquals(zone, alias.getSecurityZone());
    }

    @Test
    public void securityZoneNull() {
        policy.setSecurityZone(null);
        final PolicyAlias alias = new PolicyAlias(policy, null);
        assertNull(alias.getSecurityZone());
    }
}
