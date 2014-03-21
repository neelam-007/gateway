package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
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
public class DependencySiteminderConfigTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencySiteminderConfigTest.class.getName());

    private final SecurityZone securityZone = new SecurityZone();
    private SiteMinderConfigurationManager siteMinderConfigurationManager;
    private final SiteMinderConfiguration siteMinderConfiguration = new SiteMinderConfiguration();
    private SecurityZoneManager securityZoneManager;
    private final SecurePassword securePassword =  new SecurePassword();
    private SecurePasswordManager securePasswordManager;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        siteMinderConfigurationManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class);


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
        siteMinderConfiguration.setName("Test Siteminder Config");
        siteMinderConfiguration.setUserName("username");
        siteMinderConfiguration.setPasswordGoid(securePassword.getGoid());
        siteMinderConfiguration.setAddress("0.0.0.0");
        siteMinderConfiguration.setClusterThreshold(3);
        siteMinderConfiguration.setSecret("secret");
        siteMinderConfiguration.setSecurityZone(securityZone);
        siteMinderConfigurationManager.save(siteMinderConfiguration);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        siteMinderConfigurationManager.delete(siteMinderConfiguration);
        securityZoneManager.delete(securityZone);
        securePasswordManager.delete(securePassword);
    }

    @Test
    public void siteMinderCheckProtectedAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SiteMinderCheckProtected>\n" +
                "            <L7p:AgentGoid goidValue=\""+siteMinderConfiguration.getId()+"\"/>\n" +
                "            <L7p:AgentId goidValue=\""+siteMinderConfiguration.getName()+"\"/>\n" +
                "            <L7p:ProtectedResource stringValue=\"protected resource\"/>\n" +
                "            <L7p:Action stringValue=\"GET\"/>\n" +
                "            <L7p:Prefix stringValue=\"prefix\"/>\n" +
                "        </L7p:SiteMinderCheckProtected>\n" +
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

                assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), mqItem.getType());
                assertEquals(siteMinderConfiguration.getId(), mqItem.getId());
                assertEquals(siteMinderConfiguration.getName(), mqItem.getName());

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
