package com.l7tech.server.log;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterContextFactory;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.log.syslog.SyslogManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SinkManagerImplMockTest {
    private ServerConfig serverConfig;
    @Mock
    private SyslogManager syslogManager;
    @Mock
    private TrafficLogger trafficLogger;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private ClusterInfoManager clusterInfoManager;
    @Mock
    private ClusterContextFactory clusterContextFactory;
    @Mock
    private RoleManager roleManager;
    private SinkConfiguration config;
    private Properties properties;

    @Before
    public void setup() {
        properties = new Properties();
        serverConfig = new StubServerConfig(properties, 1000L);
        config = new SinkConfiguration();
    }

    @Test
    public void createRoles() throws Exception {
        createManager(properties).createRoles(config);
        verify(roleManager, times(1)).save(any(Role.class));
    }

    @Test
    public void createRolesSkipped() throws Exception {
        properties.setProperty(SinkManagerImpl.AUTO_CREATE_ROLE_PROPERTY, "false");
        createManager(properties).createRoles(config);
        verify(roleManager, never()).save(any(Role.class));
    }

    private SinkManagerImpl createManager(final Properties configProperties) {
        return new SinkManagerImpl(new StubServerConfig(configProperties, 1000L), syslogManager, trafficLogger, applicationEventProxy,
                clusterInfoManager, clusterContextFactory, roleManager);
    }

    private class StubServerConfig extends ServerConfig {
        protected StubServerConfig(final Properties properties, final long cacheAge) {
            super(properties, cacheAge);
        }
    }
}
