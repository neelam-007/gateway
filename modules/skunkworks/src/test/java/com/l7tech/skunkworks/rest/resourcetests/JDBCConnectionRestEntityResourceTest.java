package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class JDBCConnectionRestEntityResourceTest extends RestEntityTests<JdbcConnection, JDBCConnectionMO> {
    private static final Logger logger = Logger.getLogger(JDBCConnectionRestEntityResourceTest.class.getName());

    private JdbcConnectionManager jdbcConnectionManager;
    private List<JdbcConnection> jdbcConnections = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        jdbcConnectionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jdbcConnectionManager", JdbcConnectionManager.class);
        //Create the active connectors

        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("Jdbc Connection 1");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setDriverClass("my.driver.class12");
        jdbcConnection.setJdbcUrl("connection/url");
        jdbcConnection.setMaxPoolSize(10);
        jdbcConnection.setMinPoolSize(5);
        jdbcConnection.setPassword("myPassword");
        jdbcConnection.setUserName("userName1");
        jdbcConnection.setAdditionalProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("prop", "value")
                .map());

        jdbcConnectionManager.save(jdbcConnection);
        jdbcConnections.add(jdbcConnection);

        jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("Jdbc Connection 2");
        jdbcConnection.setEnabled(true);
        jdbcConnection.setDriverClass("my.driver.class12");
        jdbcConnection.setJdbcUrl("connection/url2");
        jdbcConnection.setMaxPoolSize(10);
        jdbcConnection.setMinPoolSize(5);
        jdbcConnection.setPassword("myPassword");
        jdbcConnection.setUserName("userName23");
        jdbcConnection.setAdditionalProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("prop", "value")
                .map());

        jdbcConnectionManager.save(jdbcConnection);
        jdbcConnections.add(jdbcConnection);

        jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("Jdbc Connection 3");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setDriverClass("my.driver.class3");
        jdbcConnection.setJdbcUrl("connection/url3");
        jdbcConnection.setMaxPoolSize(10);
        jdbcConnection.setMinPoolSize(5);
        jdbcConnection.setPassword("myPassword");
        jdbcConnection.setUserName("userName23");
        jdbcConnection.setAdditionalProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("prop", "value")
                .map());

        jdbcConnectionManager.save(jdbcConnection);
        jdbcConnections.add(jdbcConnection);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<JdbcConnection> all = jdbcConnectionManager.findAll();
        for (JdbcConnection jdbcConnection : all) {
            jdbcConnectionManager.delete(jdbcConnection.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(jdbcConnections, new Functions.Unary<String, JdbcConnection>() {
            @Override
            public String call(JdbcConnection jdbcConnection) {
                return jdbcConnection.getId();
            }
        });
    }

    @Override
    public List<JDBCConnectionMO> getCreatableManagedObjects() {
        List<JDBCConnectionMO> jdbcConnectionMOs = new ArrayList<>();

        JDBCConnectionMO jdbcConnection = ManagedObjectFactory.createJDBCConnection();
        jdbcConnection.setId(getGoid().toString());
        jdbcConnection.setName("Jdbc Connection created");
        jdbcConnection.setDriverClass("my.driver.class");
        jdbcConnection.setJdbcUrl("connection.url");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("user", "myUser")
                .put("password", "myPassword")
                .map());
        jdbcConnectionMOs.add(jdbcConnection);

        return jdbcConnectionMOs;
    }

    @Override
    public List<JDBCConnectionMO> getUpdateableManagedObjects() {
        List<JDBCConnectionMO> jdbcConnectionMOs = new ArrayList<>();

        JdbcConnection jdbcConnection = this.jdbcConnections.get(0);
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setId(jdbcConnection.getId());
        jdbcConnectionMO.setName(jdbcConnection.getName() + " Updated");
        jdbcConnectionMO.setDriverClass(jdbcConnection.getDriverClass());
        jdbcConnectionMO.setJdbcUrl(jdbcConnection.getJdbcUrl());
        jdbcConnectionMO.setEnabled(jdbcConnection.isEnabled());
        jdbcConnectionMO.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("user", jdbcConnection.getUserName())
                .put("password", jdbcConnection.getPassword())
                .map());
        jdbcConnectionMOs.add(jdbcConnectionMO);
        return jdbcConnectionMOs;
    }

    @Override
    public Map<JDBCConnectionMO, Functions.BinaryVoid<JDBCConnectionMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<JDBCConnectionMO, Functions.BinaryVoid<JDBCConnectionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        JDBCConnectionMO jdbcConnection = ManagedObjectFactory.createJDBCConnection();
        jdbcConnection.setName(jdbcConnections.get(0).getName());
        jdbcConnection.setDriverClass("my.driver.class");
        jdbcConnection.setJdbcUrl("connection.url");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("user", "myUser")
                .map());
        builder.put(jdbcConnection, new Functions.BinaryVoid<JDBCConnectionMO, RestResponse>() {
            @Override
            public void call(JDBCConnectionMO jdbcConnectionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //try creating without a password
        jdbcConnection = ManagedObjectFactory.createJDBCConnection();
        jdbcConnection.setName("Jdbc Connection created");
        jdbcConnection.setDriverClass("my.driver.class");
        jdbcConnection.setJdbcUrl("connection.url");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("user", "myUser")
                .map());
        builder.put(jdbcConnection, new Functions.BinaryVoid<JDBCConnectionMO, RestResponse>() {
            @Override
            public void call(JDBCConnectionMO jdbcConnectionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //try creating without a user
        jdbcConnection = ManagedObjectFactory.createJDBCConnection();
        jdbcConnection.setName("Jdbc Connection created");
        jdbcConnection.setDriverClass("my.driver.class");
        jdbcConnection.setJdbcUrl("connection.url");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("password", "myPassword")
                .map());
        builder.put(jdbcConnection, new Functions.BinaryVoid<JDBCConnectionMO, RestResponse>() {
            @Override
            public void call(JDBCConnectionMO jdbcConnectionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @BugId("SSG-8126")
    @Test
    public void testCreateEntityFailedInvalidEnabledValue() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<l7:JDBCConnection xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>Jdbc_11_5_1_18_a</l7:Name>\n" +
                        "    <l7:Enabled>xxxx</l7:Enabled>\n" +
                        "    <l7:Extension>\n" +
                        "        <l7:DriverClass>com.l7tech.jdbc.db2.DB2Driver</l7:DriverClass>\n" +
                        "        <l7:JdbcUrl>jdbc:l7tech:db2://10.130.225.195:50000;DatabaseName=l7tech</l7:JdbcUrl>\n" +
                        "        <l7:ConnectionProperties>\n" +
                        "            <l7:Property key=\"user\">\n" +
                        "                <l7:StringValue>db2admin</l7:StringValue>\n" +
                        "            </l7:Property>\n" +
                        "            <l7:Property key=\"password\">\n" +
                        "                <l7:StringValue>db2admin</l7:StringValue>\n" +
                        "            </l7:Property>\n" +
                        "        </l7:ConnectionProperties>\n" +
                        "    </l7:Extension>\n" +
                        "</l7:JDBCConnection>"
        );

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        logger.log(Level.FINE, response.toString());
        Assert.assertEquals(400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<l7:JDBCConnection xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>Jdbc_11_5_1_18_a</l7:Name>\n" +
                        "    <l7:Enabled></l7:Enabled>\n" +
                        "    <l7:Extension>\n" +
                        "        <l7:DriverClass>com.l7tech.jdbc.db2.DB2Driver</l7:DriverClass>\n" +
                        "        <l7:JdbcUrl>jdbc:l7tech:db2://10.130.225.195:50000;DatabaseName=l7tech</l7:JdbcUrl>\n" +
                        "        <l7:ConnectionProperties>\n" +
                        "            <l7:Property key=\"user\">\n" +
                        "                <l7:StringValue>db2admin</l7:StringValue>\n" +
                        "            </l7:Property>\n" +
                        "            <l7:Property key=\"password\">\n" +
                        "                <l7:StringValue>db2admin</l7:StringValue>\n" +
                        "            </l7:Property>\n" +
                        "        </l7:ConnectionProperties>\n" +
                        "    </l7:Extension>\n" +
                        "</l7:JDBCConnection>"
        );

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        logger.log(Level.FINE, response.toString());
        Assert.assertEquals(400, response.getStatus());
    }

    @Override
    public Map<JDBCConnectionMO, Functions.BinaryVoid<JDBCConnectionMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<JDBCConnectionMO, Functions.BinaryVoid<JDBCConnectionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //same name as another connection
        JdbcConnection jdbcConnection = jdbcConnections.get(0);
        JDBCConnectionMO jdbcConnectionMO = ManagedObjectFactory.createJDBCConnection();
        jdbcConnectionMO.setId(jdbcConnection.getId());
        jdbcConnectionMO.setName(jdbcConnections.get(1).getName());
        jdbcConnectionMO.setDriverClass(jdbcConnection.getDriverClass());
        jdbcConnectionMO.setJdbcUrl(jdbcConnection.getJdbcUrl());
        jdbcConnectionMO.setEnabled(jdbcConnection.isEnabled());
        jdbcConnectionMO.setConnectionProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("user", jdbcConnection.getUserName())
                .put("password", jdbcConnection.getPassword())
                .map());
        builder.put(jdbcConnectionMO, new Functions.BinaryVoid<JDBCConnectionMO, RestResponse>() {
            @Override
            public void call(JDBCConnectionMO jdbcConnectionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(jdbcConnections, new Functions.Unary<String, JdbcConnection>() {
            @Override
            public String call(JdbcConnection jdbcConnection) {
                return jdbcConnection.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "jdbcConnections";
    }

    @Override
    public String getType() {
        return EntityType.JDBC_CONNECTION.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        JdbcConnection entity = jdbcConnectionManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        JdbcConnection entity = jdbcConnectionManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, JDBCConnectionMO managedObject) throws FindException {
        JdbcConnection entity = jdbcConnectionManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.isEnabled(), managedObject.isEnabled());
            Assert.assertEquals(entity.getJdbcUrl(), managedObject.getJdbcUrl());
            Assert.assertEquals(entity.getDriverClass(), managedObject.getDriverClass());
            Assert.assertNotNull(managedObject.getConnectionProperties());
            Assert.assertEquals(entity.getUserName(), managedObject.getConnectionProperties().get("user"));
            boolean hasPassword = false;
            if (managedObject.getConnectionProperties().get("password") != null) {
                Assert.assertEquals(entity.getPassword(), managedObject.getConnectionProperties().get("password"));
                hasPassword = true;
            }
            Assert.assertEquals(entity.getAdditionalProperties().size(), managedObject.getConnectionProperties().size() - (hasPassword ? 2 : 1));
            for (String propKey : entity.getAdditionalProperties().keySet()) {
                Assert.assertEquals(entity.getAdditionalProperties().get(propKey), managedObject.getConnectionProperties().get(propKey));
            }
            if (managedObject.getProperties() != null) {
                Assert.assertEquals(entity.getMaxPoolSize(), managedObject.getProperties().get("maximumPoolSize"));
                Assert.assertEquals(entity.getMinPoolSize(), managedObject.getProperties().get("minimumPoolSize"));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(jdbcConnections, new Functions.Unary<String, JdbcConnection>() {
                    @Override
                    public String call(JdbcConnection jdbcConnection) {
                        return jdbcConnection.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(jdbcConnections.get(0).getName()), Arrays.asList(jdbcConnections.get(0).getId()))
                .put("name=" + URLEncoder.encode(jdbcConnections.get(0).getName()) + "&name=" + URLEncoder.encode(jdbcConnections.get(1).getName()), Functions.map(jdbcConnections.subList(0, 2), new Functions.Unary<String, JdbcConnection>() {
                    @Override
                    public String call(JdbcConnection jdbcConnection) {
                        return jdbcConnection.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=true", Arrays.asList(jdbcConnections.get(1).getId()))
                .put("enabled=false", Arrays.asList(jdbcConnections.get(0).getId(), jdbcConnections.get(2).getId()))
                .put("jdbcUrl=" + URLEncoder.encode(jdbcConnections.get(0).getJdbcUrl()) + "&jdbcUrl=" + URLEncoder.encode(jdbcConnections.get(2).getJdbcUrl()), Arrays.asList(jdbcConnections.get(0).getId(), jdbcConnections.get(2).getId()))
                .put("driverClass=" + URLEncoder.encode(jdbcConnections.get(0).getDriverClass()), Arrays.asList(jdbcConnections.get(0).getId(), jdbcConnections.get(1).getId()))
                .put("userName=" + URLEncoder.encode(jdbcConnections.get(1).getUserName()), Arrays.asList(jdbcConnections.get(1).getId(), jdbcConnections.get(2).getId()))
                .put("name=" + URLEncoder.encode(jdbcConnections.get(0).getName()) + "&name=" + URLEncoder.encode(jdbcConnections.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(jdbcConnections.get(1).getId(), jdbcConnections.get(0).getId()))
                .map();
    }
}
