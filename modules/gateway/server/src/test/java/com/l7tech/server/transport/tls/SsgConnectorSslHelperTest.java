package com.l7tech.server.transport.tls;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.http.DefaultHttpCiphers;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.test.BugId;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Add additional SsgConnectorSslHelper tests here
 */
@RunWith(MockitoJUnitRunner.class)
public class SsgConnectorSslHelperTest {

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

    private HttpTransportModule module;
    private SsgConnector connector;
    private final String recommendedCiphers = DefaultHttpCiphers.getRecommendedCiphers();

    @Before
    public void setup() throws Exception {
        final ServerConfig serverConfig = new ServerConfigStub();
        module = Mockito.spy(new HttpTransportModule(
                serverConfig,
                masterPasswordManager,
                defaultKey,
                licenseManager,
                connectorManager,
                trustedCertServices,
                Collections.emptySet()
        ));

        final Pair<X509Certificate, PrivateKey> keys = new TestCertificateGenerator().basicConstraintsCa(1).subject("cn=test1").keySize(1024).generateWithKey();
        final SsgKeyEntry keyEntry = new SsgKeyEntry(Goid.DEFAULT_GOID, "ALIAS", new X509Certificate[]{keys.left}, keys.right);

        connector = new SsgConnector(Goid.DEFAULT_GOID, "test connector1", 1111, SsgConnector.SCHEME_HTTPS, true, SsgConnector.Endpoint.MESSAGE_INPUT.name(), 0, null, null);
        connector.setEnabled(true);
        connector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, "TLSv1,TLSv1.1,TLSv1.2");

        Mockito.doReturn(keyEntry).when(module).getKeyEntry(connector);
    }

    @After
    public void teardown() {
    }

    @Test
    @BugId("DE270482")
    public void testForUnsupportedCiphers() throws Exception {
        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, recommendedCiphers + ",UNSUPPORTED_CIPHER_TEST1");
        SsgConnectorSslHelper helper = new SsgConnectorSslHelper(module, connector);
        Assert.assertThat(helper.getEnabledCiphers(), Matchers.arrayContaining(recommendedCiphers.split(",")));

        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, "UNSUPPORTED_CIPHER_TEST1, " + recommendedCiphers);
        helper = new SsgConnectorSslHelper(module, connector);
        Assert.assertThat(helper.getEnabledCiphers(), Matchers.arrayContaining(recommendedCiphers.split(",")));

        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, "UNSUPPORTED_CIPHER_TEST1, " + recommendedCiphers + ",UNSUPPORTED_CIPHER_TEST2");
        helper = new SsgConnectorSslHelper(module, connector);
        Assert.assertThat(helper.getEnabledCiphers(), Matchers.arrayContaining(recommendedCiphers.split(",")));

        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, "UNSUPPORTED_CIPHER_TEST1, UNSUPPORTED_CIPHER_TEST2");
        try {
            new SsgConnectorSslHelper(module, connector);
            Assert.fail("should have failed with ListenerException");
        } catch (final ListenerException ex) {
            Assert.assertThat(ExceptionUtils.getMessage(ex), Matchers.equalTo("Unable to open listen port with the specified SSL configuration: None of the selected cipher suites are supported by the underlying TLS provider"));
        }
    }

    @Test
    public void testForSupportedCiphers() throws Exception {
        // this will also give an early warning if some of our recommended ciphers become unsupported by the underlying TLS provider
        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, recommendedCiphers);
        SsgConnectorSslHelper helper = new SsgConnectorSslHelper(module, connector);
        Assert.assertThat(helper.getEnabledCiphers(), Matchers.arrayContaining(recommendedCiphers.split(",")));
    }

}