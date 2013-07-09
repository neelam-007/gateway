package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.RoleMatchingTestUtil;
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
public class ServiceManagerImpTest {
    private ServiceManager manager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private ServiceAliasManager aliasManager;
    private PublishedService service;

    @Before
    public void setup() {
        manager = new ServiceManagerImp(roleManager, aliasManager);
        service = new PublishedService();
    }

    @BugId("SSG-7101")
    @Test
    public void addManageServiceRoleCanReadAllAssertions() throws Exception {
        manager.addManageServiceRole(service);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }
}
