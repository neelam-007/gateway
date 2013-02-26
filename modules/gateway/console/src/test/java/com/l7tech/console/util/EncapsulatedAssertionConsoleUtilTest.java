package com.l7tech.console.util;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.console.util.EncapsulatedAssertionConsoleUtil.NOT_CONFIGURED;
import static com.l7tech.console.util.EncapsulatedAssertionConsoleUtil.POLICY_UNAVAILABLE;
import static org.junit.Assert.*;

public class EncapsulatedAssertionConsoleUtilTest {
    private static final String POLICY_NAME = "TestPolicy";
    private Policy policy;
    private EncapsulatedAssertionConfig config;

    @Before
    public void setup() {
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, POLICY_NAME, "xml", false);
        config = new EncapsulatedAssertionConfig();
    }

    @Test
    public void getPolicyDisplayName() {
        assertEquals(POLICY_NAME, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(policy, config));
    }

    @Test
    public void getPolicyDisplayNameNullPolicyNewConfig() {
        config.setGuid(null);
        assertEquals(NOT_CONFIGURED, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(null, config));
    }

    @Test
    public void gePolicyDisplayNameNullPolicyExistingConfig() {
        config.setGuid("abc123");
        assertEquals(POLICY_UNAVAILABLE, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(null, config));
    }

    @Test
    public void getPolicyDisplayNameForBackingPolicyOnConfig() {
        config.setPolicy(policy);
        assertEquals(POLICY_NAME, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(config));
    }

    @Test
    public void getPolicyDisplayNameForBackingPolicyOnConfigNullPolicyNewConfig() {
        config.setPolicy(null);
        config.setGuid(null);
        assertEquals(NOT_CONFIGURED, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(config));
    }

    @Test
    public void getPolicyDisplayNameForBackingPolicyOnConfigNullPolicyExistingConfig() {
        config.setPolicy(null);
        config.setGuid("abc123");
        assertEquals(POLICY_UNAVAILABLE, EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(config));
    }
}
