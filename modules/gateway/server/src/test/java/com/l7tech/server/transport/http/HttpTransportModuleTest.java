package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpTransportModuleTest {
    private HttpTransportModule module;
    private ServerConfig serverConfig;
    @Mock
    private MasterPasswordManager masterPasswordManager;
    @Mock
    private DefaultKey defaultKey;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private SsgConnectorManager connectorManager;
    @Mock
    private TrustedCertServices trustedCertServices;

    @Before
    public void setup() {
        serverConfig = new ServerConfigStub();
        module = new TestableHttpTransportModule(serverConfig,
                masterPasswordManager,
                defaultKey,
                licenseManager,
                connectorManager,
                trustedCertServices,
                Collections.<SsgConnectorActivationListener>emptySet());
    }

    @After
    public void teardown() {
        SyspropUtil.clearProperty(HttpTransportModule.APACHE_ALLOW_BACKSLASH);
    }

    @Test
    public void readyForMessagesDisallowBackslashByDefault() throws LifecycleException {
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertFalse(SyspropUtil.getBoolean(HttpTransportModule.APACHE_ALLOW_BACKSLASH));
    }

    @Test
    public void readyForMessagesDisallowBackslash() throws LifecycleException {
        serverConfig.putProperty(ServerConfigParams.PARAM_IO_HTTP_ALLOW_BACKSLASH, "false");
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertFalse(SyspropUtil.getBoolean(HttpTransportModule.APACHE_ALLOW_BACKSLASH));
    }

    @Test
    public void readyForMessagesAllowBackslash() throws LifecycleException {
        serverConfig.putProperty(ServerConfigParams.PARAM_IO_HTTP_ALLOW_BACKSLASH, "true");
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertTrue(SyspropUtil.getBoolean(HttpTransportModule.APACHE_ALLOW_BACKSLASH));
    }

    private class TestableHttpTransportModule extends HttpTransportModule {
        public TestableHttpTransportModule(final ServerConfig serverConfig,
                                           final MasterPasswordManager masterPasswordManager,
                                           final DefaultKey defaultKey,
                                           final LicenseManager licenseManager,
                                           final SsgConnectorManager ssgConnectorManager,
                                           final TrustedCertServices trustedCertServices,
                                           final Set<SsgConnectorActivationListener> endpointListeners) {
            super(serverConfig, masterPasswordManager, defaultKey, licenseManager, ssgConnectorManager, trustedCertServices, endpointListeners);
        }

        @Override
        public boolean isStarted() {
            return true;
        }
    }
}
