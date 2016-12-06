package com.l7tech.server.transport;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.SyspropUtil;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Logger;

@Ignore("works localy but fails in TeamCity as the JceProvider is being initialized before our test, hence JceProvider.ENGINE_PROPERTY has no effect when our test is executed")
@RunWith(MockitoJUnitRunner.class)
public class TransportModuleTest {

    @Mock
    SsgConnectorManager ssgConnectorManager;

    @Mock
    TrustedCertServices trustedCertServices;

    @Mock
    DefaultKey defaultKey;
    @Mock
    SsgKeyEntry ssgKeyEntry;

    @Mock
    Config config;

    private TransportModule transportModule;

    @Mock X509Certificate defaultCertificate;

    private static final TrustedCert certificate1 = new TrustedCert();
    private static final TrustedCert certificate2 = new TrustedCert();

    @BeforeClass
    public static void beforeClass(){
        // set the default provider to our own class so that we can control the compatibility flag (JceProvider#getCompatibilityFlag(...))
        SyspropUtil.setProperty(JceProvider.ENGINE_PROPERTY, "com.l7tech.server.transport.JceProviderForTesting");

        certificate1.setGoid(new Goid(1,1));
        certificate1.setName("Cert1");
        certificate2.setGoid(new Goid(1,2));
        certificate2.setName("Cert2");
    }

    @Before
    public void before() throws IOException {
        JceProviderForTesting.compatibilityFlag = null;
        transportModule = new TransportModule(
                "test",
                Component.GATEWAY,
                Logger.getLogger(TransportModuleTest.class.getName()),
                "my feature",
                null,
                ssgConnectorManager,
                trustedCertServices,
                defaultKey,
                config
        ) {
            @Override
            protected void addConnector(SsgConnector connector) throws ListenerException {

            }

            @Override
            protected void removeConnector(Goid goid) {

            }

            @Override
            protected Set<String> getSupportedSchemes() {
                return null;
            }

            @Override
            public void reportMisconfiguredConnector(Goid connectorGoid) {

            }
        };

        Mockito.when(defaultKey.getSslInfo()).thenReturn(ssgKeyEntry);
        Mockito.when(ssgKeyEntry.getCertificate()).thenReturn(defaultCertificate);
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS10_NoIssuers() throws Exception {
        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, "TLSv1.0");

        // by default the for TLS10 should NOT return issuers list (unless the TLS10 provider is SSL-j)
        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //reset i.e. remove includeSelfCertIfAcceptedIssuersEmpty
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        // set compatibility mode to TRUE (simulating SSL-J as TLS10 provider)
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(1, acceptedIssuers.length);
        Assert.assertEquals("SSL-J cannot send an empty list so the default cert should be returned", defaultCertificate, acceptedIssuers[0]);
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS10_WithIssuers() throws Exception {
        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, "TLSv1.0");

        // by default the for TLS10 should NOT return issuers list (unless the TLS10 provider is SSL-j)
        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //reset i.e. remove includeSelfCertIfAcceptedIssuersEmpty
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        // set compatibility mode to TRUE (simulating SSL-J as TLS10 provider)
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("SSL-J cannot send an empty list so issuers list is expected to be returned", 2, acceptedIssuers.length);

        // reset noAcceptedIssuers, set acceptedIssuers to false and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "false");
        JceProviderForTesting.compatibilityFlag = null;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("acceptedIssuers");
        myConnector.putProperty("noAcceptedIssuers", "true");
        JceProviderForTesting.compatibilityFlag = null;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: For SSL-J should the accepted issuers be empty when acceptedIssuers=false and noAcceptedIssuers=true
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // reset noAcceptedIssuers, set acceptedIssuers to false and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "false");
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("acceptedIssuers");
        myConnector.putProperty("noAcceptedIssuers", "true");
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS11And12_NoIssuers() throws Exception {
        doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport("TLSv1.1");
        doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport("TLSv1.2");
    }

    private void doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport(final String transport) throws Exception {
        Assert.assertThat(transport, Matchers.not(Matchers.isEmptyOrNullString()));

        // reset compatibility flag
        JceProviderForTesting.compatibilityFlag = null;

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("empty issuers list is expected", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("default cert should be returned", 1, acceptedIssuers.length);

        //reset includeSelfCertIfAcceptedIssuersEmpty
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        // set compatibility mode to TRUE (simulating SSL-J as TLS11 and TLS12 provider)
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("SSL-J cannot send an empty list so the default cert should be returned", 1, acceptedIssuers.length);
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS12_WithIssuers() throws Exception {
        doTestGetAcceptedIssuersForConnectorWithIssuersForTransport("TLSv1.1");
        doTestGetAcceptedIssuersForConnectorWithIssuersForTransport("TLSv1.2");
    }

    private void doTestGetAcceptedIssuersForConnectorWithIssuersForTransport(final String transport) throws Exception {
        Assert.assertThat(transport, Matchers.not(Matchers.isEmptyOrNullString()));

        // reset compatibility flag
        JceProviderForTesting.compatibilityFlag = null;

        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(2, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("includeSelfCertIfAcceptedIssuersEmpty has no impact if issuers list is not empty", 2, acceptedIssuers.length);

        //reset includeSelfCertIfAcceptedIssuersEmpty
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        // set compatibility mode to TRUE (simulating SSL-J as TLS11 and TLS12 provider)
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(2, acceptedIssuers.length);

        // reset noAcceptedIssuers, set acceptedIssuers to false and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "false");
        JceProviderForTesting.compatibilityFlag = null;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("acceptedIssuers");
        myConnector.putProperty("noAcceptedIssuers", "true");
        JceProviderForTesting.compatibilityFlag = null;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: For SSL-J should the accepted issuers be empty when acceptedIssuers=false and noAcceptedIssuers=true
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // reset noAcceptedIssuers, set acceptedIssuers to false and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "false");
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true and reset compatibility flag
        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("acceptedIssuers");
        myConnector.putProperty("noAcceptedIssuers", "true");
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void getAcceptedIssuersForConnector_ClientAuthNever() throws Exception {
        doTestGetAcceptedIssuersForConnectorWithClientAuthNever("TLSv1.0");
        doTestGetAcceptedIssuersForConnectorWithClientAuthNever("TLSv1.1");
        doTestGetAcceptedIssuersForConnectorWithClientAuthNever("TLSv1.2");
    }

    private void doTestGetAcceptedIssuersForConnectorWithClientAuthNever(final String transport) throws Exception {
        Assert.assertThat(transport, Matchers.not(Matchers.isEmptyOrNullString()));

        // reset compatibility flag
        JceProviderForTesting.compatibilityFlag = null;

        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        myConnector.removeProperty("includeSelfCertIfAcceptedIssuersEmpty");
        myConnector.removeProperty("noAcceptedIssuers");
        myConnector.putProperty("acceptedIssuers", "true");
        JceProviderForTesting.compatibilityFlag = Boolean.TRUE;
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);
    }
}