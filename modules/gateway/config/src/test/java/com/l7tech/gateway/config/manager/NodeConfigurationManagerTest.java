package com.l7tech.gateway.config.manager;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.Properties;
import static org.junit.Assert.*;

/**
 *
 */
public class NodeConfigurationManagerTest {

    {
        SyspropUtil.setProperty( "com.l7tech.util.buildVersion", "4.7.0" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.util.buildVersion"
        );
    }

    @Test
    public void testLoadBasicConfiguration() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(basicConfigurationProperties));

        NodeConfig config = NodeConfigurationManager.loadNodeConfig( "test", properties, true );

        Assert.assertNotNull("Missing database config", config.getDatabases());
        Assert.assertEquals("Should be one database config", 1, config.getDatabases().size());
        Assert.assertNull(config.getDatabases().iterator().next().getParent());
    }

    @Test
    public void testLoadConfigurationWithSecret() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(configurationProperties));

        NodeConfig config = NodeConfigurationManager.loadNodeConfig( "test", properties, true );

        Assert.assertNotNull("Configuration not loaded.", config);
        Assert.assertNotNull("Configuration name not loaded.", config.getName());
        Assert.assertNotNull("Database configuration is missing.", config.getDatabases() );

        Assert.assertEquals("Node ID loaded incorrectly", "d21d57cbf8fd45c183ba9131d05a883f", config.getGuid() );
        Assert.assertEquals("Node name loaded incorrectly", "test", config.getName() );
        Assert.assertEquals("Node enabled loaded incorrectly", true, config.isEnabled() );

        for ( DatabaseConfig dbconfig : config.getDatabases() ) {
            Assert.assertNotNull("Database configuration password not loaded.", dbconfig.getNodePassword() );

            Assert.assertEquals("Database configuration port loaded incorrectly.", 3306, dbconfig.getPort() );
            Assert.assertEquals("Database configuration name loaded incorrectly.", "ssg", dbconfig.getName() );
            Assert.assertEquals("Database configuration username loaded incorrectly.", "gateway", dbconfig.getNodeUsername() );
            Assert.assertEquals("Database configuration password loaded incorrectly.", "$L7C$vI8T03Cz6EcVSw4nqRT6uA==$bScWpnxtQrUi8CwPxMDTbw==", dbconfig.getNodePassword() );

            if ( "littlefish2".equals(dbconfig.getHost()) ) {
                Assert.assertEquals("Database configuration type loaded incorrectly.", DatabaseType.NODE_ALL, dbconfig.getType() );
                Assert.assertEquals("Database configuration cluster type  loaded incorrectly.", NodeConfig.ClusterType.REPL_MASTER, dbconfig.getClusterType() );
            } else if ("failoverhost".equals(dbconfig.getHost())) {
                Assert.assertEquals("Database configuration cluster type  loaded incorrectly.", NodeConfig.ClusterType.REPL_SLAVE, dbconfig.getClusterType() );
            } else {
                Assert.fail("Unexpected host : " + dbconfig.getHost());
            }
        }
    }

    @Test
    public void testLoadConfigurationNoSecret() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(configurationProperties));

        NodeConfig config = NodeConfigurationManager.loadNodeConfig( "test", properties, false );

        Assert.assertNotNull("Configuration not loaded.", config);
        Assert.assertNotNull("Configuration name not loaded.", config.getName());
        Assert.assertNotNull("Database configuration is missing.", config.getDatabases() );

        Assert.assertEquals("Node ID loaded incorrectly", "d21d57cbf8fd45c183ba9131d05a883f", config.getGuid() );
        Assert.assertEquals("Node name loaded incorrectly", "test", config.getName() );
        Assert.assertEquals("Node enabled loaded incorrectly", true, config.isEnabled() );

        for ( DatabaseConfig dbconfig : config.getDatabases() ) {
            Assert.assertNull("Database configuration password loaded.", dbconfig.getNodePassword() );

            Assert.assertEquals("Database configuration port loaded incorrectly.", 3306, dbconfig.getPort() );
            Assert.assertEquals("Database configuration name loaded incorrectly.", "ssg", dbconfig.getName() );
            Assert.assertEquals("Database configuration username loaded incorrectly.", "gateway", dbconfig.getNodeUsername() );

            if ( "littlefish2".equals(dbconfig.getHost()) ) {
                Assert.assertEquals("Database configuration type loaded incorrectly.", DatabaseType.NODE_ALL, dbconfig.getType() );
                Assert.assertEquals("Database configuration cluster type  loaded incorrectly.", NodeConfig.ClusterType.REPL_MASTER, dbconfig.getClusterType() );
            } else if ("failoverhost".equals(dbconfig.getHost())) {
                Assert.assertEquals("Database configuration cluster type  loaded incorrectly.", NodeConfig.ClusterType.REPL_SLAVE, dbconfig.getClusterType() );
            } else {
                Assert.fail("Unexpected host : " + dbconfig.getHost());
            }
        }
    }

    @Test
    public void createDerbyQueries(){
        final String query = NodeConfigurationManager.createDerbyQueries("pmadmin", "7layer", "nodeconfigmantest.l7tech.com");
        assertTrue(query.contains("UPDATE internal_user set name='pmadmin',login='pmadmin',password='"));
        assertTrue(query.contains("',version=1 where objectid=3;"));
        assertTrue(query.contains("UPDATE cluster_properties set propvalue='nodeconfigmantest.l7tech.com',version=1 where objectid=-700001 and propkey='cluster.hostname';"));
    }

    @Test
    public void createDerbyQueriesEscapChars(){
        final String query = NodeConfigurationManager.createDerbyQueries("pm'admin'", "7layer", "node'configman'test.l7tech.com");
        assertTrue(query.contains("UPDATE internal_user set name='pm''admin''',login='pm''admin''',password='"));
        assertTrue(query.contains("',version=1 where objectid=3;"));
        assertTrue(query.contains("UPDATE cluster_properties set propvalue='node''configman''test.l7tech.com',version=1 where objectid=-700001 and propkey='cluster.hostname';"));
    }

    @Test
    public void createDerbyQueriesNoAdminLogin(){
        final String query = NodeConfigurationManager.createDerbyQueries(null, "7layer", "nodeconfigmantest.l7tech.com");
        assertFalse(query.contains("UPDATE internal_user"));
        assertTrue(query.contains("UPDATE cluster_properties set propvalue='nodeconfigmantest.l7tech.com',version=1 where objectid=-700001 and propkey='cluster.hostname';"));
    }

    @Test
    public void createDerbyQueriesNoAdminPassword(){
        final String query = NodeConfigurationManager.createDerbyQueries("pmadmin", null, "nodeconfigmantest.l7tech.com");
        assertFalse(query.contains("UPDATE internal_user"));
        assertTrue(query.contains("UPDATE cluster_properties set propvalue='nodeconfigmantest.l7tech.com',version=1 where objectid=-700001 and propkey='cluster.hostname';"));
    }

    @Test
    public void createDerbyQueriesNoClusterHost(){
        final String query = NodeConfigurationManager.createDerbyQueries("pmadmin", "7layer", null);
        assertTrue(query.contains("UPDATE internal_user set name='pmadmin',login='pmadmin',password='"));
        assertTrue(query.contains("',version=1 where objectid=3;"));
        assertFalse(query.contains("UPDATE cluster_properties"));
    }

    @Test
    public void derbyEscape(){
        assertEquals("derby''s string", NodeConfigurationManager.derbyEscape("derby's string"));
        assertEquals("derby''s test''s string", NodeConfigurationManager.derbyEscape("derby's test's string"));
    }

    private static final String basicConfigurationProperties =
        "node.id = d912338b16334b3eadb017705d569e14\n" +
        "node.enabled = true\n" +
        "node.cluster.pass = $L7C$PnYora7RKi9ToQQjDd5i1g==$HsABcaR7rub4yOHlfgtufw==\n" +
        "node.db.config.main.host = localhost\n" +
        "node.db.config.main.port = 3306\n" +
        "node.db.config.main.name = ssg\n" +
        "node.db.config.main.user = gateway\n" +
        "node.db.config.main.pass = $L7C$q\\/gW7oW59ddG3jzochGrnQ==$GaIHOAruHhv0CbheE\\/YeIQ==";

    private static final String configurationProperties =
        "node.id = d21d57cbf8fd45c183ba9131d05a883f\n" +
        "node.enabled = true\n" +
        "node.cluster.pass = $L7C$n6HLqGMJOCurOTe5jG5lig==$zBdHsxhdG9yG19NsrPq5rA==\n" +
        "node.db.clusterType = replicated\n" +
        "node.db.configs = main,failover\n" +
        "\n" +
        "node.db.config.main.host = littlefish2\n" +
        "node.db.config.main.port = 3306\n" +
        "node.db.config.main.name = ssg\n" +
        "node.db.config.main.user = gateway\n" +
        "node.db.config.main.pass = $L7C$vI8T03Cz6EcVSw4nqRT6uA==$bScWpnxtQrUi8CwPxMDTbw==\n" +
        "node.db.config.main.type = REPL_MASTER\n" +
        "\n" +
        "node.db.config.failover.inheritFrom = main\n" +
        "node.db.config.failover.type = REPL_SLAVE\n" +
        "node.db.config.failover.host = failoverhost\n" +
        "node.db.config.failover.port = 3306";
}
