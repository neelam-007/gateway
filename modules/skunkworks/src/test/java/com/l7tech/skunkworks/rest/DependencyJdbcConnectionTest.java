package com.l7tech.skunkworks.rest;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunOnNightly;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
*
*/
@ConditionalIgnore(condition = RunOnNightly.class)
public class DependencyJdbcConnectionTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyJdbcConnectionTest.class.getName());

    private Item<JDBCConnectionMO> jdbcConnectionItem;
    private Item<JDBCConnectionMO> jdbcConnectionPasswordItem;
    private Item<StoredPasswordMO> securePasswordItem;
    private Item<SecurityZoneMO> securityZoneItem;

    @Before
    public void before() throws Exception {
        super.before();

        //create security zone
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("Test security zone");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        securityZoneMO.setDescription("stuff");
        RestResponse response = getEnvironment().processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));
        assertOkCreatedResponse(response);
        securityZoneItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securityZoneItem.setContent(securityZoneMO);

        //create secure password
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("MyPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        response = getEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securePasswordItem.setContent(storedPasswordMO);

        // create jdbc connection in security zone
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setName("Test Connection");
        jdbcConnectionMO.setEnabled(false);
        jdbcConnectionMO.setDriverClass("com.l7tech.jdbc.mysql.MySQLDriver");
        jdbcConnectionMO.setJdbcUrl("bad url");
        jdbcConnectionMO.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("password", "password")
                .put("user", "jdbcUserName")
                .map());
        jdbcConnectionMO.setSecurityZoneId(securityZoneItem.getId());
        response = getEnvironment().processRequest("jdbcConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jdbcConnectionMO)));
        assertOkCreatedResponse(response);
        jdbcConnectionItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jdbcConnectionItem.setContent(jdbcConnectionMO);

        // create jdbc connection with secure password
        JDBCConnectionMO jdbcConnectionPasswordMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionPasswordMO.setName("Test Connection Password");
        jdbcConnectionPasswordMO.setEnabled(false);
        jdbcConnectionPasswordMO.setDriverClass("com.l7tech.jdbc.mysql.MySQLDriver");
        jdbcConnectionPasswordMO.setJdbcUrl("bad url");
        jdbcConnectionPasswordMO.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("password", "${secpass.MyPassword.plaintext}")
                .put("user", "jdbcUserName")
                .map());
        response = getEnvironment().processRequest("jdbcConnections", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(jdbcConnectionPasswordMO)));

        assertOkCreatedResponse(response);

        jdbcConnectionPasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        jdbcConnectionPasswordItem.setContent(jdbcConnectionPasswordMO);


    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        RestResponse response = getEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = getEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = getEnvironment().processRequest("jdbcConnections/" + jdbcConnectionItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        response = getEnvironment().processRequest("jdbcConnections/" + jdbcConnectionPasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);
    }

    @Test
    public void JdbcQueryAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:JdbcQuery>\n" +
                "        <L7p:ConnectionName stringValue=\""+jdbcConnectionItem.getName()+"\"/>\n" +
                "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                "            <L7p:SqlQuery stringValue=\"select * from blah\"/>\n" +
                "        </L7p:JdbcQuery>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jdbcConnectionItem);

                // verify security zone dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securityZoneItem.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securityZoneItem.getName(), passwordDep.getDependentObject().getName());
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
                        "        <L7p:ConnectionName stringValue=\""+jdbcConnectionPasswordItem.getName()+"\"/>\n" +
                        "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
                        "            <L7p:SqlQuery stringValue=\"select * from blah\"/>\n" +
                        "        </L7p:JdbcQuery>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),jdbcConnectionPasswordItem);

                // verify password dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securePasswordItem.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securePasswordItem.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURE_PASSWORD.toString(), passwordDep.getDependentObject().getType());
            }
        });
    }

    protected void verifyItem(Item item, Item<JDBCConnectionMO> jdbcConnectionItem){
        assertEquals(jdbcConnectionItem.getId(), item.getId());
        assertEquals(jdbcConnectionItem.getName(), item.getName());
        assertEquals(EntityType.JDBC_CONNECTION.toString(), item.getType());
    }
}
