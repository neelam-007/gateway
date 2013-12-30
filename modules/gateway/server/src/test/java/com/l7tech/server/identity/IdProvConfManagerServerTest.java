package com.l7tech.server.identity;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IdProvConfManagerServerTest {
    private IdProvConfManagerServer manager;
    @Mock
    private RoleManager roleManager;
    private IdentityProviderConfig config;
    private Properties properties;

    @Before
    public void setup() {
        properties = new Properties();
        manager = new IdProvConfManagerServer();
        manager.setRoleManager(roleManager);
        manager.setConfig(new MockConfig(properties));
        config = new IdentityProviderConfig();
    }

    @Test
    public void createRoles() throws Exception {
        manager.createRoles(config);
        verify(roleManager, times(1)).save(any(Role.class));
    }

    @Test
    public void createRolesSkipped() throws Exception {
        properties.setProperty(IdProvConfManagerServer.AUTO_CREATE_ROLE_PROPERTY, "false");
        manager.createRoles(config);
        verify(roleManager, never()).save(any(Role.class));
    }
}
