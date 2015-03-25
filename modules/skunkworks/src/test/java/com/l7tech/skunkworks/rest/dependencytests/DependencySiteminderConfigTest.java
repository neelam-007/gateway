package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
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
public class DependencySiteminderConfigTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencySiteminderConfigTest.class.getName());

    private final SecurityZone securityZone = new SecurityZone();
    private final SiteMinderConfiguration siteMinderConfiguration = new SiteMinderConfiguration();
    private final SecurePassword securePassword =  new SecurePassword();
    private SiteMinderConfigurationManager siteMinderConfigurationManager;
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

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(3, dependencyAnalysisMO.getDependencies().size());

                DependencyMO siteminderDep = getDependency(dependencyAnalysisMO,EntityType.SITEMINDER_CONFIGURATION);
                assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), siteminderDep.getType());
                assertEquals(siteMinderConfiguration.getId(), siteminderDep.getId());
                assertEquals(siteMinderConfiguration.getName(), siteminderDep.getName());
                assertNotNull( "Missing dependency:"+securePassword.getId(), getDependency(siteminderDep.getDependencies(),securePassword.getId()));
                assertNotNull( "Missing dependency:"+securityZone.getId(), getDependency(siteminderDep.getDependencies(),securityZone.getId()));

                DependencyMO keyDep = getDependency(dependencyAnalysisMO,EntityType.SECURE_PASSWORD);
                assertEquals(EntityType.SECURE_PASSWORD.toString(), keyDep.getType());
                assertEquals(securePassword.getId(), keyDep.getId());
                assertEquals(securePassword.getName(), keyDep.getName());

                DependencyMO zoneDep = getDependency(dependencyAnalysisMO, EntityType.SECURITY_ZONE);
                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getType());
                assertEquals(securityZone.getId(), zoneDep.getId());
                assertEquals(securityZone.getName(), zoneDep.getName());
            }
        });
    }

    @Test
    public void brokenReferenceTest() throws Exception {

        final Goid brokenReferenceGoid = new Goid(siteMinderConfiguration.getGoid().getLow(),siteMinderConfiguration.getGoid().getHi());

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:SiteMinderCheckProtected>\n" +
                        "            <L7p:AgentGoid goidValue=\""+ brokenReferenceGoid +"\"/>\n" +
                        "            <L7p:AgentId goidValue=\""+ brokenReferenceGoid +"\"/>\n" +
                        "            <L7p:ProtectedResource stringValue=\"protected resource\"/>\n" +
                        "            <L7p:Action stringValue=\"GET\"/>\n" +
                        "            <L7p:Prefix stringValue=\"prefix\"/>\n" +
                        "        </L7p:SiteMinderCheckProtected>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>\n";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(0, dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.SITEMINDER_CONFIGURATION.toString(), brokenDep.getType());
                assertEquals(brokenReferenceGoid.toString(), brokenDep.getId());
            }
        });
    }

}
