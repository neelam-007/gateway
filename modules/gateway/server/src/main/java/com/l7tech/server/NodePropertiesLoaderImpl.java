package com.l7tech.server;

import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;

import java.io.*;
import java.util.Properties;

/**
 * This is a singleton that consults the disklessConfig system property on startup, and if it is defined and has
 * a value of true then it loads node properties from environment variables. The names of these environment variables
 * and the corresponding node properties are defined in the nodePropsEnvVarDefs.properties file, and are based on the
 * variable names already for our Docker release. If disklessConfig is undefined or is set to false then the node
 * properties will be read from the standard node.properties file.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class NodePropertiesLoaderImpl implements NodePropertiesLoader {
    private static final String NODE_PROPERTIES_FILE_NAME = "node.properties";
    private static final String ENV_VAR_DEFINITIONS_FILE = "resources/nodePropsEnvVarDefs.properties";

    private static final String NODE_ID = "node.id";
    private static final String NODE_ENABLED = "node.enabled";
    private static final String NODE_JAVA_PATH = "node.java.path";
    private static final String NODE_JAVA_HEAP = "node.java.heap";
    private static final String NODE_CLUSTER_PASS = "node.cluster.pass";
    private static final String NODE_DB_TYPE = "node.db.type";
    private static final String NODE_DB_CONFIG_MAIN_NAME = "node.db.config.main.name";
    private static final String NODE_DB_CONFIG_MAIN_HOST = "node.db.config.main.host";
    private static final String NODE_DB_CONFIG_MAIN_PORT = "node.db.config.main.port";
    private static final String NODE_DB_CONFIG_MAIN_USER = "node.db.config.main.user";
    private static final String NODE_DB_CONFIG_MAIN_PASS = "node.db.config.main.pass";

    private static NodePropertiesLoader INSTANCE = new NodePropertiesLoaderImpl();

    private Properties properties = new Properties();

    private final boolean diskless;

    private NodePropertiesLoaderImpl() {
        this.diskless = SyspropUtil.getBoolean("disklessConfig", false);

        loadNodeProperties();
    }

    private void loadNodeProperties() {
        if (diskless) {
            Properties envVarDefs = new Properties();

            try {
                envVarDefs.load(NodePropertiesLoaderImpl.class.getResourceAsStream(ENV_VAR_DEFINITIONS_FILE));
            } catch (IOException e) {
                throw new IllegalStateException("Could not load node properties environment variable definitions");
            }

            String idVar = envVarDefs.getProperty(NODE_ID);
            String enabledVar = envVarDefs.getProperty(NODE_ENABLED);
            String javaPathVar = envVarDefs.getProperty(NODE_JAVA_PATH);
            String javaHeapVar = envVarDefs.getProperty(NODE_JAVA_HEAP);
            String clusterPassVar = envVarDefs.getProperty(NODE_CLUSTER_PASS);
            String dbTypeVar = envVarDefs.getProperty(NODE_DB_TYPE);
            String dbConfigMainNameVar = envVarDefs.getProperty(NODE_DB_CONFIG_MAIN_NAME);
            String dbConfigMainHostVar = envVarDefs.getProperty(NODE_DB_CONFIG_MAIN_HOST);
            String dbConfigMainPortVar = envVarDefs.getProperty(NODE_DB_CONFIG_MAIN_PORT);
            String dbConfigMainUserVar = envVarDefs.getProperty(NODE_DB_CONFIG_MAIN_USER);
            String dbConfigMainPassVar = envVarDefs.getProperty(NODE_DB_CONFIG_MAIN_PASS);

            String nodeId = System.getenv(idVar);

            /**
             * When using a node.properties file a generated a node id is added to the properties file if one is
             * not present, which is then used on subsequent gateway restarts.
             * If we are operating in diskless config mode then a node id must be specified via an environment variable
             * otherwise a new id will be generated on each gateway startup and stored to the database in the cluster
             * node info table, leading to an ever-increasing number of invalid entries.
             */
            if (null == nodeId || nodeId.trim().isEmpty()) {
                throw new IllegalStateException("A node id must be specified");
            }

            properties.setProperty(NODE_ID, nodeId);
            properties.setProperty(NODE_ENABLED, System.getenv(enabledVar));
            properties.setProperty(NODE_JAVA_PATH, System.getenv(javaPathVar));
            properties.setProperty(NODE_JAVA_HEAP, System.getenv(javaHeapVar));
            properties.setProperty(NODE_CLUSTER_PASS, System.getenv(clusterPassVar));
            properties.setProperty(NODE_DB_TYPE, System.getenv(dbTypeVar));
            properties.setProperty(NODE_DB_CONFIG_MAIN_NAME, System.getenv(dbConfigMainNameVar));
            properties.setProperty(NODE_DB_CONFIG_MAIN_HOST, System.getenv(dbConfigMainHostVar));
            properties.setProperty(NODE_DB_CONFIG_MAIN_PORT, System.getenv(dbConfigMainPortVar));
            properties.setProperty(NODE_DB_CONFIG_MAIN_USER, System.getenv(dbConfigMainUserVar));
            properties.setProperty(NODE_DB_CONFIG_MAIN_PASS, System.getenv(dbConfigMainPassVar));
        } else {
            final File configDir = ServerConfig.getInstance()
                    .getLocalDirectoryProperty(ServerConfigParams.PARAM_CONFIG_DIRECTORY, false);

            File nodePropertiesFile = new File(configDir, NODE_PROPERTIES_FILE_NAME);

            if (!nodePropertiesFile.exists() || !nodePropertiesFile.isFile()) {
                throw new IllegalStateException("invalid node properties file: " + nodePropertiesFile);
            }

            try {
                properties = IOUtils.loadProperties(nodePropertiesFile);
            } catch (IOException e) {
                throw new IllegalStateException("Error accessing node properties", e);
            }
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static NodePropertiesLoader getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isDiskless() {
        return diskless;
    }
}
