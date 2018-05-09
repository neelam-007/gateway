package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;
import org.apache.catalina.connector.Connector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.l7tech.server.transport.http.HttpTransportModule.*;
import static com.l7tech.util.BuildInfo.getProductVersionMajor;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BuildInfo.class)
public class HttpTransportModuleTest {

    private static final String CONNECTOR_SERVER_HEADER = "server_header_connector";
    private static final String SYSPROP_SERVER_HEADER = "server_header_sysprop";
    private static final String TEST_VERSION_9_3 = "9.3.00";
    private static final String TEST_VERSION_9_4 = "9.4.00";
    private static final String TEST_VERSION_10 = "10.0.00";
    private static final String DEFAULT_SERVER_HEADER_9 = format(HEADER_SERVER_DEFAULT_FORMAT, "9");
    private static final String DEFAULT_SERVER_HEADER_10 = format(HEADER_SERVER_DEFAULT_FORMAT, "10");
    private static final String DEFAULT_SERVER_HEADER_CURRENT = format(HEADER_SERVER_DEFAULT_FORMAT, getProductVersionMajor());

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
                trustedCertServices);

    }

    @After
    public void teardown() {
        SyspropUtil.clearProperty(APACHE_ALLOW_BACKSLASH);
        SyspropUtil.clearProperty(HEADER_SERVER_CONFIG_PROP_DEFAULT);
    }

    @Test
    public void readyForMessagesDisallowBackslashByDefault() {
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertFalse(SyspropUtil.getBoolean(APACHE_ALLOW_BACKSLASH));
    }

    @Test
    public void readyForMessagesDisallowBackslash() {
        serverConfig.putProperty(ServerConfigParams.PARAM_IO_HTTP_ALLOW_BACKSLASH, "false");
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertFalse(SyspropUtil.getBoolean(APACHE_ALLOW_BACKSLASH));
    }

    @Test
    public void readyForMessagesAllowBackslash() {
        serverConfig.putProperty(ServerConfigParams.PARAM_IO_HTTP_ALLOW_BACKSLASH, "true");
        module.onApplicationEvent(new ReadyForMessages(this, null, null));
        assertTrue(SyspropUtil.getBoolean(APACHE_ALLOW_BACKSLASH));
    }

    @Test
    public void serverPropertyUseFromConnectorAttributes() throws Exception {
        Map<String, Object> attributes = Stream.of(new SimpleEntry<>(PROP_NAME_SERVER, CONNECTOR_SERVER_HEADER)).collect(toMap(Entry::getKey, Entry::getValue));
        Connector connector = new Connector();
        HttpTransportModule.setConnectorAttributes(connector, attributes);

        assertEquals(CONNECTOR_SERVER_HEADER, connector.getProperty(PROP_NAME_SERVER));
    }

    @Test
    public void serverPropertyUseFromSysprop() throws Exception {
        SyspropUtil.setProperty(HEADER_SERVER_CONFIG_PROP_DEFAULT, SYSPROP_SERVER_HEADER);

        Connector connector = new Connector();
        HttpTransportModule.setConnectorAttributes(connector, new HashMap<>());

        assertEquals(SYSPROP_SERVER_HEADER, connector.getProperty(PROP_NAME_SERVER));
    }

    @Test
    public void serverPropertyUseDefault_9_3() throws Exception {
        testServerPropertyDefaultWithVersion(TEST_VERSION_9_3, DEFAULT_SERVER_HEADER_9);
    }

    @Test
    public void serverPropertyUseDefault_9_4() throws Exception {
        testServerPropertyDefaultWithVersion(TEST_VERSION_9_4, DEFAULT_SERVER_HEADER_9);
    }

    @Test
    public void serverPropertyUseDefault_10() throws Exception {
        testServerPropertyDefaultWithVersion(TEST_VERSION_10, DEFAULT_SERVER_HEADER_10);
    }

    @Test
    public void serverPropertyUseDefault_CurrentVersion() throws Exception {
        testServerPropertyDefaultWithVersion(null, DEFAULT_SERVER_HEADER_CURRENT);
    }

    private static void testServerPropertyDefaultWithVersion(String version, String expected) throws Exception {
        mockProductVersion(version);

        Connector connector = new Connector();
        HttpTransportModule.setConnectorAttributes(connector, new HashMap<>());

        assertEquals(expected, connector.getProperty(PROP_NAME_SERVER));
    }

    private static void mockProductVersion(String version) throws Exception {
        if (version == null) {
            return;
        }

        PowerMockito.spy(BuildInfo.class);
        PowerMockito.doReturn(version).when(BuildInfo.class, "getPackageImplementationVersion");
    }

    private class TestableHttpTransportModule extends HttpTransportModule {
        TestableHttpTransportModule(final ServerConfig serverConfig,
                                    final MasterPasswordManager masterPasswordManager,
                                    final DefaultKey defaultKey,
                                    final LicenseManager licenseManager,
                                    final SsgConnectorManager ssgConnectorManager,
                                    final TrustedCertServices trustedCertServices) {
            super( serverConfig, masterPasswordManager, defaultKey, licenseManager, ssgConnectorManager, trustedCertServices );
        }

        @Override
        public boolean isStarted() {
            return true;
        }
    }

}
