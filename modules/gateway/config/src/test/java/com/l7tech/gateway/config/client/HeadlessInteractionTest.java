package com.l7tech.gateway.config.client;


import com.l7tech.config.client.ConfigurationClient;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.gateway.config.client.Main;
import com.l7tech.gateway.config.client.beans.NodeConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.NodeManagementApiFactory;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.IOUtils;
import org.hamcrest.CustomMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings({"ConstantConditions", "unchecked"})
@RunWith(MockitoJUnitRunner.class)
public class HeadlessInteractionTest {

    @Mock
    NodeManagementApiFactory nodeManagementApiFactory;
    @Mock
    NodeManagementApi nodeManagementApi;


    private InputStream oldIn;

    private PipedOutputStream configStream;
    private PipedInputStream pipedInputStream;
    private PrintWriter printWriter;
    private OptionSet fullGatewayConfigurationOptionSet;
    private NodeConfigurationBeanProvider nodeConfigurationBeanProvider;
    private PrintStream oldOut;
    private PipedInputStream outPipedInputStream;
    private PipedOutputStream outConfigStream;
    private PrintStream outPrintStream;
    private PrintStream oldErr;
    private PipedInputStream errPipedInputStream;
    private PipedOutputStream errConfigStream;
    private PrintStream errPrintStream;

    @Before
    public void before() throws IOException, ConfigurationException {
        Mockito.when(nodeManagementApiFactory.getManagementService()).thenReturn(nodeManagementApi);

        fullGatewayConfigurationOptionSet = ConfigurationFactory.newConfiguration(Main.class, "configTemplates/GatewayApplianceConfiguration.xml");

        nodeConfigurationBeanProvider = new NodeConfigurationBeanProvider(nodeManagementApiFactory);

        oldIn = System.in;
        oldOut = System.out;
        oldErr = System.err;

        pipedInputStream = new PipedInputStream();
        configStream = new PipedOutputStream(pipedInputStream);

        printWriter = new PrintWriter(configStream);

        System.setIn(pipedInputStream);

        outPipedInputStream = new PipedInputStream();
        outConfigStream = new PipedOutputStream(outPipedInputStream);
        outPrintStream = new PrintStream(outConfigStream);
        System.setOut(outPrintStream);

        errPipedInputStream = new PipedInputStream();
        errConfigStream = new PipedOutputStream(errPipedInputStream);
        errPrintStream = new PrintStream(errConfigStream);
        System.setErr(errPrintStream);
    }

    @After
    public void after() throws IOException {
        System.setIn(oldIn);

        printWriter.close();
        configStream.close();
        pipedInputStream.close();

        System.setOut(oldOut);

        outPrintStream.close();
        outConfigStream.close();
        outPipedInputStream.close();

        System.setErr(oldErr);

        errPrintStream.close();
        errConfigStream.close();
        errPipedInputStream.close();
    }

    @Test
    public void createDBAndConfigMysqlNoFailover() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";
        printWriter.println("create-db\n" +
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=" + clusterHost + "\n" +
                "database.failover.port=" + databaseFailoverPort + "\n" +
                "database.name=" + databaseName + "\n" +
                "admin.pass=" + pmAdminUserPassword + "\n" +
                "database.admin.user=" + databaseAdminUser + "\n" +
                "database.port=" + databasePort + "\n" +
                "database.user=" + databaseUser + "\n" +
                "node.enable=" + nodeEnabled + "\n" +
                "admin.user=" + pmAdminUser + "\n" +
                "database.admin.pass=" + databaseAdminPass + "\n" +
                "database.failover.host=" + databaseFailoverHost + "\n" +
                "database.pass=" + databasePass + "\n" +
                "database.host=" + databaseHost + "\n" +
                "cluster.pass=" + clusterPass + "\n" +
                ".");
        printWriter.flush();

        boolean success = configurationClient.doInteraction();
        assertTrue("running configuration client was not successful", success);

        DatabaseConfig expectedDatabaseConfig = new DatabaseConfig(databaseHost, databasePort, databaseName, databaseUser, databasePass);
        expectedDatabaseConfig.setClusterType(NodeConfig.ClusterType.STANDALONE);
        expectedDatabaseConfig.setVendor(DatabaseConfig.Vendor.MYSQL);
        expectedDatabaseConfig.setType(DatabaseType.NODE_ALL);
        expectedDatabaseConfig.setDatabaseAdminUsername(databaseAdminUser);
        expectedDatabaseConfig.setDatabaseAdminPassword(databaseAdminPass);
        Mockito.verify(nodeManagementApi).createDatabase(
                Matchers.eq("default"),
                Matchers.eq(expectedDatabaseConfig),
                (Collection<String>) Matchers.argThat(contains(databaseHost)),
                Matchers.eq(pmAdminUser),
                Matchers.eq(pmAdminUserPassword),
                Matchers.eq(clusterHost));

        Mockito.verify(nodeManagementApi).createNode(Matchers.argThat(new CustomMatcher<NodeConfig>("Node config matches.") {
            @Override
            public boolean matches(Object o) {
                if(! (o instanceof NodeConfig)) {
                    return false;
                }
                NodeConfig nodeConfig = (NodeConfig)o;
                return nodeEnabled.equals(nodeConfig.isEnabled()) &&
                        clusterHost.equals(nodeConfig.getClusterHostname()) &&
                        clusterPass.equals(nodeConfig.getClusterPassphrase());
            }
        }));
    }

    @Test
    public void getHelpContainsConfigDBOnlyOption() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        printWriter.println("help\n");
        printWriter.flush();

        boolean success = configurationClient.doInteraction();
        assertFalse("running configuration client was successful", success);

        Mockito.verify(nodeManagementApi, new Times(0)).createDatabase(Matchers.anyString(), Matchers.<DatabaseConfig>any(), Matchers.<Collection<String>>any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());

        Mockito.verify(nodeManagementApi, new Times(0)).createNode(Matchers.<NodeConfig>any());

        outConfigStream.close();

        StringWriter writer = new StringWriter();
        IOUtils.copyStream(new InputStreamReader(outPipedInputStream), writer);
        String outMessage = writer.toString();

        assertNotNull("help message should not be null", outMessage);
        assertThat("help message does not contain configure node option", outMessage, containsString("configure.node"));
    }

    @Test
    public void badOptionValue() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        printWriter.println("create-db\n" +
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=test.cluster.hostname\n" +
                "database.failover.port=null\n" +
                "database.name=ssg\n" +
                "admin.pass=7layer\n" +
                "database.admin.user=root\n" +
                "database.port=blah\n" +
                "database.user=gateway\n" +
                "node.enable=true\n" +
                "admin.user=admin\n" +
                "database.admin.pass=7layer\n" +
                "database.failover.host=null\n" +
                "database.pass=7layer\n" +
                "database.host=localhost\n" +
                "cluster.pass=7layer\n" +
                ".");
        printWriter.flush();

        boolean success = configurationClient.doInteraction();
        assertFalse("running configuration client was successful", success);

        errConfigStream.close();

        StringWriter writer = new StringWriter();
        IOUtils.copyStream(new InputStreamReader(errPipedInputStream), writer);
        String errorMessage = writer.toString();

        assertNotNull("error message should not be null", errorMessage);
        assertThat("error message does not contain correct variable", errorMessage, containsString("database.port"));
    }

    @Test
    public void handleDbException() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";
        printWriter.println("create-db\n" +
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=" + clusterHost + "\n" +
                "database.failover.port=" + databaseFailoverPort + "\n" +
                "database.name=" + databaseName + "\n" +
                "admin.pass=" + pmAdminUserPassword + "\n" +
                "database.admin.user=" + databaseAdminUser + "\n" +
                "database.port=" + databasePort + "\n" +
                "database.user=" + databaseUser + "\n" +
                "node.enable=" + nodeEnabled + "\n" +
                "admin.user=" + pmAdminUser + "\n" +
                "database.admin.pass=" + databaseAdminPass + "\n" +
                "database.failover.host=" + databaseFailoverHost + "\n" +
                "database.pass=" + databasePass + "\n" +
                "database.host=" + databaseHost + "\n" +
                "cluster.pass=" + clusterPass + "\n" +
                ".");
        printWriter.flush();

        Mockito.doThrow(NodeManagementApi.DatabaseCreationException.class).when(nodeManagementApi).createDatabase(Matchers.anyString(), Matchers.<DatabaseConfig>any(), Matchers.<Collection<String>>any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());

        boolean success = configurationClient.doInteraction();
        assertFalse("running configuration client was successful", success);

        errConfigStream.close();

        StringWriter writer = new StringWriter();
        IOUtils.copyStream(new InputStreamReader(errPipedInputStream), writer);
        String errorMessage = writer.toString();

        assertNotNull("error message should not be null", errorMessage);
        assertThat("error message does not contain correct variable", errorMessage, containsString("Error creating database when saving configuration"));
    }

    @Test
    public void createDBOnlyMysqlNoFailover() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";
        final Boolean configureNode = false;

        printWriter.println("create-db\n" +
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=" + clusterHost + "\n" +
                "database.failover.port=" + databaseFailoverPort + "\n" +
                "database.name=" + databaseName + "\n" +
                "admin.pass=" + pmAdminUserPassword + "\n" +
                "database.admin.user=" + databaseAdminUser + "\n" +
                "database.port=" + databasePort + "\n" +
                "database.user=" + databaseUser + "\n" +
                "node.enable=" + nodeEnabled + "\n" +
                "admin.user=" + pmAdminUser + "\n" +
                "database.admin.pass=" + databaseAdminPass + "\n" +
                "database.failover.host=" + databaseFailoverHost + "\n" +
                "database.pass=" + databasePass + "\n" +
                "database.host=" + databaseHost + "\n" +
                "cluster.pass=" + clusterPass + "\n" +
                "configure.node=" + configureNode + "\n" +
                ".");
        printWriter.flush();

        boolean success = configurationClient.doInteraction();
        assertTrue("running configuration client was not successful", success);

        DatabaseConfig expectedDatabaseConfig = new DatabaseConfig(databaseHost, databasePort, databaseName, databaseUser, databasePass);
        expectedDatabaseConfig.setClusterType(NodeConfig.ClusterType.STANDALONE);
        expectedDatabaseConfig.setVendor(DatabaseConfig.Vendor.MYSQL);
        expectedDatabaseConfig.setType(DatabaseType.NODE_ALL);
        expectedDatabaseConfig.setDatabaseAdminUsername(databaseAdminUser);
        expectedDatabaseConfig.setDatabaseAdminPassword(databaseAdminPass);
        Mockito.verify(nodeManagementApi).createDatabase(
                Matchers.eq("default"),
                Matchers.eq(expectedDatabaseConfig),
                (Collection<String>) Matchers.argThat(contains(databaseHost)),
                Matchers.eq(pmAdminUser),
                Matchers.eq(pmAdminUserPassword),
                Matchers.eq(clusterHost));

        Mockito.verify(nodeManagementApi, new Times(0)).createNode(Matchers.<NodeConfig>any());
    }

    @Test
    public void createDBOnlyDerbyNoFailover() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

        ConfigurationClient configurationClient = new ConfigurationClient(nodeConfigurationBeanProvider, fullGatewayConfigurationOptionSet, "headless");

        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = null;
        final String clusterPass = "clusterPass";
        final Boolean configureNode = false;

        printWriter.println("create-db\n" +
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=" + clusterHost + "\n" +
                "database.failover.port=" + databaseFailoverPort + "\n" +
                "database.name=" + databaseName + "\n" +
                "admin.pass=" + pmAdminUserPassword + "\n" +
                "database.admin.user=" + databaseAdminUser + "\n" +
                "database.port=" + databasePort + "\n" +
                "database.user=" + databaseUser + "\n" +
                "node.enable=" + nodeEnabled + "\n" +
                "admin.user=" + pmAdminUser + "\n" +
                "database.admin.pass=" + databaseAdminPass + "\n" +
                "database.failover.host=" + databaseFailoverHost + "\n" +
                "database.pass=" + databasePass + "\n" +
                "database.host=" + databaseHost + "\n" +
                "cluster.pass=" + clusterPass + "\n" +
                "configure.node=" + configureNode + "\n" +
                ".");
        printWriter.flush();

        boolean success = configurationClient.doInteraction();
        assertTrue("running configuration client was not successful", success);

        Mockito.verify(nodeManagementApi).createDatabase(
                Matchers.eq("default"),
                Matchers.isNull(DatabaseConfig.class),
                (Collection<String>) Matchers.argThat(empty()),
                Matchers.eq(pmAdminUser),
                Matchers.eq(pmAdminUserPassword),
                Matchers.eq(clusterHost));

        Mockito.verify(nodeManagementApi, new Times(0)).createNode(Matchers.<NodeConfig>any());
    }
}
