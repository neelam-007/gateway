package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JmsEndpointTest {
    private JmsEndpoint endpoint;
    private SecurityZone zone;

    @Before
    public void setup() {
        endpoint = new JmsEndpoint();
        zone = new SecurityZone();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        endpoint.setSecurityZone(zone);
        final JmsEndpoint copy = new JmsEndpoint();
        copy.copyFrom(endpoint);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        endpoint.setSecurityZone(null);
        final JmsEndpoint copy = new JmsEndpoint();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(endpoint);
        assertNull(copy.getSecurityZone());
    }

    @Test
    public void copyConstructor() {
        endpoint.setSecurityZone(zone);
        final JmsEndpoint copy = new JmsEndpoint(endpoint, false);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void testJmsEndpointMessagePropertyRule() throws Exception {
        JmsEndpointMessagePropertyRule jmsPropRule = new JmsEndpointMessagePropertyRule();
        jmsPropRule.setRuleName("name");
        jmsPropRule.setPassThru(false);
        jmsPropRule.setCustomPattern("value");
        Set<JmsEndpointMessagePropertyRule> ruleSet = new HashSet<>();
        ruleSet.add(jmsPropRule);
        endpoint.setJmsEndpointMessagePropertyRules(ruleSet);
        assertTrue(endpoint.getJmsEndpointMessagePropertyRules().contains(jmsPropRule));
    }
}
