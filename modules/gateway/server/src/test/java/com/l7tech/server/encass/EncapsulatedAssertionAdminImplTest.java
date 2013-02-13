package com.l7tech.server.encass;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncapsulatedAssertionAdminImplTest {
    private static final Long POLICY_OID = 1234L;
    private static final Long CONFIG_OID = 12345L;
    private static final String CONFIG_GUID = "abc123";
    private static final String POLICY_GUID = "abc";
    private static final String NAME = "Test";
    private EncapsulatedAssertionAdminImpl admin;
    private EncapsulatedAssertionConfig config;
    private Collection<EncapsulatedAssertionConfig> configs;
    private Policy policy;
    @Mock
    private EncapsulatedAssertionConfigManager manager;

    @Before
    public void setup() {
        admin = new EncapsulatedAssertionAdminImpl();
        admin.setEncapsulatedAssertionConfigManager(manager);
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "policyXml", false);
        policy.setGuid(POLICY_GUID);
        config = new EncapsulatedAssertionConfig();
        config.setOid(CONFIG_OID);
        config.setName(NAME);
        config.setPolicy(policy);
        configs = new ArrayList<EncapsulatedAssertionConfig>();
        configs.add(config);
    }

    @Test
    public void findAllDetachesPolicies() throws Exception {
        when(manager.findAll()).thenReturn(configs);
        assertPoliciesDetached(admin.findAllEncapsulatedAssertionConfigs());
    }

    @Test
    public void findByPrimaryKeyDetachesPolicy() throws Exception {
        when(manager.findByPrimaryKey(CONFIG_OID)).thenReturn(config);
        assertPolicyDetached(admin.findByPrimaryKey(CONFIG_OID));
    }

    @Test
    public void findByGuidDetachesPolicy() throws Exception {
        when(manager.findByGuid(CONFIG_GUID)).thenReturn(config);
        assertPolicyDetached(admin.findByGuid(CONFIG_GUID));
    }

    @Test
    public void findByPolicyOidDetachesPolicies() throws Exception {
        when(manager.findByPolicyOid(POLICY_OID)).thenReturn(configs);
        assertPoliciesDetached(admin.findByPolicyOid(POLICY_OID));
    }

    @Test
    public void findByUniqueName() throws Exception {
        when(manager.findByUniqueName(NAME)).thenReturn(config);
        assertPolicyDetached(admin.findByUniqueName(NAME));
    }

    @Test
    public void findAllNull() throws Exception {
        when(manager.findAll()).thenReturn(null);
        assertTrue(admin.findAllEncapsulatedAssertionConfigs().isEmpty());
    }

    @Test(expected = FindException.class)
    public void findByPrimaryKeyNull() throws Exception {
        when(manager.findByPrimaryKey(anyLong())).thenReturn(null);
        admin.findByPrimaryKey(CONFIG_OID);
    }

    @Test(expected = FindException.class)
    public void findByGuidNull() throws Exception {
        when(manager.findByGuid(anyString())).thenReturn(null);
        admin.findByGuid(CONFIG_GUID);
    }

    @Test
    public void findByPolicyOidNull() throws Exception {
        when(manager.findByPolicyOid(anyLong())).thenReturn(null);
        assertTrue(admin.findByPolicyOid(POLICY_OID).isEmpty());
    }

    @Test
    public void findByUniqueNameNull() throws Exception {
        when(manager.findByUniqueName(anyString())).thenReturn(null);
        assertNull(admin.findByUniqueName(NAME));
    }

    private void assertPolicyDetached(final EncapsulatedAssertionConfig config) {
        assertNull(config.getPolicy());
        assertEquals(POLICY_GUID, config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
    }

    private void assertPoliciesDetached(final Collection<EncapsulatedAssertionConfig> configs) {
        for (final EncapsulatedAssertionConfig config : configs) {
            assertPolicyDetached(config);
        }
    }
}
