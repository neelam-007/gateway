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

    private static NodePropertiesLoader INSTANCE = null;

    private Properties properties = new Properties();

    private final boolean diskless;

    private NodePropertiesLoaderImpl() {
        this.diskless = SyspropUtil.getBoolean("disklessConfig", false);

        loadNodeProperties();
    }

    private void loadNodeProperties() {
        if (diskless) {
            Properties envVarDefs = new Properties();

            // load corresponding environment variable names for node properties
            try {
                envVarDefs.load(NodePropertiesLoaderImpl.class.getResourceAsStream(ENV_VAR_DEFINITIONS_FILE));
            } catch (IOException e) {
                throw new IllegalStateException("Could not load node properties environment variable definitions");
            }

            // add all node properties from environment variables
            for (String nodePropertyName : envVarDefs.stringPropertyNames()) {
                String environmentVariable = envVarDefs.getProperty(nodePropertyName);

                String nodePropertyValue = System.getenv(environmentVariable);

                if (null == nodePropertyValue) {
                    throw new IllegalStateException("The '" + environmentVariable +
                            "' environment variable must be defined for the '" + nodePropertyName +
                            "' node property when using diskless config mode");
                } else {
                    properties.setProperty(nodePropertyName, nodePropertyValue);
                }
            }
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
        if (null == INSTANCE) {
            synchronized (NodePropertiesLoaderImpl.class) {
                if (null == INSTANCE) {
                    INSTANCE = new NodePropertiesLoaderImpl();
                }
            }
        }

        return INSTANCE;
    }

    @Override
    public boolean isDiskless() {
        return diskless;
    }
}
