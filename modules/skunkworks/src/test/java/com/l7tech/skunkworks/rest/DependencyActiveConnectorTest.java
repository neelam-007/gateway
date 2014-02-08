package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.*;

import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*
*/

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyActiveConnectorTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyActiveConnectorTest.class.getName());

    private final SecurityZone securityZone = new SecurityZone();
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    private final SsgActiveConnector mqNative = new SsgActiveConnector();
    private SecurityZoneManager securityZoneManager;
    private final SecurePassword securePassword =  new SecurePassword();
    private SecurePasswordManager securePasswordManager;

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

    @BeforeClass
    public static void beforeClass()  throws PolicyAssertionException, IllegalAccessException, InstantiationException  {
        DependencyTestBase.beforeClass();
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>() {

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1, dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep = dependencyAnalysisMO.getDependencies().get(0);
                Item mqItem = dep.getDependentObject();

                assertEquals(EntityType.SSG_ACTIVE_CONNECTOR.toString(), mqItem.getType());
                assertEquals(mqNative.getId(), mqItem.getId());
                assertEquals(mqNative.getName(), mqItem.getName());

                assertEquals(2, dep.getDependencies().size());
                DependencyMO keyDep = getDependency(dep.getDependencies(),EntityType.SECURE_PASSWORD);

                assertEquals(EntityType.SECURE_PASSWORD.toString(), keyDep.getDependentObject().getType());
                assertEquals(securePassword.getId(), keyDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), keyDep.getDependentObject().getName());

                DependencyMO zoneDep = getDependency(dep.getDependencies(), EntityType.SECURITY_ZONE);

                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getDependentObject().getType());
                assertEquals(securityZone.getId(), zoneDep.getDependentObject().getId());
                assertEquals(securityZone.getName(), zoneDep.getDependentObject().getName());
            }
        });
    }
}
