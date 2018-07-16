package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
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
public class DependencyJdbcConnectionTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyJdbcConnectionTest.class.getName());

    private final SecurePassword securePassword =  new SecurePassword();
    private final JdbcConnection jdbcConnection =  new JdbcConnection();
    private final JdbcConnection jdbcConnectionPassword =  new JdbcConnection();
    private final SecurityZone securityZone =  new SecurityZone();
    private SecurePasswordManager securePasswordManager;
    private SecurityZoneManager securityZoneManager;
    private JdbcConnectionManager jdbcConnectionManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        jdbcConnectionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jdbcConnectionManager", JdbcConnectionManager.class);


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

        // create jdbc connection in security zone
        jdbcConnection.setName("Test Connection");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setDriverClass("com.l7tech.jdbc.mysql.MySQLDriver");
        jdbcConnection.setJdbcUrl("bad url");
        jdbcConnection.setPassword("password");
        jdbcConnection.setUserName("jdbcUserName");
        jdbcConnection.setSecurityZone(securityZone);
        jdbcConnectionManager.save(jdbcConnection);

        // create jdbc connection with secure password
        jdbcConnectionPassword.setName("Test Connection Password");
        jdbcConnectionPassword.setEnabled(false);
        jdbcConnectionPassword.setDriverClass("com.l7tech.jdbc.mysql.MySQLDriver");
        jdbcConnectionPassword.setJdbcUrl("bad url");
        jdbcConnectionPassword.setPassword("${secpass.MyPassword.plaintext}");
        jdbcConnectionPassword.setUserName("jdbcUserName");
        jdbcConnectionManager.save(jdbcConnectionPassword);


    }

    @After
    public void after() throws Exception {
        super.after();
        securePasswordManager.delete(securePassword);
        jdbcConnectionManager.delete(jdbcConnection);
        jdbcConnectionManager.delete(jdbcConnectionPassword);
        securityZoneManager.delete(securityZone);

    }

    @Test
    public void JdbcQueryAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:JdbcQuery>\n" +
                "        <L7p:ConnectionName stringValue=\""+jdbcConnection.getName()+"\"/>\n" +
                "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                "            <L7p:SqlQuery stringValue=\"select * from blah\"/>\n" +
                "        </L7p:JdbcQuery>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO, EntityType.JDBC_CONNECTION);
                assertEquals(jdbcConnection.getId(), dep.getId());
                assertEquals(jdbcConnection.getName(), dep.getName());
                assertEquals(EntityType.JDBC_CONNECTION.toString(), dep.getType());
                assertNotNull( "Missing dependency:"+securityZone.getId(), getDependency(dep.getDependencies(),securityZone.getId()));

                // verify security zone dependency
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURITY_ZONE);
                assertEquals(securityZone.getId(), passwordDep.getId());
                assertEquals(securityZone.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void JdbcQueryAssertionWithPasswordTest() throws Exception {
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:JdbcQuery>\n" +
                        "        <L7p:ConnectionName stringValue=\""+jdbcConnectionPassword.getName()+"\"/>\n" +
                        "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                        "            <L7p:SqlQuery stringValue=\"select * from blah\"/>\n" +
                        "        </L7p:JdbcQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = getDependency(dependencyAnalysisMO, EntityType.JDBC_CONNECTION);
                assertEquals(jdbcConnectionPassword.getId(), dep.getId());
                assertEquals(jdbcConnectionPassword.getName(), dep.getName());
                assertEquals(EntityType.JDBC_CONNECTION.toString(), dep.getType());
                assertNotNull( "Missing dependency:"+securePassword.getId(), getDependency(dep.getDependencies(),securePassword.getId()));

                // verify password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
                assertEquals(securePassword.getId(), passwordDep.getId());
                assertEquals(securePassword.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void brokenReferenceTest() throws Exception {

        final String brokenReferenceName = "brokenReference";

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:JdbcQuery>\n" +
                        "        <L7p:ConnectionName stringValue=\""+ brokenReferenceName +"\"/>\n" +
                        "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                        "            <L7p:SqlQuery stringValue=\"select * from blah\"/>\n" +
                        "        </L7p:JdbcQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0,dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.JDBC_CONNECTION.toString(), brokenDep.getType());
                assertEquals(brokenReferenceName, brokenDep.getName());
            }
        });

    }
}
