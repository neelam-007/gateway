package com.l7tech.gateway.config.manager;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.db.ClusterPropertyUtil;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages configuration for a node.
 */
public class NodeConfigurationManager {

    private static final Logger logger = Logger.getLogger(NodeConfigurationManager.class.getName());

    private static final File gatewayDir = new File(SyspropUtil.getString("com.l7tech.gateway.home","/opt/SecureSpan/Gateway"));
    private static final File nodesDir = new File(gatewayDir, "node");
    private static final String sqlPath = "../config/etc/sql/ssg.sql";
    private static final String configPath = "{0}/etc/conf";

    private static final String NODE_PROPS_FILE = "node.properties";

    private static final String NODEPROPERTIES_ID = "node.id";
    private static final String NODEPROPERTIES_ENABLED = "node.enabled";
    private static final String NODEPROPERTIES_DB_CLUSTYPE = "node.db.clusterType";
    private static final String NODEPROPERTIES_DB_CONFIGS = "node.db.configs";
    private static final String NODEPROPERTIES_CLUSTPROP = "node.cluster.pass";
    private static final String NODEPROPERTIES_DB_INHE_FORMAT = "node.db.config.{0}.inheritFrom";
    private static final String NODEPROPERTIES_DB_HOST_FORMAT = "node.db.config.{0}.host";
    private static final String NODEPROPERTIES_DB_PORT_FORMAT = "node.db.config.{0}.port";
    private static final String NODEPROPERTIES_DB_NAME_FORMAT = "node.db.config.{0}.name";
    private static final String NODEPROPERTIES_DB_USER_FORMAT = "node.db.config.{0}.user";
    private static final String NODEPROPERTIES_DB_PASS_FORMAT = "node.db.config.{0}.pass";
    private static final String NODEPROPERTIES_DB_TYPE_FORMAT = "node.db.config.{0}.type";

    private static final String CLUSTER_PROP_CLUSTERHOSTNAME = "cluster.hostname";

    private static final DBActions dbActions = new DBActions();

    public static final class NodeConfigurationException extends Exception {
        public NodeConfigurationException( final String message ) {
            super(message);
        }

        public NodeConfigurationException( final String message, final Throwable cause ) {
            super(message, cause);
        }
    }

    /**
     * Configure a gateway node properties and validate database config if required..
     *
     * @param name The name of the node to configure ("default")
     * @param nodeid The unique identifier to use for the node.
     * @param enabled Is the node enabled?
     * @param clusterPassword The cluster password
     * @param databaseConfig The database configuration to use.
     * @throws IOException If an error occurs
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static void configureGatewayNode( final String name,
                                             final String nodeid,
                                             final Boolean enabled,
                                             final String clusterPassword,
                                             final DatabaseConfig databaseConfig,
                                             final DatabaseConfig database2ndConfig ) throws IOException, NodeConfigurationException {
        String nodeName = name;
        if ( nodeName == null ) {
            nodeName = "default";
        }

        File configDirectory = getConfigurationDirectory(name);

        logger.log( Level.INFO, "Configuring node in directory ''{0}''.", configDirectory.getAbsolutePath());
        if ( !configDirectory.isDirectory() ) {
            throw new FileNotFoundException( "Missing configuration directory '" + configDirectory.getAbsolutePath() + "'." );
        }

        // Validate DB
        if ( databaseConfig != null && databaseConfig.getNodePassword() != null ) {
            if ( databaseConfig.getHost() == null ) throw new CausedIOException("Database host is required.");
            if ( databaseConfig.getPort() == 0 ) throw new CausedIOException("Database port is required.");
            if ( databaseConfig.getName() == null ) throw new CausedIOException("Database name is required.");
            if ( databaseConfig.getNodeUsername() == null ) throw new CausedIOException("Database username is required.");

            String dbVersion = dbActions.checkDbVersion( databaseConfig );
            if ( dbVersion != null && !dbVersion.equals(BuildInfo.getFormalProductVersion()) ) {
                throw new NodeConfigurationException("Database version mismatch '"+dbVersion+"'.");
            } else if ( dbVersion == null ) {
                throw new NodeConfigurationException("Cannot connect to database.");
            }
        }

        // Update config
        File nodeProperties = new File( configDirectory, NODE_PROPS_FILE );

        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setAutoSave(false);
        props.setListDelimiter((char)0);

        // Read the existing properties (if any)
        Boolean setEnabled = enabled;
        if ( nodeProperties.exists() ) {
            logger.log( Level.INFO, "Loading node configuration from ''{0}''.", nodeProperties.getAbsolutePath());
            FileInputStream origFis = null;
            try {
                origFis = new FileInputStream( nodeProperties );
                props.load(origFis);
            } catch (ConfigurationException ce) {
                throw new CausedIOException("Error reading properties file '"+nodeProperties.getAbsolutePath()+"'.", ce);
            } catch (FileNotFoundException e) {
                throw new CausedIOException("Error reading properties file '"+nodeProperties.getAbsolutePath()+"'.", e);
            } finally {
                ResourceUtils.closeQuietly(origFis);
            }
        } else {
            // validate that we have enough settings to create a valid configuration
            if ( nodeid == null || clusterPassword == null || databaseConfig == null ) {
                throw new NodeConfigurationException("Missing configuration parameters, cannot create new configuration for node '"+nodeName+"'.");
            }

            if ( setEnabled == null ) setEnabled = true;
        }

        MasterPasswordManager mpm = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(configDirectory, "omp.dat") ) );
        String encDatabasePassword = databaseConfig==null || databaseConfig.getNodePassword()==null ? null : mpm.encryptPassword( databaseConfig.getNodePassword().toCharArray() );
        String encClusterPassword = clusterPassword==null ? null : mpm.encryptPassword( clusterPassword.toCharArray() );

        setPropertyIfNotNull( props, NODEPROPERTIES_ID, nodeid );
        setPropertyIfNotNull( props, NODEPROPERTIES_ENABLED, setEnabled );
        setPropertyIfNotNull( props, NODEPROPERTIES_CLUSTPROP, encClusterPassword );
        if ( databaseConfig != null && databaseConfig.getNodePassword() != null ) {
            setPropertyIfNotNull( props, "node.db.config.main.host", databaseConfig.getHost() );
            setPropertyIfNotNull( props, "node.db.config.main.port", Integer.toString(databaseConfig.getPort()) );
            setPropertyIfNotNull( props, "node.db.config.main.name", databaseConfig.getName() );
            setPropertyIfNotNull( props, "node.db.config.main.user", databaseConfig.getNodeUsername() );
            setPropertyIfNotNull( props, "node.db.config.main.pass", encDatabasePassword );

            if ( database2ndConfig != null ) {
                props.setProperty(NODEPROPERTIES_DB_CLUSTYPE, "replicated" );
                props.setProperty(NODEPROPERTIES_DB_CONFIGS, "main,failover");
                props.setProperty("node.db.config.main.type", NodeConfig.ClusterType.REPL_MASTER);
                props.setProperty("node.db.config.failover.inheritFrom", "main");
                props.setProperty("node.db.config.failover.type", NodeConfig.ClusterType.REPL_SLAVE);
                setPropertyIfNotNull( props, "node.db.config.failover.host", database2ndConfig.getHost() );
                setPropertyIfNotNull( props, "node.db.config.failover.port", Integer.toString(database2ndConfig.getPort()) );
            } else {
                props.clearProperty(NODEPROPERTIES_DB_CLUSTYPE);
                props.clearProperty(NODEPROPERTIES_DB_CONFIGS);
                props.clearProperty("node.db.config.main.type");
                props.clearProperty("node.db.config.failover.inheritFrom");
                props.clearProperty("node.db.config.failover.host");
                props.clearProperty("node.db.config.failover.port");
                props.clearProperty("node.db.config.failover.type");
            }
        }

        FileOutputStream origFos = null;
        try {
            origFos = new FileOutputStream( nodeProperties );
            props.save(origFos);
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error writing properties file '"+nodeProperties.getAbsolutePath()+"'.", ce);
        } catch (FileNotFoundException e) {
            throw new CausedIOException("Error writing properties file '"+nodeProperties.getAbsolutePath()+"'.", e);
        } finally {
            ResourceUtils.closeQuietly(origFos);
        }
    }

    /**
     * Create a new database.
     *
     * @param nodeName The node the database will be used by
     * @param databaseConfig The configuration for the database to be created.
     * @param extraGrantHosts Additional hostnames from which access to the database should be granted
     * @param adminLogin The SSM admin account username
     * @param adminPassword The SSM admin account password
     * @param clusterHostname the external hostname for the cluster
     */
    public static void createDatabase( final String nodeName,
                                       final DatabaseConfig databaseConfig,
                                       final Collection<String> extraGrantHosts,
                                       final String adminLogin,
                                       final String adminPassword,
                                       final String clusterHostname )
        throws IOException {
        final DatabaseConfig localConfig;
        try {
            // If the host is localhost then use that when connecting
            if ( NetworkInterface.getByInetAddress(InetAddress.getByName(databaseConfig.getHost())) != null ) {
                localConfig = new DatabaseConfig(databaseConfig);
                localConfig.setHost("localhost");
            } else {
                localConfig = databaseConfig;
            }
        } catch ( UnknownHostException uhe ) {
            throw new CausedIOException("Could not resolve host '"+databaseConfig.getHost()+"'.");
        }

        Set<String> hosts = new HashSet<String>();
        hosts.add( databaseConfig.getHost() );
        if (extraGrantHosts != null) hosts.addAll(extraGrantHosts);

        String pathToSqlScript = MessageFormat.format( sqlPath, nodeName );

        DBActions.DBActionsResult res = dbActions.createDb(localConfig, hosts, new File(nodesDir,pathToSqlScript).getAbsolutePath(), false);
        if ( res.getStatus() != DBActions.StatusType.SUCCESS ) {
            throw new CausedIOException(MessageFormat.format("Cannot create database: ''{1}'' [code:{0}]", res.getStatus().getCode(), res.getErrorMessage()), res.getThrown());
        }

        AccountReset.resetAccount(databaseConfig, adminLogin, adminPassword);
        if ( clusterHostname != null ) {
            ClusterPropertyUtil.addClusterProperty( dbActions, databaseConfig, CLUSTER_PROP_CLUSTERHOSTNAME, clusterHostname );
        }
    }

    public static NodeConfig loadNodeConfig( final String name, final boolean loadSecrets ) throws IOException {
        return loadNodeConfig( name, new File(getConfigurationDirectory(name), NODE_PROPS_FILE), loadSecrets );
    }

    public static Collection<Pair<NodeConfig, File>> loadNodeConfigs( final boolean throwOnError ) throws IOException {
        Collection<Pair<NodeConfig, File>> nodeConfigs = new ArrayList<Pair<NodeConfig, File>>();

        File nodeBaseDirectory = nodesDir;
        String[] nodeNames = nodeBaseDirectory.list();
        if (nodeNames == null) return Collections.emptyList();
        
        for ( String nodeConfigName : nodeNames) {
            try {
                final File nodePropsFile = new File(getConfigurationDirectory(nodeConfigName), NODE_PROPS_FILE);
                nodeConfigs.add(new Pair<NodeConfig, File>(loadNodeConfig( nodeConfigName, nodePropsFile, false), nodePropsFile));
            } catch ( IOException ioe ) {
                if ( throwOnError ) {
                    throw ioe;
                } else {
                    if ( !ExceptionUtils.causedBy(ioe, FileNotFoundException.class) ) {
                        logger.log( Level.WARNING, "Error loading configuration for node '" + nodeConfigName + "'.", ioe );
                    } else {
                        logger.log( Level.INFO, "Not loading configuration for node '" + nodeConfigName + "' due to '"+ExceptionUtils.getMessage(ioe)+"'." );
                    }
                }
            }
        }

        return nodeConfigs;
    }

    public static NodeConfig loadNodeConfig( final String name, final File nodeConfigFile, final boolean loadSecrets ) throws IOException {
        NodeConfig config;

        if ( nodeConfigFile.isFile() ) {
            Properties nodeProperties = new Properties();
            InputStream in = null;
            try {
                nodeProperties.load(in = new FileInputStream(nodeConfigFile));
            } finally {
                ResourceUtils.closeQuietly(in);
            }

            config = loadNodeConfig( name, nodeProperties, loadSecrets );
        } else {
            throw new FileNotFoundException( "Node configuration file missing or invalid '"+nodeConfigFile.getAbsolutePath()+"'." );
        }

        return config;
    }

    static NodeConfig loadNodeConfig( final String name, final Properties nodeProperties, final boolean loadSecrets ) throws IOException {
        if (!nodeProperties.containsKey(NODEPROPERTIES_ID))
            throw new CausedIOException("Unable to load node configuration for '"+name+"' due to invalid properties.");

        final PCNodeConfig node = new PCNodeConfig();
        node.setGuid(nodeProperties.getProperty(NODEPROPERTIES_ID));
        node.setName(name);
        node.setSoftwareVersion(SoftwareVersion.fromString(BuildInfo.getProductVersion()));
        node.setEnabled(Boolean.valueOf(nodeProperties.getProperty(NODEPROPERTIES_ENABLED, "true")));

        // load db settings
        String[] dbConfigurations = nodeProperties.getProperty(NODEPROPERTIES_DB_CONFIGS, "main").split("[\\s]{0,128},[\\s]{0,128}");
        Map<String,DatabaseConfig> configs = new LinkedHashMap<String,DatabaseConfig>();
        for ( String dbConfigName : dbConfigurations ) {
            loadNodeDatabaseConfig( nodeProperties, dbConfigName, loadSecrets, configs, true );
        }
        node.getDatabases().addAll( configs.values() );

        return node;
    }

    private static DatabaseConfig loadNodeDatabaseConfig( final Properties nodeProperties,
                                                          final String configName,
                                                          final boolean loadSecrets,
                                                          final Map<String,DatabaseConfig> configs,
                                                          final boolean allowParent ) throws IOException {
        DatabaseConfig config = null;

        if ( configs.containsKey( configName ) ) {
            // already loaded
            config = configs.get( configName );
        } else {
            // need to load
            String hostProp = MessageFormat.format(NODEPROPERTIES_DB_HOST_FORMAT, configName);
            String portProp = MessageFormat.format(NODEPROPERTIES_DB_PORT_FORMAT, configName);
            String nameProp = MessageFormat.format(NODEPROPERTIES_DB_NAME_FORMAT, configName);
            String userProp = MessageFormat.format(NODEPROPERTIES_DB_USER_FORMAT, configName);
            String passProp = MessageFormat.format(NODEPROPERTIES_DB_PASS_FORMAT, configName);
            String typeProp = MessageFormat.format(NODEPROPERTIES_DB_TYPE_FORMAT, configName);
            String inheProp = MessageFormat.format(NODEPROPERTIES_DB_INHE_FORMAT, configName);

            boolean needsParent = nodeProperties.containsKey(inheProp);
            DatabaseConfig parentConfig = null;
            if ( needsParent && allowParent ) {
                parentConfig = loadNodeDatabaseConfig( nodeProperties, nodeProperties.getProperty(inheProp), loadSecrets, configs, false );
            }

            if ( !needsParent || parentConfig!=null ) {
                if ( nodeProperties.keySet().containsAll( Arrays.asList( hostProp, portProp, nameProp, userProp) ) ||
                     (parentConfig!=null && nodeProperties.containsKey( hostProp ) ) ) {
                    try {
                        final DatabaseConfig db = new DatabaseConfig();
                        db.setParent( parentConfig );
                        db.setType( DatabaseType.NODE_ALL );
                        db.setHost( nodeProperties.getProperty(hostProp) );

                        if ( nodeProperties.containsKey(typeProp) )
                            db.setClusterType( NodeConfig.ClusterType.valueOf(nodeProperties.getProperty(typeProp)) );

                        if ( nodeProperties.containsKey(portProp) )
                            db.setPort( Integer.parseInt(nodeProperties.getProperty(portProp)) );

                        if ( nodeProperties.containsKey(nameProp) )
                            db.setName( nodeProperties.getProperty(nameProp) );

                        if ( nodeProperties.containsKey(userProp) )
                            db.setNodeUsername( nodeProperties.getProperty(userProp) );
                        
                        if ( loadSecrets && nodeProperties.containsKey(passProp) )
                            db.setNodePassword( nodeProperties.getProperty(passProp) );

                        configs.put( configName, db );
                        config = db;
                    } catch (IllegalArgumentException iae) {
                        throw new CausedIOException( iae.getMessage(), iae );
                    }
                }
            }
        }

        return config;
    }

    private static File getConfigurationDirectory( final String nodeName ) throws IOException {
        String path = MessageFormat.format( configPath, nodeName );
        return new File( nodesDir, path ).getCanonicalFile();
    }

    private static void setPropertyIfNotNull( final PropertiesConfiguration props, final String propName, final Object propValue ) {
        if ( propValue != null ) {
            props.setProperty( propName, propValue.toString() );
        }
    }
}
