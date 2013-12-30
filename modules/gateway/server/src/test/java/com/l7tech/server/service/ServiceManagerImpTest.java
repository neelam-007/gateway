package com.l7tech.server.service;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.RoleMatchingTestUtil;
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
public class ServiceManagerImpTest {
    private ServiceManager manager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private ServiceAliasManager aliasManager;
    private PublishedService service;
    private Properties properties;

    @Before
    public void setup() {
        properties = new Properties();
        manager = new ServiceManagerImp(roleManager, aliasManager, new MockConfig(properties));
        service = new PublishedService();
    }

    @BugId("SSG-7101")
    @Test
    public void addManageServiceRoleCanReadAllAssertions() throws Exception {
        manager.addManageServiceRole(service);
        verify(roleManager).save(argThat(RoleMatchingTestUtil.canReadAllAssertions()));
    }

    @Test
    public void createRoles() throws Exception {
        manager.createRoles(service);
        verify(roleManager, times(1)).save(any(Role.class));
    }

    @Test
    public void createRolesSkipped() throws Exception {
        properties.setProperty(ServiceManagerImp.AUTO_CREATE_ROLE_PROPERTY, "false");
        manager.createRoles(service);
        verify(roleManager, never()).save(any(Role.class));
    }
}
