package com.l7tech.server.transport;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
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

//@Ignore("works locally but fails in TeamCity as the JceProvider is being initialized before our test, hence JceProvider.ENGINE_PROPERTY has no effect when our test is executed")
@RunWith(MockitoJUnitRunner.class)
public class TransportModuleTest {

    private static final String ACCEPTED_ISSUERS = "acceptedIssuers";
    private static final String INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY = "includeSelfCertIfAcceptedIssuersEmpty";
    private static final String NO_ACCEPTED_ISSUERS = "noAcceptedIssuers";

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
        certificate1.setGoid(new Goid(1,1));
        certificate1.setName("Cert1");
        certificate2.setGoid(new Goid(1,2));
        certificate2.setName("Cert2");
    }

    @Before
    public void before() throws IOException {
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

        // by default the for TLS10 should NOT return issuers list
        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        // reset noAcceptedIssuers, set acceptedIssuers to false
        myConnector.removeProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY);
        myConnector.putProperty(ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("Gateway does not include an accepted issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true, and accepted Issuers to true
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        myConnector.putProperty(ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("Should send back a default key when there are no issuers", 1, acceptedIssuers.length);
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS10_WithIssuers() throws Exception {
        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, "TLSv1.0");

        // by default the for TLS10 should NOT return issuers list
        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("TLS10 default behaviour is always to send empty issuers list", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true, and accepted Issuers to true
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        myConnector.putProperty(ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("Should send back a list of 2 accepted issuers", 2, acceptedIssuers.length);

        // reset noAcceptedIssuers, set acceptedIssuers to false
        myConnector.removeProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY);
        myConnector.putProperty(ACCEPTED_ISSUERS, "false");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("Gateway does not include an accepted issuers list", 0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true
        myConnector.removeProperty(ACCEPTED_ISSUERS);
        myConnector.putProperty(NO_ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("Gateway does not include an accepted issuers list",0, acceptedIssuers.length);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS11And12_NoIssuers() throws Exception {
        doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport("TLSv1.1");
        doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport("TLSv1.2");
    }

    private void doTestGetAcceptedIssuersForConnectorWithNoIssuersForTransport(final String transport) throws Exception {
        Assert.assertThat(transport, Matchers.not(Matchers.isEmptyOrNullString()));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("empty issuers list is expected", 0, acceptedIssuers.length);

        myConnector.putProperty(ACCEPTED_ISSUERS, "false");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("empty issuers list is expected", 0, acceptedIssuers.length);

        myConnector.removeProperty(ACCEPTED_ISSUERS);
        myConnector.putProperty(NO_ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("empty issuers list is expected", 0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.removeProperty(NO_ACCEPTED_ISSUERS);
        myConnector.putProperty("includeSelfCertIfAcceptedIssuersEmpty", "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("default cert should be returned", 1, acceptedIssuers.length);
    }

    @Test
    public void getAcceptedIssuersForConnector_TLS12_WithIssuers() throws Exception {
        doTestGetAcceptedIssuersForConnectorWithIssuersForTransport("TLSv1.1");
        doTestGetAcceptedIssuersForConnectorWithIssuersForTransport("TLSv1.2");
    }

    private void doTestGetAcceptedIssuersForConnectorWithIssuersForTransport(final String transport) throws Exception {
        Assert.assertThat(transport, Matchers.not(Matchers.isEmptyOrNullString()));
        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("default is to send accepted issuers", 2, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("includeSelfCertIfAcceptedIssuersEmpty has no impact if issuers list is not empty", 2, acceptedIssuers.length);

        // reset noAcceptedIssuers, set acceptedIssuers to false
        myConnector.removeProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY);
        myConnector.removeProperty(NO_ACCEPTED_ISSUERS);
        myConnector.putProperty(ACCEPTED_ISSUERS, "false");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("The Gateway's certificate request does not include an accepted issuers list.",
                0, acceptedIssuers.length);

        // reset acceptedIssuers, set acceptedIssuers to true
        myConnector.removeProperty(ACCEPTED_ISSUERS);
        myConnector.putProperty(NO_ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals("The Gateway's certificate request does not include an accepted issuers list.",0, acceptedIssuers.length);
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

        Mockito.when(trustedCertServices.getAllCertsByTrustFlags(Mockito.anySetOf(TrustedCert.TrustedFor.class))).thenReturn(CollectionUtils.set(certificate1, certificate2));

        final SsgConnector myConnector = new SsgConnector();
        myConnector.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        myConnector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, transport);

        X509Certificate[] acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        //Set includeSelfCertIfAcceptedIssuersEmpty to true to include the default ssl key
        myConnector.putProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        myConnector.removeProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY);
        myConnector.removeProperty(NO_ACCEPTED_ISSUERS);
        myConnector.putProperty(ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);

        myConnector.removeProperty(INCLUDE_SELF_CERT_IF_ACCEPTED_ISSUERS_EMPTY);
        myConnector.removeProperty(NO_ACCEPTED_ISSUERS);
        myConnector.putProperty(ACCEPTED_ISSUERS, "true");
        acceptedIssuers = transportModule.getAcceptedIssuersForConnector(myConnector);
        Assert.assertEquals(0, acceptedIssuers.length);
    }
}