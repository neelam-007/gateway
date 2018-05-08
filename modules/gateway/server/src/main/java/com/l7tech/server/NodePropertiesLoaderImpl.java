package com.l7tech.server;

import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Map.Entry;

/**
 * This is a singleton that consults the com.l7tech.disklessConfig system property on startup, and if it is defined and
 * has a value of true then it loads node properties from environment variables. The names of these environment
 * variables and the corresponding node properties and their default values (where applicable) are defined in the
 * nodePropsEnvVarDefs.properties file, and are based on the variable names already for our Docker release.
 * If com.l7tech.disklessConfig is undefined or is set to false then the node properties will be read from the standard
 * node.properties file.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class NodePropertiesLoaderImpl implements NodePropertiesLoader {
    private static final Logger logger = Logger.getLogger(NodePropertiesLoaderImpl.class.getName());

    private static final String DISKLESS_CONFIG_SYSTEM_PROP = "com.l7tech.disklessConfig";
    private static final String NODE_PROPERTIES_FILE_NAME = "node.properties";
    private static final String ENV_VAR_DEFINITIONS_FILE = "resources/nodePropsEnvVarDefs.properties";
    private static final String VARIABLE_SUFFIX = "variable";
    private static final String DEFAULT_SUFFIX = "default";
    private static final String OPTIONAL_SUFFIX = "optional";
    private static final String GENERATE_SPECIAL_DEFAULT = "GENERATE";

    private static NodePropertiesLoader INSTANCE = null;

    private Properties properties = new Properties();

    private final boolean diskless;

    private NodePropertiesLoaderImpl() {
        this.diskless = SyspropUtil.getBoolean(DISKLESS_CONFIG_SYSTEM_PROP, false);

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

            HashMap<String,String> variables = new HashMap<>();
            HashMap<String,String> defaults = new HashMap<>();
            HashMap<String,String> optionals = new HashMap<>();

            // add all node properties from environment variables
            for (String propertyName : envVarDefs.stringPropertyNames()) {
                int suffixSeparatorIndex = propertyName.lastIndexOf('.');

                String nodePropertyName = propertyName.substring(0, suffixSeparatorIndex);
                String suffix = propertyName.substring(suffixSeparatorIndex + 1);
                String value = envVarDefs.getProperty(propertyName);

                switch (suffix) {
                    case VARIABLE_SUFFIX:
                        variables.put(nodePropertyName, value);
                        break;
                    case DEFAULT_SUFFIX:
                        defaults.put(nodePropertyName, value);
                        break;
                    case OPTIONAL_SUFFIX:
                        optionals.put(nodePropertyName, value);
                    default:
                        logger.fine("Unrecognized node property definition: " + propertyName);
                        break;
                }
            }

            for (Entry<String, String> variableDef : variables.entrySet()) {
                String environmentVariableValue = System.getenv(variableDef.getValue());

                // no environment variable defined or value empty
                if (StringUtils.isBlank(environmentVariableValue)) {
                    // if there's a default, use it
                    if (defaults.containsKey(variableDef.getKey())) {
                        String defaultValue = defaults.get(variableDef.getKey());

                        // do special handling for properties that should have generated defaults (i.e. node.id)
                        if (GENERATE_SPECIAL_DEFAULT.equals(defaultValue)) {
                            defaultValue = generateRandomNodeId();
                            logger.fine("Generated value for ' " + variableDef.getKey() + "': " + defaultValue);
                        } else {
                            logger.fine("Using default value for '" + variableDef.getKey() + "': " + defaultValue);
                        }

                        properties.setProperty(variableDef.getKey(), defaultValue);
                    } else if(!Boolean.parseBoolean(optionals.get(variableDef.getKey()))) {
                        // otherwise if this is not an optional property and no value is provided
                        // this is a required property and we can't start
                        throw new IllegalStateException("The '" + variableDef.getValue() +
                                "' environment variable must be defined for the '" + variableDef.getKey() +
                                "' node property when using diskless config mode");
                    }
                } else { // use the value set to the environment variable
                    properties.setProperty(variableDef.getKey(), environmentVariableValue);
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

    private String generateRandomNodeId() {
        byte[] nodeIdBytes = new byte[32];
        new SecureRandom().nextBytes(nodeIdBytes);
        return HexUtils.encodeBase64(nodeIdBytes, true).substring(0,32);
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
