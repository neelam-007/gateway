package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.RoleMatchingTestUtil;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

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

    @Before
    public void setup() {
        manager = new PolicyManagerImpl(roleManager, aliasManager, folderManager, policyCache);
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "xml", false);
    }

    @BugId("SSG-7101")
    @Test
    public void addManagePolicyRoleCanReadAllAssertions() throws Exception {
        manager.addManagePolicyRole(policy);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }
}
