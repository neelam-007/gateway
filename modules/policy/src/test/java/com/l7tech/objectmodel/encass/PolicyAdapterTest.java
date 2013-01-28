package com.l7tech.objectmodel.encass;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyAdapterTest {
    private PolicyAdapter adapter;
    private Policy policy;
    private SimplifiedPolicy simple;

    @Before
    public void setup() {
        adapter = new PolicyAdapter();
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "policyName", "testXML", false);
        policy.setGuid("policyGuid");
        simple = new SimplifiedPolicy();
        simple.setGuid("simpleGuid");
        simple.setName("simpleName");
    }

    @Test
    public void marshal() throws Exception {
        final SimplifiedPolicy simple = adapter.marshal(policy);
        assertEquals("policyGuid", simple.getGuid());
        assertEquals("policyName", simple.getName());
    }

    @Test
    public void marshalNull() throws Exception {
        assertNull(adapter.marshal(null));
    }

    @Test
    public void unmarshal() throws Exception {
        final Policy policy = adapter.unmarshal(simple);
        assertEquals("simpleGuid", policy.getGuid());
        assertEquals("simpleName", policy.getName());
        assertNull(policy.getType());
        assertNull(policy.getXml());
        assertFalse(policy.isSoap());
    }

    @Test
    public void unmarshalNull() throws Exception {
        assertNull(adapter.unmarshal(null));
    }
}
