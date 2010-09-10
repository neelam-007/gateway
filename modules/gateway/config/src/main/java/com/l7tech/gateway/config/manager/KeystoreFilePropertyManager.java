package com.l7tech.gateway.config.manager;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.*;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for querying and setting properties in the keystore_file table in the database.
 */
public class KeystoreFilePropertyManager {
    private static final Logger logger = Logger.getLogger(KeystoreFilePropertyManager.class.getName());

    public static final int EXIT_STATUS_OK = 0;
    public static final int EXIT_STATUS_ERROR = 1;
    public static final int EXIT_STATUS_NO_SUCH_PROPERTY = 11;

    private static final String DEFAULT_CONFIG_PATH = "../node/{0}/etc/conf";
    private static final String DEFAULT_NODE = "default";

    private static final String NODE = SyspropUtil.getString("com.l7tech.config.node", DEFAULT_NODE);
    private static final String CONFIG_PATH = SyspropUtil.getString("com.l7tech.config.path", DEFAULT_CONFIG_PATH);

    private final DatabaseConfig dbConfig;

    private static class NoSuchPropertyException extends Exception {}

    public static void main(final String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        String errorAction = "process";
        String propertyName = null;
        try {
            if (args.length < 3)
                usage("Not enough arguments.");
            if (args.length > 4)
                usage("Too many arguments.");

            String oid = args[0];
            String action = args[1];
            propertyName = args[2];
            String propertyValueStr = args.length > 3 ? args[3] : null;

            KeystoreFilePropertyManager kpm = new KeystoreFilePropertyManager(getDatabaseConfig());
            if ("get".equalsIgnoreCase(action)) {
                errorAction = "get";
                if (propertyValueStr != null)
                    usage("Property value should not be provided for get.");

                propertyValueStr = kpm.getProperty(Integer.parseInt(oid), propertyName);
                System.out.println(propertyValueStr);
                System.exit(0);
            } else if ("set".equalsIgnoreCase(action)) {
                errorAction = "set";
                if (propertyValueStr == null)
                    usage("New property value must be provided for set.");

                kpm.setProperty(Integer.parseInt(oid), propertyName, propertyValueStr);
                System.exit(EXIT_STATUS_OK);
            } else {
                usage("Invalid action; must be set or get.");
            }
        } catch (NoSuchPropertyException e) {
            fatal("No such property: " + propertyName, null, EXIT_STATUS_NO_SUCH_PROPERTY);
        } catch (Throwable e) {
            fatal("Unable to " + errorAction + " keystore property: " + ExceptionUtils.getMessage(e), e, EXIT_STATUS_ERROR);
        }
    }

    private static void fatal(String msg, Throwable t, int status) {
        logger.log(Level.WARNING, msg, t);
        System.err.println(msg);
        System.exit(status);
    }

    // Never returns; calls System.exit
    private static void usage(String prefix) {
        String p = prefix == null ? "" : prefix + "\n";
        fatal(p + "Usage: KeystoreFilePropertyManager <objectid> <get|set> <propertyname> [newPropertyValueString]", null, 2);
    }

    private static DatabaseConfig getDatabaseConfig() throws IOException {
        final String configurationDirPath = MessageFormat.format(CONFIG_PATH, NODE);
        File configDirectory = new File(configurationDirPath);
        File ompFile = new File(configDirectory, "omp.dat");
        if (!ompFile.exists()) {
            throw new FileNotFoundException("Node is not configured (missing obfuscated master password).");
        }

        DatabaseConfig config;
        File nodePropsFile = new File(configDirectory, "node.properties");
        if (nodePropsFile.exists()) {
            NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig(NODE, true);
            config = nodeConfig.getDatabase(DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER);
            if (config == null) {
                throw new CausedIOException("Database configuration not found.");
            }
        } else {
            throw new FileNotFoundException("Node is not configured.");
        }

        // load and decrypt DB connnection info
        final MasterPasswordManager masterPasswordManager =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        config.setNodePassword(new String(masterPasswordManager.decryptPasswordIfEncrypted(config.getNodePassword())));

        logger.info("Using database host '" + config.getHost() + "'.");
        logger.info("Using database port '" + config.getPort() + "'.");
        logger.info("Using database name '" + config.getName() + "'.");
        logger.info("Using database user '" + config.getNodeUsername() + "'.");
        return config;
    }

    public KeystoreFilePropertyManager(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void setProperty(int oid, String propertyName, String propertyValueStr) throws SQLException {
        Map<String, String> properties = loadProperties(oid);
        properties.put(propertyName, propertyValueStr);
        saveProperties(oid, properties);
    }

    public String getProperty(int oid, String propertyName) throws SQLException, NoSuchPropertyException {
        Map<String, String> properties = loadProperties(oid);
        String result = properties.get(propertyName);
        if (result == null)
            throw new NoSuchPropertyException();
        return result;
    }

    private Map<String, String> loadProperties(int oid) throws SQLException {
        final Map<String, String> properties;

        DBActions dbactions = new DBActions();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(dbConfig, false);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select properties from keystore_file where objectid = " + oid);
            if (resultSet.next()) {
                String xml = resultSet.getString(1);
                if ( xml != null && xml.length() > 0 ) {
                    XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)));
                    //noinspection unchecked
                    properties = (Map<String, String>)xd.readObject();
                } else {
                    properties = new HashMap<String, String>();
                }
            } else {
                throw new SQLException("No keystore_file found with objectid " + oid);
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

        return properties;
    }

    private void saveProperties(int oid, Map<String, String> properties) throws SQLException {
        final String xml;
        if ( properties == null )
            throw new NullPointerException("properties");
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
        try {
            XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
            xe.writeObject(properties);
            xe.close();
            xml = baos.toString(Charsets.UTF8);
        } finally {
            baos.close();
        }

        DBActions dbactions = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(dbConfig, false);
            statement = connection.prepareStatement("update keystore_file set properties = ? where objectid = " + oid);
            statement.setString(1, xml);
            statement.execute();
            int updateCount = statement.getUpdateCount();
            if (updateCount < 1) {
                throw new SQLException("No keystore_file found with objectid " + oid);
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
    }
}
