package com.l7tech.gateway.config.client.beans;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class NodeManagementApiFactory {

    //- PUBLIC

    public NodeManagementApiFactory() {
        this.nodeManagementUrl = null;
    }

    public NodeManagementApiFactory( final URL nodeManagementUrl ) {
        this.nodeManagementUrl = nodeManagementUrl;
    }

    public NodeManagementApiFactory( final String nodeManagementUrl ) {
        try {
            this.nodeManagementUrl = new URL(nodeManagementUrl);
        } catch ( MalformedURLException murle ) {
            throw new IllegalArgumentException("Invalid URL '"+nodeManagementUrl+"'", murle);
        }
    }

    public NodeManagementApi getManagementService() {
        NodeManagementApi managementService = this.managementService;

        if ( managementService == null ) {
            if ( nodeManagementUrl != null ) {
                managementService = buildPCManagementService( nodeManagementUrl.toString() );
            } else {
                try {
                    managementService = buildDirectManagementService();
                } catch ( IOException ioe ) {
                    throw new IllegalStateException("Node configuration error.", ioe);
                }
            }

            this.managementService = managementService;
         }

        return managementService;
    }

    /**
     * Get the host that this management API is running against.
     *
     * @return The hostname or ip address.
     */
    public String getHost() {
        return nodeManagementUrl==null ? InetAddressUtil.getLocalHostAddress() : nodeManagementUrl.getHost();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( NodeManagementApiFactory.class.getName() );

    private static final String DEFAULT_PC_HOSTCONFIG_PATH = "/opt/SecureSpan/Controller/etc/host.properties";
    private static final String PC_HOSTCONFIG_PATH = ConfigFactory.getProperty( "com.l7tech.gateway.config.pcHostProperties", DEFAULT_PC_HOSTCONFIG_PATH );

    private final URL nodeManagementUrl;
    private NodeManagementApi managementService;

    private NodeManagementApi buildPCManagementService( final String url ) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean(){
            @Override
            protected ClientProxy clientClientProxy( final Client c ) {
                HTTPConduit hc = (HTTPConduit)c.getConduit();
                HTTPClientPolicy policy = hc.getClient();
                policy.setCookie( "PC-AUTH="+getHostSecret() );                
                hc.setTlsClientParameters(new TLSClientParameters() {
                    @Override
                    public TrustManager[] getTrustManagers() {
                        return new TrustManager[] { new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                        }};
                    }

                    @Override
                    public boolean isDisableCNCheck() {
                        return true;
                    }
                });

                return super.clientClientProxy( c );
            }
        };
        factory.setServiceClass(NodeManagementApi.class);
        factory.setAddress(url);

        return (NodeManagementApi) factory.create();
    }

    private String getHostSecret() {
        String secret = ConfigFactory.getProperty( "com.l7tech.gateway.config.pcAuth", "" );
        if ( secret.isEmpty() ) {
            final File propertiesFile = new File(PC_HOSTCONFIG_PATH);
            if ( propertiesFile.exists() ) {
                try {
                    final Properties properties = loadProperties( propertiesFile );
                    secret = properties.getProperty( "host.secret", "" );
                } catch ( IOException e ) {
                    logger.warning( "Unable to load pc secret '"+ExceptionUtils.getMessage( e )+"'." );       
                }
            }
        }
        return secret;
    }

    private Properties loadProperties( final File propertiesFile ) throws IOException {
        final Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream( propertiesFile );
            properties.load(fileInputStream);
        } finally {
            ResourceUtils.closeQuietly(fileInputStream);
        }
        return properties;
    }

    private NodeManagementApi buildDirectManagementService() throws IOException {
        final Collection<NodeConfig> nodes = NodeConfigurationManager.loadNodeConfigs(false);
        return new NodeManagementApi(){
            @Override
            public Collection<NodeHeader> listNodes() throws FindException {
                Collection<NodeHeader> headers = new ArrayList<NodeHeader>();

                for ( NodeConfig nc : nodes ) {
                    headers.add( new NodeHeader( nc.getGuid(), nc.getName(), SoftwareVersion.fromString(BuildInfo.getProductVersion()), true, NodeStateType.UNKNOWN, new Date(), new Date()) );
                }

                return headers;
            }

            @Override
            public NodeConfig getNode(final String nodeName) throws FindException {
                return doGetNode( nodeName );
            }

            @Override
            public void createDatabase( final String nodeName,
                                        final DatabaseConfig dbconfig,
                                        final Collection<String> dbHosts,
                                        final String adminLogin,
                                        final String adminPassword,
                                        final String clusterHostname ) throws DatabaseCreationException {
                DBActions dbActions = new DBActions();
                String dbVersion = dbActions.checkDbVersion( dbconfig );
                if ( dbVersion == null ) {
                    throw new DatabaseCreationException("Unable to create database, there is an existing incompatible database.");
                } else if ( BuildInfo.getFormalProductVersion().equals(dbVersion) ) {
                    // further validate existing db?                    
                } else if ( "Unknown".equals(dbVersion) ) {
                    // create new db
                    try {
                        NodeConfigurationManager.createDatabase(nodeName, dbconfig, dbHosts, adminLogin, adminPassword, clusterHostname);
                    } catch (IOException e) {
                        throw new DatabaseCreationException("Unable to create database", e);
                    }
                } else {
                    throw new DatabaseCreationException("Unable to create database, there is an existing incompatible database (version "+dbVersion+").");
                }
            }

            @Override
            public NodeConfig createNode( final NodeConfig node) throws SaveException {
                if ( doGetNode(node.getName()) == null ) {
                    try {
                        DatabaseConfig firstDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE );
                        DatabaseConfig secondDb = null;
                        if (firstDb == null) {
                            //this isn't a standalone db config
                            firstDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.REPL_MASTER);
                            if (firstDb == null) {
                                throw new SaveException("A database node must be either standalone or a replication member");
                            }
                            secondDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.REPL_SLAVE);
                        }
                        NodeConfigurationManager.configureGatewayNode( node.getName(), true, node.getClusterPassphrase(), firstDb, secondDb);
                    } catch ( NodeConfigurationManager.NodeConfigurationException nce ) {
                        throw new SaveException( ExceptionUtils.getMessage(nce), nce );
                    } catch ( IOException ioe ) {
                        throw new SaveException( ioe );
                    }
                } else {
                    throw new SaveException("Node already exists '"+node.getName()+"'.");
                }

                return node;
            }

            @Override
            public void updateNode(final NodeConfig node) throws UpdateException, RestartRequiredException {
                if ( doGetNode(node.getName()) != null ) {
                    try {
                        DatabaseConfig firstDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE );
                        DatabaseConfig secondDb = null;
                        if (firstDb == null) {
                            //this isn't a standalone db config
                            firstDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.REPL_MASTER);
                            if (firstDb == null) {
                                throw new UpdateException("A database node must be either standalone or a replication member");
                            }
                            secondDb = node.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.REPL_SLAVE);
                        }
                        NodeConfigurationManager.configureGatewayNode( node.getName(), true, node.getClusterPassphrase(), firstDb, secondDb );
                    } catch ( NodeConfigurationManager.NodeConfigurationException nce ) {
                        throw new UpdateException( ExceptionUtils.getMessage(nce), nce );
                    } catch ( IOException ioe ) {
                        throw new UpdateException( ioe );
                    }
                } else {
                    throw new UpdateException("Node not found '"+node.getName()+"'.");
                }
            }

            private NodeConfig doGetNode(final String nodeName) {
                NodeConfig config = null;

                for ( NodeConfig nc : nodes ) {
                    if ( nc.getName().equals(nodeName) ) {
                        config = nc;
                        break;
                    }
                }

                return config;
            }

            // unsupported operations
            @Override
            public void deleteNode( String nodeName, int shutdownTimeout) throws DeleteException { throw new UnsupportedOperationException(); }
            @Override
            public NodeStateType startNode(String nodeName) throws FindException, StartupException { throw new UnsupportedOperationException(); }
            @Override
            public void stopNode(String nodeName, int timeout) throws FindException { throw new UnsupportedOperationException(); }
            @Override
            public boolean testDatabaseConfig(DatabaseConfig dbconfig) { throw new UnsupportedOperationException(); }
            @Override
            public void deleteDatabase(DatabaseConfig dbconfig, Collection<String> dbHosts) throws DatabaseDeletionException { throw new UnsupportedOperationException(); }
        };
    }
}
