package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
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
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyJmsTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyJmsTest.class.getName());

    private final JmsConnection jmsConnection =  new JmsConnection();
    private final JmsEndpoint jmsEndpoint =  new JmsEndpoint();
    private final JmsConnection jmsConnection1 =  new JmsConnection();
    private final JmsEndpoint jmsEndpoint1 =  new JmsEndpoint();
    private final JmsConnection jmsConnectionTemplate =  new JmsConnection();
    private final JmsEndpoint jmsEndpointTemplate =  new JmsEndpoint();
    private final SecurityZone securityZone =  new SecurityZone();
    private final SecurePassword securePassword =  new SecurePassword();
    private SecurePasswordManager securePasswordManager;
    private SecurityZoneManager securityZoneManager;
    private JmsEndpointManager jmsEndpointManager;
    private JmsConnectionManager jmsConnectionManager;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        jmsConnectionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsConnectionManager", JmsConnectionManager.class);
        jmsEndpointManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsEndpointManager", JmsEndpointManager.class);


        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        //create secure password
        securePassword.setName("MyPassword");
        securePassword.setEncodedPassword("password");
        securePassword.setUsageFromVariable(true);
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePasswordManager.save(securePassword);

        // create jms connection with endpoint secured password
        jmsConnection.setName( "JMS test Connection endpoint secure password" );
        jmsConnection.setQueueFactoryUrl("queueFactory");
        jmsConnection.setInitialContextFactoryClassname("contextClassname");
        jmsConnection.setJndiUrl("jndiUrl");
        jmsConnection.setProviderType(JmsProviderType.MQ);
        jmsConnection.setUsername("user");
        jmsConnection.setPassword("password");
        jmsConnectionManager.save(jmsConnection);

        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());
        jmsEndpoint.setName( "queueName" );
        jmsEndpoint.setDestinationName( "queueName" );
        jmsEndpoint.setUsername("user");
        jmsEndpoint.setPassword("${secpass.MyPassword.plaintext}");
        jmsEndpointManager.save(jmsEndpoint);

        // create jms connection with connection secured password
        jmsConnection1.setName( "JMS test Connection secure password" );
        jmsConnection1.setQueueFactoryUrl("queueFactory");
        jmsConnection1.setInitialContextFactoryClassname("contextClassname");
        jmsConnection1.setJndiUrl("jndiUrl");
        jmsConnection1.setProviderType(JmsProviderType.MQ);
        jmsConnection1.setUsername("user");
        jmsConnection1.setPassword("${secpass.MyPassword.plaintext}");
        jmsConnectionManager.save(jmsConnection1);

        jmsEndpoint1.setConnectionGoid(jmsConnection1.getGoid());
        jmsEndpoint1.setName( "queueName" );
        jmsEndpoint1.setDestinationName( "queueName" );
        jmsEndpoint1.setUsername("user");
        jmsEndpoint1.setPassword("password");
        jmsEndpointManager.save(jmsEndpoint1);


        // create jms connection template with security zone
        jmsConnectionTemplate.setName( "JMS test Connection template" );
        jmsConnectionTemplate.setQueueFactoryUrl("queueFactory");
        jmsConnectionTemplate.setInitialContextFactoryClassname("contextClassname");
        jmsConnectionTemplate.setJndiUrl("jndiUrl");
        jmsConnectionTemplate.setProviderType(JmsProviderType.MQ);
        jmsConnectionManager.save(jmsConnectionTemplate);

        jmsEndpointTemplate.setConnectionGoid(jmsConnectionTemplate.getGoid());
        jmsEndpointTemplate.setTemplate(true);
        jmsEndpointTemplate.setName("queueName");
        jmsEndpointTemplate.setDestinationName("queueName");
        jmsEndpointTemplate.setSecurityZone(securityZone);
        jmsEndpointManager.save(jmsEndpointTemplate);
    }


    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        securityZoneManager.delete(securityZone);
        securePasswordManager.delete(securePassword);
        jmsEndpointManager.delete(jmsEndpoint);
        jmsConnectionManager.delete(jmsConnection);
        jmsEndpointManager.delete(jmsEndpoint1);
        jmsConnectionManager.delete(jmsConnection1);
        jmsEndpointManager.delete(jmsEndpointTemplate);
        jmsConnectionManager.delete(jmsConnectionTemplate);

    }

    @Test
    public void JmsRouteAssertionTest() throws Exception {

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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jmsEndpoint);

                // verify secure password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
            }
        });
    }

    @Test
    public void JmsRouteAssertionConnectionPasswordTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:JmsRoutingAssertion>\n" +
                        "            <L7p:EndpointName stringValue=\"name\"/>\n" +
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jmsEndpoint1);

                // verify secure password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
            }
        });
    }

    @Test
    public void JmsRouteAssertionTemplateSecPasswordTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:JmsRoutingAssertion>\n" +
                "            <L7p:DynamicJmsRoutingProperties dynamicJmsRoutingProperties=\"included\">\n" +
                "                <L7p:DestPassword stringValue=\"password\"/>\n" +
                "                <L7p:DestUserName stringValue=\"zxcv\"/>\n" +
                "                <L7p:JndiPassword stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                "                <L7p:JndiUserName stringValue=\"zxcv\"/>\n" +
                "            </L7p:DynamicJmsRoutingProperties>\n" +
                "            <L7p:EndpointName stringValue=\"template\"/>\n" +
                "            <L7p:EndpointOid goidValue=\""+jmsEndpointTemplate.getId()+"\"/>\n" +
                "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
                "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
                "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
                "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
                "        </L7p:JmsRoutingAssertion>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO.getDependencies(),EntityType.JMS_ENDPOINT);
                verifyItem(dep.getDependentObject(), jmsEndpointTemplate);

                // verify secure password dependency
                DependencyMO passwordDep  =  getDependency(dependencyAnalysisMO.getDependencies(),EntityType.SECURE_PASSWORD);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());


                // verify security zone dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO securityZoneDep  = dep.getDependencies().get(0);
                assertEquals(securityZone.getId(), securityZoneDep.getDependentObject().getId());
                assertEquals(securityZone.getName(), securityZoneDep.getDependentObject().getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), securityZoneDep.getDependentObject().getType());
            }
        });
    }

    protected void verifyItem(Item item, JmsEndpoint endpoint){
        assertEquals(endpoint.getId(), item.getId());
        assertEquals(endpoint.getName(), item.getName());
        assertEquals(EntityType.JMS_ENDPOINT.toString(), item.getType());
    }
}
