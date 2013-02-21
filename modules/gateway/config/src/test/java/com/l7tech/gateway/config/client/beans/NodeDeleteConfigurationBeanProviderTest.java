package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.l7tech.gateway.config.client.beans.NodeDeleteConfigurationBeanProvider.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NodeDeleteConfigurationBeanProviderTest {
    private static final String DELETED_DERBY_DATABASE_TEST_MESSAGE = "deleted derby database";
    private NodeDeleteConfigurationBeanProvider provider;
    @Mock
    private NodeManagementApiFactory factory;
    @Mock
    private NodeManagementApi manager;
    private NodeConfig config;
    private List<ConfigurationBean> configBeans;
    private List<String> testMessages;

    @Before
    public void setup() {
        config = new NodeConfig();
        testMessages = new ArrayList<String>();
        provider = new NodeDeleteConfigurationBeanProvider(factory) {
            @Override
            void deleteDerbyDatabase() throws NodeConfigurationManager.DeleteEmbeddedDatabaseException {
                // add test message (instead of actual deletion) which can be verified
                testMessages.add(DELETED_DERBY_DATABASE_TEST_MESSAGE);
            }
        };
        provider.config = config;
        configBeans = new ArrayList<ConfigurationBean>();
        when(factory.getManagementService()).thenReturn(manager);
    }

    @Test
    public void isOptionActiveDerbyDeleteNullDatabases() {
        config.setDatabases(null);
        assertTrue(provider.isOptionActive(null, createDerbyDeleteOption()));
    }

    @Test
    public void isOptionActiveDerbyDeleteEmptyDatabases() {
        config.setDatabases(Collections.<DatabaseConfig>emptyList());
        assertTrue(provider.isOptionActive(null, createDerbyDeleteOption()));
    }

    @Test
    public void isOptionActiveDerbyDeleteHasDatabase() {
        config.setDatabases(Collections.singletonList(new DatabaseConfig()));
        assertFalse(provider.isOptionActive(null, createDerbyDeleteOption()));
    }

    @Test
    public void storeConfigurationDerbyDeleteTrue() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DERBY_DELETE_OPTION, true));
        provider.storeConfiguration(configBeans);
        assertTrue(testMessages.contains(DELETED_DERBY_DATABASE_TEST_MESSAGE));
    }

    @Test
    public void storeConfigurationDerbyDeleteFalse() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DERBY_DELETE_OPTION, false));
        provider.storeConfiguration(configBeans);
        assertFalse(testMessages.contains(DELETED_DERBY_DATABASE_TEST_MESSAGE));
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationDerbyDeleteError() throws Exception {
        provider = new NodeDeleteConfigurationBeanProvider(factory) {
            @Override
            void deleteDerbyDatabase() throws NodeConfigurationManager.DeleteEmbeddedDatabaseException {
                throw new NodeConfigurationManager.DeleteEmbeddedDatabaseException("mocking exception");
            }
        };
        provider.config = config;
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DERBY_DELETE_OPTION, true));

        try {
            provider.storeConfiguration(configBeans);
            fail("expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Error deleting embedded database 'mocking exception'", e.getMessage());
            throw e;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationDerbyDeleteWithoutNodeDelete() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, false));
        configBeans.add(createConfigBean(DERBY_DELETE_OPTION, true));
        try {
            provider.storeConfiguration(configBeans);
            fail("expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Cannot delete database unless configuration is also deleted.", e.getMessage());
            throw e;
        }
    }

    @Test
    public void storeConfigurationMySqlDelete() throws Exception {
        config.setDatabases(Collections.singletonList(createDatabaseConfig()));
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        when(manager.testDatabaseConfig(any(DatabaseConfig.class))).thenReturn(true);

        provider.storeConfiguration(configBeans);
        verify(manager).testDatabaseConfig(any(DatabaseConfig.class));
        verify(manager).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));
    }

    @Test
    public void storeConfigurationMySqlDeleteNoAdminUser() throws Exception {
        config.setDatabases(Collections.singletonList(createDatabaseConfig()));
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));

        provider.storeConfiguration(configBeans);
        verify(manager, never()).testDatabaseConfig(any(DatabaseConfig.class));
        verify(manager, never()).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));
    }

    @Test
    public void storeConfigurationMySqlDeleteNoDatabases() throws Exception {
        config.setDatabases(Collections.<DatabaseConfig>emptyList());
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        when(manager.testDatabaseConfig(any(DatabaseConfig.class))).thenReturn(true);

        provider.storeConfiguration(configBeans);
        verify(manager, never()).testDatabaseConfig(any(DatabaseConfig.class));
        verify(manager, never()).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationMySqlTestDbFalse() throws Exception {
        config.setDatabases(Collections.singletonList(createDatabaseConfig()));
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        when(manager.testDatabaseConfig(any(DatabaseConfig.class))).thenReturn(false);

        try {
            provider.storeConfiguration(configBeans);
            fail("Expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Invalid database credentials.", e.getMessage());
            verify(manager, never()).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));
            throw e;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationMySqlDeleteException() throws Exception {
        config.setDatabases(Collections.singletonList(createDatabaseConfig()));
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        when(manager.testDatabaseConfig(any(DatabaseConfig.class))).thenReturn(true);
        doThrow(new NodeManagementApi.DatabaseDeletionException("mocking exception")).
                when(manager).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));

        try {
            provider.storeConfiguration(configBeans);
            fail("Expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Error deleting database 'mocking exception'", e.getMessage());
            throw e;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationMySqlDeleteWithoutNodeDelete() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, false));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        try {
            provider.storeConfiguration(configBeans);
            fail("expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Cannot delete database unless configuration is also deleted.", e.getMessage());
            throw e;
        }
    }

    @Test
    public void storeConfigurationNodeDeleteTrue() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        provider.storeConfiguration(configBeans);
        verify(manager).deleteNode(anyString(), anyInt());
    }

    @Test
    public void storeConfigurationNodeDeleteFalse() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, false));
        provider.storeConfiguration(configBeans);
        verify(manager, never()).deleteNode(anyString(), anyInt());
    }

    @Test(expected = ConfigurationException.class)
    public void storeConfigurationNodeDeleteException() throws Exception {
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        doThrow(new DeleteException("mocking exception")).when(manager).deleteNode(anyString(), anyInt());

        try {
            provider.storeConfiguration(configBeans);
            fail("Expected ConfigurationException");
        } catch (final ConfigurationException e) {
            assertEquals("Error when deleting node 'mocking exception'", e.getMessage());
            throw e;
        }
    }

    /**
     * NodeDeleteConfigurationBeanProvider.storeConfiguration supports deleting both mysql and derby dbs even if we
     * currently do not support having both database types at the same time.
     */
    @Test
    public void storeConfigurationDerbyAndMySqlDelete() throws Exception {
        config.setDatabases(Collections.singletonList(createDatabaseConfig()));
        configBeans.add(createConfigBean(NODE_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DERBY_DELETE_OPTION, true));
        configBeans.add(createConfigBean(DATABASE_ADMIN_USER_OPTION, "root"));
        when(manager.testDatabaseConfig(any(DatabaseConfig.class))).thenReturn(true);

        provider.storeConfiguration(configBeans);
        assertTrue(testMessages.contains(DELETED_DERBY_DATABASE_TEST_MESSAGE));
        verify(manager).testDatabaseConfig(any(DatabaseConfig.class));
        verify(manager).deleteDatabase(any(DatabaseConfig.class), any(Collection.class));
    }

    private ConfigurationBean createConfigBean(final String name, final Object value) {
        final ConfigurationBean config = new ConfigurationBean();
        config.setConfigName(name);
        config.setConfigValue(value);
        return config;
    }

    private Option createDerbyDeleteOption() {
        final Option derbyDelete = new Option();
        derbyDelete.setConfigName(DERBY_DELETE_OPTION);
        return derbyDelete;
    }

    private DatabaseConfig createDatabaseConfig() {
        final DatabaseConfig database = new DatabaseConfig();
        database.setType(DatabaseType.NODE_ALL);
        database.setClusterType(NodeConfig.ClusterType.STANDALONE);
        return database;
    }
}
