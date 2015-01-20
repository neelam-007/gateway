package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
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


@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyCassandraConnectionTest extends DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyCassandraConnectionTest.class.getName());

    private final SecurePassword securePassword = new SecurePassword();
    private final SecurityZone securityZone = new SecurityZone();
    private SecurePasswordManager securePasswordManager;
    private SecurityZoneManager securityZoneManager;
    private CassandraConnectionEntityManager cassandraEntityManager;
    private CassandraConnection cassandraConnection;
    private CassandraConnection cassandraConnectionPassword;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        cassandraEntityManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("cassandraEntityManager", CassandraConnectionEntityManager.class);

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

        cassandraConnection = new CassandraConnection();
        // create Cassandra connection in security zone
        cassandraConnection.setName("Test Connection");
        cassandraConnection.setCompression(CassandraConnection.COMPRESS_NONE);
        cassandraConnection.setSsl(false);
        cassandraConnection.setKeyspaceName("test");
        cassandraConnection.setPort("9042");
        cassandraConnection.setContactPoints("localhost");
        cassandraConnection.setEnabled(false);
        cassandraConnection.setSecurityZone(securityZone);
        cassandraEntityManager.save(cassandraConnection);

        cassandraConnectionPassword = new CassandraConnection();
        // create Cassandra connection with secure password
        cassandraConnectionPassword.setName("Test Connection Password");
        cassandraConnectionPassword.setCompression(CassandraConnection.COMPRESS_NONE);
        cassandraConnectionPassword.setSsl(false);
        cassandraConnectionPassword.setKeyspaceName("test");
        cassandraConnectionPassword.setPort("9042");
        cassandraConnectionPassword.setContactPoints("localhost");
        cassandraConnectionPassword.setEnabled(false);
        cassandraConnectionPassword.setUsername("cassandra");
        cassandraConnectionPassword.setPasswordGoid(securePassword.getGoid());
        cassandraEntityManager.save(cassandraConnectionPassword);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        cassandraEntityManager.delete(cassandraConnection);
        cassandraEntityManager.delete(cassandraConnectionPassword);
        securityZoneManager.delete(securityZone);
        securePasswordManager.delete(securePassword);
    }

    @Test
    public void CassandraQueryAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:CassandraQuery>\n" +
                        "            <L7p:ConnectionName stringValue=\"" + cassandraConnection.getName() + "\"/>\n" +
                        "            <L7p:QueryDocument stringValue=\"select * from users;\"/>\n" +
                        "            <L7p:QueryTimeout stringValue=\"10\"/>\n" +
                        "        </L7p:CassandraQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(2, dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep = getDependency(dependencyAnalysisMO, EntityType.CASSANDRA_CONFIGURATION);
                assertEquals(cassandraConnection.getId(), dep.getId());
                assertEquals(cassandraConnection.getName(), dep.getName());
                assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), dep.getType());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(dep.getDependencies(), securityZone.getId()));

                // verify security zone dependency
                DependencyMO passwordDep = getDependency(dependencyAnalysisMO, EntityType.SECURITY_ZONE);
                assertEquals(securityZone.getId(), passwordDep.getId());
                assertEquals(securityZone.getName(), passwordDep.getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), passwordDep.getType());
            }
        });
    }

    @Test
    public void CassandraQueryAssertionWithPasswordTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:CassandraQuery>\n" +
                        "            <L7p:ConnectionName stringValue=\"" + cassandraConnectionPassword.getName() + "\"/>\n" +
                        "            <L7p:QueryDocument stringValue=\"select * from users;\"/>\n" +
                        "            <L7p:QueryTimeout stringValue=\"10\"/>\n" +
                        "        </L7p:CassandraQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(2, dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep = getDependency(dependencyAnalysisMO, EntityType.CASSANDRA_CONFIGURATION);
                assertEquals(cassandraConnectionPassword.getId(), dep.getId());
                assertEquals(cassandraConnectionPassword.getName(), dep.getName());
                assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), dep.getType());
                assertNotNull("Missing dependency:" + securePassword.getId(), getDependency(dep.getDependencies(), securePassword.getId()));

                // verify password dependency
                assertEquals(1, dep.getDependencies().size());
                DependencyMO passwordDep = getDependency(dependencyAnalysisMO, EntityType.SECURE_PASSWORD);
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
                        "        <L7p:CassandraQuery>\n" +
                        "            <L7p:ConnectionName stringValue=\"" + brokenReferenceName + "\"/>\n" +
                        "            <L7p:QueryDocument stringValue=\"select * from users;\"/>\n" +
                        "            <L7p:QueryTimeout stringValue=\"10\"/>\n" +
                        "        </L7p:CassandraQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyListMO>>() {

            @Override
            public void call(Item<DependencyListMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0, dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.CASSANDRA_CONFIGURATION.toString(), brokenDep.getType());
                assertEquals(brokenReferenceName, brokenDep.getName());
            }
        });

    }
}

