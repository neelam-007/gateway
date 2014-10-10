package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.PrivateKeyAdminHelper;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.*;
import org.springframework.context.ApplicationEventPublisher;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyPrivateKeyTest extends DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyPrivateKeyTest.class.getName());

    private SsgKeyStoreManager keyStoreManager;
    private final SecurityZone securityZone = new SecurityZone();
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    private final SsgActiveConnector activeConnector = new SsgActiveConnector();
    private final JmsConnection jmsConnection =  new JmsConnection();
    private final JmsEndpoint jmsEndpoint =  new JmsEndpoint();
    private final JmsConnection jmsConnection1 =  new JmsConnection();
    private final JmsEndpoint jmsEndpoint1 =  new JmsEndpoint();
    private SecurityZoneManager securityZoneManager;
    private JmsEndpointManager jmsEndpointManager;
    private JmsConnectionManager jmsConnectionManager;
    private DefaultKey defaultKey;
    private ApplicationEventPublisher applicationEventPublisher;

    private String keyAlias = "alice";
    private Goid defaultKeystoreId = new Goid(0, 2);


    private String brokenKeyAlias = "brokenKeyAlias";
    private Goid brokenKeystoreId = new Goid(2, 0);

    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        keyStoreManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        defaultKey = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("defaultKey", DefaultKey.class);
        applicationEventPublisher = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("applicationEventProxy", ApplicationEventPublisher.class);
        jmsConnectionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsConnectionManager", JmsConnectionManager.class);
        jmsEndpointManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsEndpointManager", JmsEndpointManager.class);

        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        //create private key
        PrivateKeyAdminHelper privateKeyAdminHelper = new PrivateKeyAdminHelper(defaultKey, keyStoreManager, applicationEventPublisher);
        final KeyGenParams keyGenParams = new KeyGenParams(2048);
        final SsgKeyMetadata keyMetadata = new SsgKeyMetadata(defaultKeystoreId, keyAlias, securityZone);
        Future<X509Certificate> certFuture = privateKeyAdminHelper.doGenerateKeyPair(
                defaultKeystoreId,
                keyAlias,
                keyMetadata, // set metadata for security zone
                new X500Principal("cn=" + keyAlias, ValidationUtils.getOidKeywordMap()),
                keyGenParams,
                365 * 5,
                false,
                KeyGenParams.getSignatureAlgorithm(KeyGenParams.ALGORITHM_RSA,"SHA256"));
        X509Certificate cert;
        while(true){
            if(certFuture.isCancelled() || certFuture.isDone()){
                cert = certFuture.get();
                break;
            }
        }

        ssgActiveConnectorManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class);

        //Create the active connector
        activeConnector.setName("Test MQ Config");
        activeConnector.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        activeConnector.setEnabled(true);
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, defaultKeystoreId.toString() );
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS, keyAlias );
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED, "true" );
        activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED, "true" );

        ssgActiveConnectorManager.save(activeConnector);

        // create jms connection with private key reference
        jmsConnection.setName( "JMS test Connection endpoint key reference" );
        jmsConnection.setQueueFactoryUrl("queueFactory");
        jmsConnection.setInitialContextFactoryClassname("contextClassname");
        jmsConnection.setJndiUrl("jndiUrl");
        jmsConnection.setProviderType(JmsProviderType.MQ);
        jmsConnection.setUsername("user");
        jmsConnection.setPassword("password");
        final Properties properties = new Properties();
        properties.put("com.l7tech.server.jms.prop.jndi.ssgKeyAlias", keyAlias);
        properties.put("com.l7tech.server.jms.prop.jndi.ssgKeystoreId", defaultKeystoreId.toString());
        properties.put("com.tibco.tibjms.naming.security_protocol", "ssl");
        properties.put("com.l7tech.server.jms.prop.customizer.class", "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer");
        properties.put("com.l7tech.server.jms.prop.queue.ssgKeyAlias", keyAlias);
        properties.put("com.l7tech.server.jms.prop.queue.ssgKeystoreId", defaultKeystoreId.toString());
        jmsConnection.properties(properties);
        jmsConnectionManager.save(jmsConnection);

        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());
        jmsEndpoint.setName("queueName");
        jmsEndpoint.setDestinationName("queueName");
        jmsEndpoint.setUsername("user");
        jmsEndpoint.setPassword("password}");
        jmsEndpointManager.save(jmsEndpoint);

        // create jms connection with private key reference
        jmsConnection1.setName( "JMS test Connection broken key reference" );
        jmsConnection1.setQueueFactoryUrl("queueFactory");
        jmsConnection1.setInitialContextFactoryClassname("contextClassname");
        jmsConnection1.setJndiUrl("jndiUrl");
        jmsConnection1.setProviderType(JmsProviderType.MQ);
        jmsConnection1.setUsername("user");
        jmsConnection1.setPassword("password");
        final Properties properties1 = new Properties();
        properties1.put("com.l7tech.server.jms.prop.jndi.ssgKeyAlias", brokenKeyAlias);
        properties1.put("com.l7tech.server.jms.prop.jndi.ssgKeystoreId", brokenKeystoreId.toString());
        properties1.put("com.tibco.tibjms.naming.security_protocol", "ssl");
        properties1.put("com.l7tech.server.jms.prop.customizer.class", "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer");
        properties1.put("com.l7tech.server.jms.prop.queue.ssgKeyAlias", brokenKeyAlias);
        properties1.put("com.l7tech.server.jms.prop.queue.ssgKeystoreId", brokenKeystoreId.toString());
        jmsConnection1.properties(properties1);
        jmsConnectionManager.save(jmsConnection1);

        jmsEndpoint1.setConnectionGoid(jmsConnection1.getGoid());
        jmsEndpoint1.setName("queueName");
        jmsEndpoint1.setDestinationName("queueName");
        jmsEndpoint1.setUsername("user");
        jmsEndpoint1.setPassword("password");
        jmsEndpointManager.save(jmsEndpoint1);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        PrivateKeyAdminHelper helper = new PrivateKeyAdminHelper(defaultKey, keyStoreManager, applicationEventPublisher);
        final SsgKeyStore keystore = keyStoreManager.findByPrimaryKey(defaultKeystoreId).getKeyStore();
        helper.doDeletePrivateKeyEntry(keystore, keyAlias);
        securityZoneManager.delete(securityZone);
        ssgActiveConnectorManager.delete(activeConnector);
        jmsEndpointManager.delete(jmsEndpoint);
        jmsConnectionManager.delete(jmsConnection);
        jmsEndpointManager.delete(jmsEndpoint1);
        jmsConnectionManager.delete(jmsConnection1);
    }


    @Test
    public void mqNativeRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:MqNativeRouting>\n" +
                "            <L7p:RequestMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:RequestMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseMessageAdvancedProperties mapValueNull=\"null\"/>\n" +
                "            <L7p:ResponseMqNativeMessagePropertyRuleSet mappingRuleSet=\"included\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:SsgActiveConnectorGoid goidValue=\""+activeConnector.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorId goidValue=\""+activeConnector.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorName stringValue=\""+activeConnector.getName()+"\"/>\n" +
                "        </L7p:MqNativeRouting>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO mqDep = getDependency(dependencyAnalysisMO,EntityType.SSG_ACTIVE_CONNECTOR);
                assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqDep.getType());
                assertEquals(activeConnector.getId(), mqDep.getId());
                assertEquals(activeConnector.getName(), mqDep.getName());
                assertNotNull("Missing dependency:" + defaultKeystoreId.toString() + ":" + keyAlias, getDependency(mqDep.getDependencies(), defaultKeystoreId.toString() + ":" + keyAlias));

                DependencyMO keyDep = getDependency(dependencyAnalysisMO,EntityType.SSG_KEY_ENTRY);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), keyDep.getType());
                assertEquals(defaultKeystoreId.toString()+":"+keyAlias, keyDep.getId());
                assertEquals(keyAlias, keyDep.getName());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(keyDep.getDependencies(), securityZone.getId()));


                DependencyMO zoneDep = getDependency(dependencyAnalysisMO,EntityType.SECURITY_ZONE);
                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getType());
                assertEquals(securityZone.getId(), zoneDep.getId());
                assertEquals(securityZone.getName(), zoneDep.getName());
            }
        });
    }

    @Test
    public void FtpsRoutingAssertionTest() throws Exception{
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:FtpRoutingAssertion>\n" +
                "            <L7p:Arguments stringValue=\"hr\"/>\n" +
                "            <L7p:ClientCertKeyAlias stringValue=\""+keyAlias+"\"/>\n" +
                "            <L7p:ClientCertKeystoreId goidValue=\""+defaultKeystoreId+"\"/>\n" +
                "            <L7p:CredentialsSource credentialsSource=\"passThru\"/>\n" +
                "            <L7p:Directory stringValue=\"hr\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "            <L7p:HostName stringValue=\"rdhzf\"/>\n" +
                "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                "                <L7p:Target target=\"RESPONSE\"/>\n" +
                "            </L7p:ResponseTarget>\n" +
                "            <L7p:Security security=\"ftpsExplicit\"/>\n" +
                "            <L7p:UseClientCert booleanValue=\"true\"/>\n" +
                "            <L7p:UserName stringValue=\"\"/>\n" +
                "        </L7p:FtpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2, dependencyAnalysisMO.getDependencies().size());

                DependencyMO keyDep = getDependency(dependencyAnalysisMO,EntityType.SSG_KEY_ENTRY);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), keyDep.getType());
                assertEquals(defaultKeystoreId.toString()+":"+keyAlias, keyDep.getId());
                assertEquals(keyAlias, keyDep.getName());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(keyDep.getDependencies(), securityZone.getId()));

                DependencyMO zoneDep = getDependency(dependencyAnalysisMO,EntityType.SECURITY_ZONE);
                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getType());
                assertEquals(securityZone.getId(), zoneDep.getId());
                assertEquals(securityZone.getName(), zoneDep.getName());
            }
        });

    }

    @Test
    public void brokenAliasReferenceTest() throws Exception{

        final String brokenReferenceUri = "brokenReference";

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:FtpRoutingAssertion>\n" +
                        "            <L7p:Arguments stringValue=\"hr\"/>\n" +
                        "            <L7p:ClientCertKeyAlias stringValue=\""+brokenReferenceUri+"\"/>\n" +
                        "            <L7p:ClientCertKeystoreId goidValue=\""+defaultKeystoreId+"\"/>\n" +
                        "            <L7p:CredentialsSource credentialsSource=\"passThru\"/>\n" +
                        "            <L7p:Directory stringValue=\"hr\"/>\n" +
                        "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                        "            <L7p:HostName stringValue=\"rdhzf\"/>\n" +
                        "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                        "                <L7p:Target target=\"RESPONSE\"/>\n" +
                        "            </L7p:ResponseTarget>\n" +
                        "            <L7p:Security security=\"ftpsExplicit\"/>\n" +
                        "            <L7p:UseClientCert booleanValue=\"true\"/>\n" +
                        "            <L7p:UserName stringValue=\"\"/>\n" +
                        "        </L7p:FtpRoutingAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(0, dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), brokenDep.getType());
                assertEquals(defaultKeystoreId + ":" + brokenReferenceUri, brokenDep.getId());
                assertEquals(brokenReferenceUri, brokenDep.getName());
            }
        });

    }

    @Test
    public void brokenKeystoreReferenceTest() throws Exception{

        final Goid brokenReferenceGoid = new Goid(defaultKeystoreId.getLow(),defaultKeystoreId.getHi());

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:FtpRoutingAssertion>\n" +
                        "            <L7p:Arguments stringValue=\"hr\"/>\n" +
                        "            <L7p:ClientCertKeyAlias stringValue=\""+keyAlias+"\"/>\n" +
                        "            <L7p:ClientCertKeystoreId goidValue=\""+brokenReferenceGoid+"\"/>\n" +
                        "            <L7p:CredentialsSource credentialsSource=\"passThru\"/>\n" +
                        "            <L7p:Directory stringValue=\"hr\"/>\n" +
                        "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                        "            <L7p:HostName stringValue=\"rdhzf\"/>\n" +
                        "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                        "                <L7p:Target target=\"RESPONSE\"/>\n" +
                        "            </L7p:ResponseTarget>\n" +
                        "            <L7p:Security security=\"ftpsExplicit\"/>\n" +
                        "            <L7p:UseClientCert booleanValue=\"true\"/>\n" +
                        "            <L7p:UserName stringValue=\"\"/>\n" +
                        "        </L7p:FtpRoutingAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(0, dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), brokenDep.getType());
                assertEquals(brokenReferenceGoid.toString() + ":" + keyAlias, brokenDep.getId());
                assertEquals(keyAlias, brokenDep.getName());
            }
        });
    }

    @Test
    public void brokenJmsKeystoreReferenceTest() throws Exception{

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:JmsRoutingAssertion>\n" +
                        "            <L7p:EndpointName stringValue=\" "+ jmsEndpoint1.getName()+"\"/>\n" +
                        "            <L7p:EndpointOid goidValue=\""+jmsEndpoint1.getId()+"\"/>\n" +
                        "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                        "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                        "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
                        "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                        "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                        "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
                        "        </L7p:JmsRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());
                DependencyMO jmsDep  = dependencyAnalysisMO.getDependencies().get(0);
                Assert.assertNotNull(jmsDep);
                assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDep.getType());
                assertEquals(jmsEndpoint1.getId(), jmsDep.getId());
                assertEquals(jmsEndpoint1.getName(), jmsDep.getName());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), brokenDep.getType());
                assertEquals(brokenKeystoreId.toString() + ":" + brokenKeyAlias, brokenDep.getId());
                assertEquals(brokenKeyAlias, brokenDep.getName());
            }
        });
    }

    @Test
    public void jmsKeystoreReferenceTest() throws Exception{

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:JmsRoutingAssertion>\n" +
                        "            <L7p:EndpointName stringValue=\"name\"/>\n" +
                        "            <L7p:EndpointOid goidValue=\""+jmsEndpoint.getId()+"\"/>\n" +
                        "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                        "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                        "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
                        "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                        "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                        "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
                        "        </L7p:JmsRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());
                DependencyMO jmsDep  = getDependency(dependencyAnalysisMO,EntityType.JMS_ENDPOINT);
                Assert.assertNotNull(jmsDep);
                assertEquals(EntityType.JMS_ENDPOINT.toString(), jmsDep.getType());
                assertEquals(jmsEndpoint.getId(), jmsDep.getId());
                assertEquals(jmsEndpoint.getName(), jmsDep.getName());

                DependencyMO keyDep = getDependency(dependencyAnalysisMO,EntityType.SSG_KEY_ENTRY);
                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), keyDep.getType());
                assertEquals(defaultKeystoreId.toString()+":"+keyAlias, keyDep.getId());
                assertEquals(keyAlias, keyDep.getName());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(keyDep.getDependencies(), securityZone.getId()));

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(0, dependencyAnalysisMO.getMissingDependencies().size());

            }
        });
    }
}
