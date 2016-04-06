package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static com.l7tech.console.util.EncapsulatedAssertionConsoleUtil.NOT_CONFIGURED;
import static com.l7tech.console.util.EncapsulatedAssertionConsoleUtil.POLICY_UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Registry.class})
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

    @Test
    public void attachPolicies() throws Exception {
        Registry registry = PowerMockito.mock(Registry.class);
        PowerMockito.mockStatic(Registry.class);
        when(Registry.getDefault()).thenReturn(registry);
        PolicyAdmin policyAdmin = mock(PolicyAdmin.class);
        when(registry.getPolicyAdmin()).thenReturn(policyAdmin);

        // 3 encass configs
        Collection < EncapsulatedAssertionConfig > configs = new HashSet<>(3);
        EncapsulatedAssertionConfig config1 = new EncapsulatedAssertionConfig();
        config1.setGuid("1");
        Map<String, String> properties = new HashMap<>(1);
        properties.put(EncapsulatedAssertionConfig.PROP_POLICY_GUID, "1");
        config1.setProperties(properties);
        configs.add(config1);
        EncapsulatedAssertionConfig config2 = new EncapsulatedAssertionConfig();
        config2.setGuid("2");
        properties = new HashMap<>(1);
        properties.put(EncapsulatedAssertionConfig.PROP_POLICY_GUID, "2");
        config2.setProperties(properties);
        configs.add(config2);
        EncapsulatedAssertionConfig config3 = new EncapsulatedAssertionConfig();
        config3.setGuid("3");
        properties = new HashMap<>(1);
        properties.put(EncapsulatedAssertionConfig.PROP_POLICY_GUID, "3");
        config3.setProperties(properties);
        configs.add(config3);

        // test empty policy list, a possible result of no rbac access SSM-5212
        List<Policy> policies = new ArrayList<>(2);
        when(policyAdmin.findPoliciesByGuids(anyListOf(String.class))).thenReturn(policies);
        EncapsulatedAssertionConsoleUtil.attachPolicies(configs);

        // test encass config longer than policy list, a possible result of no rbac access
        Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, POLICY_NAME, "xml", false);
        policy1.setGuid("1");
        policies.add(policy1);
        EncapsulatedAssertionConsoleUtil.attachPolicies(configs);

        // test when next encass config doesn't match next policy list, a possible result of no rbac access
        Policy policy3 = new Policy(PolicyType.INCLUDE_FRAGMENT, POLICY_NAME, "xml", false);
        policy3.setGuid("3");
        policies.add(policy3);
        EncapsulatedAssertionConsoleUtil.attachPolicies(configs);

        // test when encass config match exactly policy list, no rbac restriction
        policies.remove(policy3);
        Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, POLICY_NAME, "xml", false);
        policy2.setGuid("2");
        policies.add(policy2);
        policies.add(policy3);
        EncapsulatedAssertionConsoleUtil.attachPolicies(configs);
    }
}
