package com.l7tech.objectmodel.encass;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimplifiedPolicyTest {
    private SimplifiedPolicy simple;
    private Policy policy;

    @Before
    public void setup() {
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "testXML", false);
        policy.setGuid("abc123");
    }

    @Test
    public void defaultConstructor() {
        simple = new SimplifiedPolicy();
        assertNull(simple.getGuid());
        assertNull(simple.getName());
    }

    @Test
    public void policyConstructor() {
        simple = new SimplifiedPolicy(policy);
        assertEquals("abc123", simple.getGuid());
        assertEquals("test", simple.getName());
    }

    @Test
    public void policyConstructorNull() {
        simple = new SimplifiedPolicy(null);
        assertNull(simple.getGuid());
        assertNull(simple.getName());
    }
}
