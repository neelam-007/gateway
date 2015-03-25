package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.password.SecurePasswordManager;
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

import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
*
*/

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyActiveConnectorTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyActiveConnectorTest.class.getName());

    private final SecurityZone securityZone = new SecurityZone();
    private final SsgActiveConnector mqNative = new SsgActiveConnector();
    private final SecurePassword securePassword =  new SecurePassword();
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    private SecurityZoneManager securityZoneManager;
    private SecurePasswordManager securePasswordManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        ssgActiveConnectorManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class);


        //create secure password
        securePassword.setName("MyPassword");
        securePassword.setEncodedPassword("password");
        securePassword.setUsageFromVariable(true);
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePasswordManager.save(securePassword);


        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        //create mq native connector
        mqNative.setName("Test MQ Config");
        mqNative.setType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        mqNative.setEnabled(true);
        mqNative.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "host");
        mqNative.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT, "1234");
        mqNative.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, "qManager");
        mqNative.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED, "true");
        mqNative.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, securePassword.getId());
        mqNative.setSecurityZone(securityZone);
        ssgActiveConnectorManager.save(mqNative);
    }

    @After
    public void after() throws Exception {
        super.after();

        ssgActiveConnectorManager.delete(mqNative);
        securityZoneManager.delete(securityZone);
        securePasswordManager.delete(securePassword);
    }

    @Test
    public void MqNativeRoutingAssertionTest() throws Exception {

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
                "            <L7p:SsgActiveConnectorGoid goidValue=\""+mqNative.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorId goidValue=\""+mqNative.getId()+"\"/>\n" +
                "            <L7p:SsgActiveConnectorName stringValue=\""+mqNative.getName()+"\"/>\n" +
                "        </L7p:MqNativeRouting>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals("Test Policy dependencies", dependencyItem.getName());
                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO zoneDep = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getType());
                assertEquals(securityZone.getId(), zoneDep.getId());
                assertEquals(securityZone.getName(), zoneDep.getName());
                assertNull(zoneDep.getDependencies());

                DependencyMO keyDep = dependencyAnalysisMO.getDependencies().get(1);
                assertEquals(EntityType.SECURE_PASSWORD.toString(), keyDep.getType());
                assertEquals(securePassword.getId(), keyDep.getId());
                assertEquals(securePassword.getName(), keyDep.getName());
                assertNull( keyDep.getDependencies());

                DependencyMO mqDep = dependencyAnalysisMO.getDependencies().get(2);
                assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqDep.getType());
                assertEquals(mqNative.getId(), mqDep.getId());
                assertEquals(mqNative.getName(), mqDep.getName());
                assertEquals(2, mqDep.getDependencies().size());
                assertNotNull( "Missing dependency:"+securePassword.getId(), getDependency(mqDep.getDependencies(),securePassword.getId()));
                assertNotNull( "Missing dependency:"+securityZone.getId(), getDependency(mqDep.getDependencies(),securityZone.getId()));
            }
        });
    }

    @Test
    public void brokenReferenceTest() throws Exception {

        final Goid brokenReferenceGoid = new Goid(mqNative.getGoid().getLow(),mqNative.getGoid().getHi());
        final String brokenReferenceName = "brokenReferenceName";

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
                        "            <L7p:SsgActiveConnectorGoid goidValue=\""+brokenReferenceGoid.toString()+"\"/>\n" +
                        "            <L7p:SsgActiveConnectorId goidValue=\""+brokenReferenceGoid.toString()+"\"/>\n" +
                        "            <L7p:SsgActiveConnectorName stringValue=\""+ brokenReferenceName +"\"/>\n" +
                        "        </L7p:MqNativeRouting>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals("Test Policy dependencies", dependencyItem.getName());
                assertEquals(0, dependencyAnalysisMO.getDependencies().size());

                assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                assertNotNull(brokenDep);
                assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), brokenDep.getType());
                assertEquals(brokenReferenceName, brokenDep.getName());
                assertEquals(brokenReferenceGoid.toString(), brokenDep.getId());
            }
        });
    }
}
