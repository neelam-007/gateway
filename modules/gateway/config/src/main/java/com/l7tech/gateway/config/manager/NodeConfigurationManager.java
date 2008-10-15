package com.l7tech.gateway.config.manager;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.config.manager.db.DBActions;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;

/**
 * Manages configuration for a node.
 */
public class NodeConfigurationManager {

    private static final Logger logger = Logger.getLogger(NodeConfigurationManager.class.getName());
    private static final String configPath = "../node/{0}/etc/conf";
    private static final String sqlPath = "../config/etc/sql/ssg.sql";

    //only these files will be copied. Anything else left in SSG_ROOT/etc/conf is likley custom, like a custom assertion
//    private static String[] configFileWhitelist = new String[] {
//        "ssglog.properties",
//        "system.properties",
//    };

    /**
     * Configure a gateway node properties and create database if required..
     *
     * @param name The name of the node to configure ("default")
     * @param nodeid The unique identifier to use for the node.
     * @param enabled Is the node enabled?
     * @param defaultClusterHostname The cluster hostname to use by default (may be null).
     * @param clusterPassword The cluster password
     * @param databaseConfig The database configuration to use.
     * @throws IOException If an error occurs
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static void configureGatewayNode( final String name,
                                             final String nodeid,
                                             final Boolean enabled,
                                             final String defaultClusterHostname,
                                             final String clusterPassword,
                                             final DatabaseConfig databaseConfig ) throws IOException {
        String nodeName = name;
        if ( nodeName == null ) {
            nodeName = "default";    
        }

        String path = MessageFormat.format( configPath, nodeName );
        File configDirectory = new File( path ).getCanonicalFile();

        logger.log( Level.INFO, "Configuring node in directory ''{0}''.", configDirectory.getAbsolutePath());
        if ( !configDirectory.isDirectory() ) {
            throw new FileNotFoundException( "Missing configuration directory '" + configDirectory.getAbsolutePath() + "'." );
        }

        // Update DB
        if ( databaseConfig != null ) {
            DBActions dbActions = new DBActions();
            String dbVersion = dbActions.checkDbVersion( databaseConfig );
            if ( dbVersion != null && !dbVersion.equals(BuildInfo.getFormalProductVersion()) ) {
                throw new CausedIOException("Database version mismatch '"+dbVersion+"'.");
            }
            if ( dbVersion == null ) { // then create the DB
                String pathToSqlScript = MessageFormat.format( sqlPath, nodeName );
                File sqlScriptFile = new File( pathToSqlScript ).getCanonicalFile();
                dbActions.createDb( databaseConfig, sqlScriptFile.getAbsolutePath(), false );
            }
        }

        // Update config
        File nodeProperties = new File( configDirectory, "node.properties" );

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
                throw new CausedIOException("Missing configuration parameters, cannot create new configuration for node '"+nodeName+"'.");
            }

            if ( setEnabled == null ) setEnabled = true;
        }

        MasterPasswordManager mpm = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(configDirectory, "omp.dat") ) );
        String encDatabasePassword = databaseConfig==null ? null : mpm.encryptPassword( databaseConfig.getNodePassword().toCharArray() );
        String encClusterPassword = clusterPassword==null ? null : mpm.encryptPassword( clusterPassword.toCharArray() );

        setPropertyIfNotNull( props, "node.id", nodeid );
        setPropertyIfNotNull( props, "node.enabled", setEnabled );
        setPropertyIfNotNull( props, "node.cluster.pass", encClusterPassword );
        if ( databaseConfig != null ) {
            setPropertyIfNotNull( props, "node.db.host", databaseConfig.getHost() );
            setPropertyIfNotNull( props, "node.db.port", Integer.toString(databaseConfig.getPort()) );
            setPropertyIfNotNull( props, "node.db.name", databaseConfig.getName() );
            setPropertyIfNotNull( props, "node.db.user", databaseConfig.getNodeUsername() );
            setPropertyIfNotNull( props, "node.db.pass", encDatabasePassword );
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

    private static void setPropertyIfNotNull( final PropertiesConfiguration props, final String propName, final Object propValue ) {
        if ( propValue != null ) {
            props.setProperty( propName, propValue.toString() );
        }
    }

//    private static void updateJavaSecurity(File destinationPartition) throws IOException {
//        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions(destinationPartition.getName());
//        String keystoreFile = osf.getKeyStorePropertiesFile();
//        Properties props = new Properties();
//
//        InputStream is = null;
//        String[] providerList = null;
//        try {
//            is = new FileInputStream(keystoreFile);
//            props.load(is);
//            String keystoreType = props.getProperty(KeyStoreConstants.PROP_KS_TYPE);
//            if (keystoreType.equalsIgnoreCase(KeystoreType.SCA6000_KEYSTORE_NAME.getShortTypeName())) {
//                providerList = KeyStoreConstants.HSM_SECURITY_PROVIDERS;
//            } else if (keystoreType.equalsIgnoreCase(KeystoreType.DEFAULT_KEYSTORE_NAME.getShortTypeName())) {
//                providerList = KeyStoreConstants.DEFAULT_SECURITY_PROVIDERS;
//            } else if (keystoreType.equalsIgnoreCase(KeystoreType.LUNA_KEYSTORE_NAME.getShortTypeName())) {
//                providerList = KeyStoreConstants.LUNA_SECURITY_PROVIDERS;
//            }
//        } finally {
//            ResourceUtils.closeQuietly(is);
//        }
//
//        String javaSecurity = osf.getPathToJavaSecurityFile();
//        if (providerList != null)
//            KeystoreActions.updateJavaSecurity(new File(javaSecurity), new File(javaSecurity + ".backup"), providerList);
//    }
}
