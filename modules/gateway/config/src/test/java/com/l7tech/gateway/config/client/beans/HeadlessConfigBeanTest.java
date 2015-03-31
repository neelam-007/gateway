package com.l7tech.gateway.config.client.beans;


import com.l7tech.config.client.ConfigurationException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.IOUtils;
import org.hamcrest.CustomMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Properties;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"unchecked", "ConstantConditions"})
@RunWith(MockitoJUnitRunner.class)
public class HeadlessConfigBeanTest {
    @Mock
    NodeManagementApiFactory nodeManagementApiFactory;
    @Mock
    NodeManagementApi nodeManagementApi;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private NodeConfigurationBeanProvider nodeConfigurationBeanProvider;

    //output stream
    private PipedInputStream outPipedInputStream;
    private PrintStream outPrintStream;

    @Before
    public void before() throws IOException, ConfigurationException {
        Mockito.when(nodeManagementApiFactory.getManagementService()).thenReturn(nodeManagementApi);

        nodeConfigurationBeanProvider = new NodeConfigurationBeanProvider(nodeManagementApiFactory);

        outPipedInputStream = new PipedInputStream();
        PipedOutputStream outConfigStream = new PipedOutputStream(outPipedInputStream);
        outPrintStream = new PrintStream(outConfigStream);
    }

    @After
    public void after() throws IOException {
    }

    @Test
    public void createDBAndConfigMysqlNoFailover() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

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
        String databaseType = "mysql";

        final Properties properties = new Properties();
        properties.setProperty("cluster.host", clusterHost);
        properties.setProperty("database.name", databaseName);
        properties.setProperty("admin.pass", pmAdminUserPassword);
        properties.setProperty("database.admin.user", databaseAdminUser);
        properties.setProperty("database.port", String.valueOf(databasePort));
        properties.setProperty("database.user", databaseUser);
        properties.setProperty("node.enable", String.valueOf(nodeEnabled));
        properties.setProperty("admin.user", pmAdminUser);
        properties.setProperty("database.admin.pass", databaseAdminPass);
        properties.setProperty("database.pass", databasePass);
        properties.setProperty("database.host", databaseHost);
        properties.setProperty("cluster.pass", clusterPass);
        properties.setProperty("database.type", databaseType);

        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("create", null, new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return properties;
            }
        });

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

        assertThat("incorrect error message: ", getOutputString(), containsString("Configuration Successful"));
    }

    @Test
    public void createDBOnlyDerbyNoFailover() throws ConfigurationException, IOException, NodeManagementApi.DatabaseCreationException, SaveException {

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
        String databaseType = "derby";

        final Properties properties = new Properties();
        properties.setProperty("cluster.host", clusterHost);
        properties.setProperty("database.name", databaseName);
        properties.setProperty("admin.pass", pmAdminUserPassword);
        properties.setProperty("database.admin.user", databaseAdminUser);
        properties.setProperty("database.port", String.valueOf(databasePort));
        properties.setProperty("database.user", databaseUser);
        properties.setProperty("node.enable", String.valueOf(nodeEnabled));
        properties.setProperty("admin.user", pmAdminUser);
        properties.setProperty("database.admin.pass", databaseAdminPass);
        properties.setProperty("database.pass", databasePass);
        properties.setProperty("cluster.pass", clusterPass);
        properties.setProperty("database.type", databaseType);
        properties.setProperty("configure.node", String.valueOf(configureNode));

        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("create", null, new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return properties;
            }
        });

        Mockito.verify(nodeManagementApi).createDatabase(
                Matchers.eq("default"),
                Matchers.isNull(DatabaseConfig.class),
                (Collection<String>) Matchers.argThat(empty()),
                Matchers.eq(pmAdminUser),
                Matchers.eq(pmAdminUserPassword),
                Matchers.eq(clusterHost));

        Mockito.verify(nodeManagementApi, new Times(0)).createNode(Matchers.<NodeConfig>any());

        assertThat("incorrect error message: ", getOutputString(), containsString("Configuration Successful"));
    }

    @Test
    public void badCommand() throws ConfigurationException {
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(containsString("Unknown command 'badCommand'"));

        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("badCommand", null, new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return new Properties();
            }
        });
    }

    @Test
    public void badSubCommand() throws ConfigurationException {
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(containsString("Unknown sub-command 'badSubCommand'"));

        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("create", "badSubCommand", new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return new Properties();
            }
        });
    }

    @Test
    public void helpCommand() throws ConfigurationException, IOException {
        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("help", null, new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return new Properties();
            }
        });

        String out = getOutputString();
        assertNotNull("The help should not be null", out);
        assertTrue("The help message should not be too short", out.length() > 100);
    }

    @Test
    public void createTemplateCommand() throws ConfigurationException, IOException {
        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("create", "template", new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return new Properties();
            }
        });

        String out = getOutputString();
        assertNotNull("The template should not be null", out);
        assertTrue("The template message should not be too short", out.length() > 100);

        Properties properties = new Properties();
        //tests that the output can be loaded as properties
        properties.load(new ByteArrayInputStream(out.getBytes(Charset.defaultCharset())));

        assertTrue("The create template is expected to contain 'node.enable'", properties.containsKey("node.enable"));
    }

    @Test
    public void badProperties() throws ConfigurationException {
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(containsString("Unable to parse option value for 'database.port' value given: 'abcd'"));

        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        String databasePort = "abcd";
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";

        final Properties properties = new Properties();
        properties.setProperty("cluster.host", clusterHost);
        properties.setProperty("database.name", databaseName);
        properties.setProperty("admin.pass", pmAdminUserPassword);
        properties.setProperty("database.admin.user", databaseAdminUser);
        properties.setProperty("database.port", String.valueOf(databasePort));
        properties.setProperty("database.user", databaseUser);
        properties.setProperty("node.enable", String.valueOf(nodeEnabled));
        properties.setProperty("admin.user", pmAdminUser);
        properties.setProperty("database.admin.pass", databaseAdminPass);
        properties.setProperty("database.pass", databasePass);
        properties.setProperty("database.host", databaseHost);
        properties.setProperty("cluster.pass", clusterPass);

        HeadlessConfigBean headlessConfigBean = new HeadlessConfigBean(nodeConfigurationBeanProvider, outPrintStream);

        headlessConfigBean.configure("create", null, new PropertiesAccessor() {
            @NotNull
            @Override
            public Properties getProperties() throws ConfigurationException {
                return properties;
            }
        });
    }

    private String getOutputString() throws IOException {
        outPrintStream.close();
        StringWriter writer = new StringWriter();
        IOUtils.copyStream(new InputStreamReader(outPipedInputStream), writer);
        return writer.toString();
    }

}
