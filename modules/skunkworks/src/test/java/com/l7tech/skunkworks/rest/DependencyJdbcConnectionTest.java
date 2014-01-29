package com.l7tech.skunkworks.rest;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
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
public class DependencyJdbcConnectionTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyJdbcConnectionTest.class.getName());

    private final SecurePassword securePassword =  new SecurePassword();
    private final JdbcConnection jdbcConnection =  new JdbcConnection();
    private final JdbcConnection jdbcConnectionPassword =  new JdbcConnection();
    private final SecurityZone securityZone =  new SecurityZone();
    private SecurePasswordManager securePasswordManager;
    private SecurityZoneManager securityZoneManager;
    private JdbcConnectionManager jdbcConnectionManager;

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

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        securityZoneManager.delete(securityZone);
        securePasswordManager.delete(securePassword);
        jdbcConnectionManager.delete(jdbcConnection);
        jdbcConnectionManager.delete(jdbcConnectionPassword);

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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyAnalysisMO>>(){

            @Override
            public void call(Item<DependencyAnalysisMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyAnalysisMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jdbcConnection);

                // verify security zone dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securityZone.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securityZone.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), passwordDep.getDependentObject().getType());
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyAnalysisMO>>(){

            @Override
            public void call(Item<DependencyAnalysisMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyAnalysisMO dependencyAnalysisMO = dependencyItem.getContent();
                
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jdbcConnectionPassword);

                // verify password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePassword.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePassword.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
            }
        });
    }

    protected void verifyItem(Item item, JdbcConnection jdbcConnectionItem){
        assertEquals(jdbcConnectionItem.getId(), item.getId());
        assertEquals(jdbcConnectionItem.getName(), item.getName());
        assertEquals(EntityType.JDBC_CONNECTION.toString(), item.getType());
    }
}
