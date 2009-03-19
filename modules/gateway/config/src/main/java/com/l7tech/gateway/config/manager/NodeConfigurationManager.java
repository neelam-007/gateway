package com.l7tech.gateway.config.manager;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.db.ClusterPropertyUtil;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.HexUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

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

    public static class NodeConfigurationException extends Exception {
        public NodeConfigurationException( final String message ) {
            super(message);
        }

        public NodeConfigurationException( final String message, final Throwable cause ) {
            super(message, cause);
        }
    }

    public static final class DeleteNodeConfigurationException extends NodeConfigurationException {
        private final File file;

        public DeleteNodeConfigurationException( final String message, final File file ) {
            super(message);
            this.file = file;
        }

        public DeleteNodeConfigurationException( final String message, final File file, final Throwable cause ) {
            super(message, cause);
            this.file = file;
        }

        public String getNodeConfigFilePath() {
            return file==null ? "" : file.getAbsolutePath();
        }
    }

    /**
     * Configure a gateway node properties and validate database config if required..
     *
     * @param name The name of the node to configure ("default")
     * @param enabled Is the node enabled?
     * @param clusterPassword The cluster password
     * @param databaseConfig The database configuration to use.
     * @return the generated or re-used GUID for the node
     * @throws IOException If an error occurs
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static String configureGatewayNode( final String name,
                                               final Boolean enabled,
                                               final String clusterPassword,
                                               final DatabaseConfig databaseConfig,
                                               final DatabaseConfig database2ndConfig ) throws IOException, NodeConfigurationException {
        String nodeid;
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

            testDBConfig( databaseConfig );

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
            nodeid = props.getString(NODEPROPERTIES_ID);
        } else {
            // validate that we have enough settings to create a valid configuration
            if ( clusterPassword == null || databaseConfig == null ) {
                throw new NodeConfigurationException("Missing configuration parameters, cannot create new configuration for node '"+nodeName+"'.");
            }

            nodeid = loadOrCreateNodeIdentifier( name, databaseConfig, false );
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
            nodeProperties.setReadable(true, false);
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error writing properties file '"+nodeProperties.getAbsolutePath()+"'.", ce);
        } catch (FileNotFoundException e) {
            throw new CausedIOException("Error writing properties file '"+nodeProperties.getAbsolutePath()+"'.", e);
        } finally {
            ResourceUtils.closeQuietly(origFos);
        }

        return nodeid;
    }

    /**
     * Load the node identifier for a mac address on this system or create a new identifier.
     *
     * @param nodeName The name of the node
     * @param databaseConfig The database to connect to to load the node identifier.
     */
    public static String loadOrCreateNodeIdentifier( final String nodeName,
                                                     final DatabaseConfig databaseConfig,
                                                     final boolean allowDbFail ) throws IOException {
        String nodeid = null;

        // Check for existing GUID
        if ( "default".equals( nodeName ) ) {
            try {
                nodeid = dbActions.getNodeIdForMac( databaseConfig, getMacAddresses() );
            } catch (SQLException e) {
                if ( !allowDbFail )
                    throw new CausedIOException("Error checking for existing nodeid for this server.", e);
            }
        }
        if ( nodeid == null ) {
            nodeid = UUID.randomUUID().toString().replace("-","");
        } else {
            logger.info( "Using existing node identifier '"+nodeid+"'." );
        }

        return nodeid;
    }

    public static void deleteNodeConfig( final String nodeName ) throws DeleteNodeConfigurationException {
        try {
            File configFile = new File(getConfigurationDirectory(nodeName), NODE_PROPS_FILE);
            if ( configFile.isFile() ) {
                if ( !configFile.delete() ) {
                    throw new DeleteNodeConfigurationException( "Delete failed", configFile );
                }
            }
        } catch (IOException ioe) {
            throw new DeleteNodeConfigurationException( "Delete failed", null );
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
            throw new CausedIOException(MessageFormat.format("Cannot create database: ''{2}'' [code:{0}, {1}]",
                    res.getStatus().getCode(), res.getStatus(), res.getErrorMessage() == null ? "" : res.getErrorMessage() ), res.getThrown());
        }

        AccountReset.resetAccount(databaseConfig, adminLogin, adminPassword);
        if ( clusterHostname != null ) {
            ClusterPropertyUtil.addClusterProperty( dbActions, databaseConfig, CLUSTER_PROP_CLUSTERHOSTNAME, clusterHostname );
        }
    }

    /**
     * Test a database configuration.
     *
     * @param dbconfig The configuration for the database to be tested.
     * @return true if the test is successful
     */
    public static boolean testDatabase( final DatabaseConfig dbconfig ) {
        boolean ok = true;

        if ( dbconfig.getNodeUsername() != null && dbconfig.getNodePassword() != null) {
            try {
                ResourceUtils.closeQuietly( dbActions.getConnection( dbconfig, false ) );
            } catch ( Exception e ) {
                ok = false;
            }

        }

        if ( dbconfig.getDatabaseAdminUsername() != null && dbconfig.getDatabaseAdminPassword() != null) {
            try {
                ResourceUtils.closeQuietly( dbActions.getConnection( dbconfig, true ) );
            } catch ( Exception e ) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Delete a Gateway database.
     *
     * @param databaseConfig The configuration for the database to be deleted.
     * @param extraGrantHosts Additional hostnames from which access to the database should be granted
     * @param revokeGrants True to revoke grants
     */
    public static void deleteDatabase( final DatabaseConfig databaseConfig,
                                       final Collection<String> extraGrantHosts,
                                       final boolean revokeGrants )
        throws IOException {
        final DatabaseConfig localConfig;
        try {
            // If the host is localhost then use that when connecting
            if ( NetworkInterface.getByInetAddress(InetAddress.getByName(databaseConfig.getHost())) != null ) {
                localConfig = new DatabaseConfig(databaseConfig);
                localConfig.setHost("localhost");
            } else {
                throw new CausedIOException("Cannot delete database on remote host '"+databaseConfig.getHost()+"'.");
            }
        } catch ( UnknownHostException uhe ) {
            throw new CausedIOException("Could not resolve host '"+databaseConfig.getHost()+"' when deleting database.");
        }

        Set<String> hosts = new HashSet<String>();
        hosts.add( databaseConfig.getHost() );
        if (extraGrantHosts != null) hosts.addAll(extraGrantHosts);

        if ( !dbActions.dropDatabase( localConfig, hosts, true, revokeGrants, null) ) {
            throw new CausedIOException(MessageFormat.format("Cannot delete database ''{0}'' on ''{1}''.", databaseConfig.getName(), databaseConfig.getHost()));
        }
    }

    public static NodeConfig loadNodeConfig( final String name, final boolean loadSecrets ) throws IOException {
        return loadNodeConfig( name, new File(getConfigurationDirectory(name), NODE_PROPS_FILE), loadSecrets );
    }

    public static Collection<NodeConfig> loadNodeConfigs( final boolean throwOnError ) throws IOException {
        Collection<NodeConfig> nodeConfigs = new ArrayList<NodeConfig>();

        File nodeBaseDirectory = nodesDir;
        String[] nodeNames = nodeBaseDirectory.list();
        if (nodeNames == null) return Collections.emptyList();
        
        for ( String nodeConfigName : nodeNames) {
            try {
                nodeConfigs.add( loadNodeConfig( nodeConfigName, false ) );
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

    private static Collection<String> getMacAddresses() {
        ArrayList<String> output = new ArrayList<String>();

        try {
            for ( NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()) ) {
                byte[] macAddr = networkInterface.getHardwareAddress();
                if ( macAddr != null ) {
                    output.add(formatMac(macAddr));
                }
            }
        } catch (SocketException e) {
            logger.log( Level.FINE, "Error getting network interfaces '" + e.getMessage() + "'.", ExceptionUtils.getDebugException(e));
        }

        return output;
    }

    private static String formatMac( final byte[] macAddr ) {
        String hex = HexUtils.hexDump(macAddr).toUpperCase();
        StringBuilder hexBuilder = new StringBuilder();
        for ( int i=0; i < hex.length(); i++ ) {
            if ( i>0 && i%2==0 ) {
                hexBuilder.append(':');
            }
            hexBuilder.append(hex.charAt(i));
        }
        return hexBuilder.toString();
    }

    private static void testDBConfig( final DatabaseConfig databaseConfig ) throws NodeConfigurationException {
       try {
            ResourceUtils.closeQuietly( dbActions.getConnection( databaseConfig, false ) );
        } catch ( SQLException e ) {
            throw new NodeConfigurationException( "Database connection error '"+ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

}
