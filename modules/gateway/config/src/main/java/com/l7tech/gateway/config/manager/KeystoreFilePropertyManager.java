package com.l7tech.gateway.config.manager;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for querying and setting properties in the keystore_file table in the database.
 */
public class KeystoreFilePropertyManager {
    private static final Logger logger = Logger.getLogger(KeystoreFilePropertyManager.class.getName());

    public static final int EXIT_STATUS_OK = 0;
    public static final int EXIT_STATUS_ERROR = 1;
    public static final int EXIT_STATUS_USAGE = 2;
    public static final int EXIT_STATUS_NO_SUCH_PROPERTY = 11;
    public static final int EXIT_STATUS_NO_SUCH_OBJECT_ID = 12;

    private static final String DEFAULT_CONFIG_PATH = "../node/{0}/etc/conf";
    private static final String DEFAULT_NODE = "default";

    private static final String NODE = SyspropUtil.getString("com.l7tech.config.node", DEFAULT_NODE);
    private static final String CONFIG_PATH = SyspropUtil.getString("com.l7tech.config.path", DEFAULT_CONFIG_PATH);

    private final DatabaseConfig dbConfig;

    private static class NoSuchPropertyException extends Exception {}
    private static class NoSuchObjectIdException extends Exception {}

    public static void main(final String[] argsArray) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");
        Goid goid = GoidEntity.DEFAULT_GOID;
        String action = "process";
        String propertyName = null;
        String propertyValue = null;
        try {
            LinkedList<String> args = new LinkedList<String>(Arrays.asList(argsArray));

            action = args.removeFirst();                                                        
            String id = args.removeFirst();
            goid = getGoid(id);
            propertyName = args.removeFirst();

            if (!args.isEmpty())
                propertyValue = args.removeFirst();

            if (!args.isEmpty())
                usage("Too many arguments.");
        } catch (IllegalArgumentException iae) {
            usage("ID must be a long or a goid .");
        } catch (NoSuchElementException e) {
            usage("Not enough arguments.");
        }

        try {
            KeystoreFilePropertyManager kpm = new KeystoreFilePropertyManager(getDatabaseConfig());
            if ("get".equalsIgnoreCase(action)) {
                if (propertyValue != null)
                    usage("Property value should not be provided for get.");

                propertyValue = kpm.getProperty(goid, propertyName);
                System.out.println(propertyValue);
                System.exit(EXIT_STATUS_OK);
            } else if ("set".equalsIgnoreCase(action)) {
                if (propertyValue == null)
                    usage("New property value must be provided for set.");

                kpm.setProperty(goid, propertyName, propertyValue);
                System.exit(EXIT_STATUS_OK);
            } else if ("clear".equalsIgnoreCase(action)) {
                if (propertyValue != null)
                    usage("Property value should not be provided for clear.");

                kpm.clearProperty(goid, propertyName);
                System.exit(EXIT_STATUS_OK);
            } else {
                usage("Invalid action.");
            }
        } catch (NoSuchPropertyException e) {
            fatal("No such property: " + propertyName, null, EXIT_STATUS_NO_SUCH_PROPERTY);
        } catch (NoSuchObjectIdException e) {
            fatal("No such keystore id: " + propertyName, null, EXIT_STATUS_NO_SUCH_OBJECT_ID);
        } catch (Throwable e) {
            fatal("Unable to " + action + " keystore property: " + ExceptionUtils.getMessage(e), e, EXIT_STATUS_ERROR);
        }
    }

    private static Goid getGoid(String id) {
        //try to get a goid from the input.
        //if it is a goid string just use that, otherwise assume it was oid so create a default goid using the oid
        try{
            return Goid.parseGoid(id);
        } catch(IllegalArgumentException e) {
            return new Goid(0,Long.parseLong(id));
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
        fatal(p + "Usage: KeystoreFilePropertyManager <get|set|clear> <objectid> <propertyname> [<newPropertyValueString>]", null, EXIT_STATUS_USAGE);
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
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile));
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

    public void setProperty(Goid goid, String propertyName, String propertyValueStr) throws SQLException, NoSuchObjectIdException {
        if ("databytes".equalsIgnoreCase(propertyName)) {
            throw new SQLException("set not supported for databytes property");
        }

        Map<String, String> properties = loadProperties(goid);
        properties.put(propertyName, propertyValueStr);
        saveProperties(goid, properties);
    }

    public String getProperty(Goid goid, String propertyName) throws SQLException, NoSuchPropertyException, NoSuchObjectIdException {
        if ("databytes".equalsIgnoreCase(propertyName)) {
            throw new SQLException("get not supported for databytes property");
        }

        Map<String, String> properties = loadProperties(goid);
        String result = properties.get(propertyName);
        if (result == null)
            throw new NoSuchPropertyException();
        return result;
    }

    public void clearProperty(Goid goid, String propertyName) throws SQLException, NoSuchObjectIdException {
        // Recognizes special property name: databytes
        if ("databytes".equalsIgnoreCase(propertyName)) {
            clearDataBytes(goid);
        } else {
            Map<String, String> properties = loadProperties(goid);
            properties.remove(propertyName);
            saveProperties(goid, properties);
        }
    }

    private Map<String, String> loadProperties(Goid goid) throws SQLException, NoSuchObjectIdException {
        final Map<String, String> properties;

        DBActions dbactions = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(dbConfig, false);
            statement = connection.prepareStatement("select properties from keystore_file where goid = ?");
            statement.setBytes(1, goid.getBytes());
            resultSet = statement.executeQuery();
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
                throw new NoSuchObjectIdException();
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

        return properties;
    }

    private void saveProperties(Goid goid, Map<String, String> properties) throws SQLException, NoSuchObjectIdException {
        final String xml;
        if ( properties == null )
            throw new NullPointerException("properties");
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
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
            statement = connection.prepareStatement("update keystore_file set properties = ? where goid = ?");
            statement.setString(1, xml);
            statement.setBytes(2, goid.getBytes());
            statement.execute();
            int updateCount = statement.getUpdateCount();
            if (updateCount < 1) {
                throw new NoSuchObjectIdException();
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
    }

    private void clearDataBytes(Goid goid) throws SQLException, NoSuchObjectIdException {
        DBActions dbactions = new DBActions();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dbactions.getConnection(dbConfig, false);
            statement = connection.prepareStatement("update keystore_file set databytes = null where goid = ?");
            statement.setBytes(1, goid.getBytes());
            statement.execute();
            int updateCount = statement.getUpdateCount();
            if (updateCount < 1) {
                throw new NoSuchObjectIdException();
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }
    }
}
