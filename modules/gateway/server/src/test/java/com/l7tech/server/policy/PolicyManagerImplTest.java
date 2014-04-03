package com.l7tech.server.policy;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.RoleMatchingTestUtil;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.test.BugId;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PolicyManagerImplTest {
    private PolicyManager manager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private PolicyAliasManager aliasManager;
    @Mock
    private FolderManager folderManager;
    @Mock
    private PolicyCache policyCache;
    private Policy policy;
    private Properties properties;

    @Before
    public void setup() {
        properties = new Properties();
        manager = new PolicyManagerImpl(roleManager, aliasManager, folderManager, policyCache, new MockConfig(properties));
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false);
    }

    @BugId("SSG-7101")
    @Test
    public void addManagePolicyRoleCanReadAllAssertions() throws Exception {
        manager.addManagePolicyRole(policy);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }

    @Test
    public void createRoles() throws Exception {
        manager.createRoles(policy);
        verify(roleManager).save(any(Role.class));
    }

    @Test
    public void createRolesSkipped() throws Exception {
        properties.setProperty(PolicyManagerImpl.AUTO_CREATE_ROLE_PROPERTY, "false");
        manager.createRoles(policy);
        verify(roleManager, never()).save(any(Role.class));
    }

    @Test
    public void addManagePolicyRoleCanDebugGlobalPolicyFragment() throws Exception {
        Policy globalPolicy = new Policy(PolicyType.GLOBAL_FRAGMENT, "test", "xml", false);
        manager.addManagePolicyRole(globalPolicy);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canDebugPolicy(globalPolicy.getGoid())));
    }

    @Test
    public void addManagePolicyRoleCannotDebugIncludedPolicyFragment() throws Exception {
        Policy includedPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false);
        manager.addManagePolicyRole(includedPolicy);
        verify(roleManager, never()).save(argThat(RoleMatchingTestUtil.canDebugPolicy(includedPolicy.getGoid())));
    }
}
