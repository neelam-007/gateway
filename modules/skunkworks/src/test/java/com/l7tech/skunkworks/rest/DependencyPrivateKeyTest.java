package com.l7tech.skunkworks.rest;

import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.PrivateKeyAdminHelper;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
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
    private SecurityZoneManager securityZoneManager;
    private DefaultKey defaultKey;
    private ApplicationEventPublisher applicationEventPublisher;

    private String keyAlias = "alice";
    private Goid defaultKeystoreId = new Goid(0, 2);

    @Before
    public void before() throws Exception {
        super.before();

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        keyStoreManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        defaultKey = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("defaultKey", DefaultKey.class);
        applicationEventPublisher = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("applicationEventProxy", ApplicationEventPublisher.class);

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
    }

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
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
    }


    @Test
    public void contextVariableTest() throws Exception {

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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>() {

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep = dependencyAnalysisMO.getDependencies().get(0);
                Item mqItem = dep.getDependentObject();

                assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqItem.getType());
                assertEquals(activeConnector.getId(), mqItem.getId());
                assertEquals(activeConnector.getName(), mqItem.getName());

                assertEquals(1, dep.getDependencies().size());
                DependencyMO keyDep = getDependency(dep.getDependencies(),EntityType.SSG_KEY_ENTRY);

                assertEquals(EntityType.SSG_KEY_ENTRY.toString(), keyDep.getDependentObject().getType());
                assertEquals(defaultKeystoreId.toString()+":"+keyAlias, keyDep.getDependentObject().getId());
                assertEquals(keyAlias, keyDep.getDependentObject().getName());

                assertEquals(1, keyDep.getDependencies().size());
                DependencyMO zoneDep = keyDep.getDependencies().get(0);

                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getDependentObject().getType());
                assertEquals(securityZone.getId(), zoneDep.getDependentObject().getId());
                assertEquals(securityZone.getName(), zoneDep.getDependentObject().getName());
            }
        });
    }


}
