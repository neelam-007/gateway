package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.OptionFilter;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.ExceptionUtils;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * ConfigurationBeanProvider for process controller / node configuration.
 *
 * @since 5.0
 */
public class NodeDeleteConfigurationBeanProvider extends NodeConfigurationBeanProviderSupport<NodeConfig> implements ConfigurationBeanProvider, OptionFilter {

    //- PUBLIC

    public NodeDeleteConfigurationBeanProvider( final NodeManagementApiFactory nodeManagementApiFactory ) {
        this.nodeManagementApiFactory = nodeManagementApiFactory;
    }

    @Override
    public boolean isValid() {
        boolean valid = false;

        NodeManagementApi managementService = getManagementService();
        try {
            Collection<NodeManagementApi.NodeHeader> headers = managementService.listNodes();
            valid = headers!=null && !headers.isEmpty();
        } catch ( FindException fe ) {
            logger.log(Level.WARNING, "Error listing nodes", fe );
        } catch ( SOAPFaultException sf ) {
            logger.log(Level.WARNING, "Error listing nodes", sf );
        }

        return valid;
    }

    @Override
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        // Load configuration only to check if there is a database to delete (see isOptionActive)
        super.loadConfiguration();

        // Don't actually use the configuration, since we're not editing it
        return Collections.emptyList();
    }

    @Override
    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            if ( config != null ) {
                boolean deleteConfig = getOption("node.delete", configuration) != null && this.<Boolean>getOption("node.delete", configuration);
                boolean deleteDatabase = getOption("database.admin.user", configuration) != null;

                if ( deleteDatabase && !deleteConfig ) {
                    throw new ConfigurationException("Cannot delete database unless configuration is also deleted.");
                }

                DatabaseConfig databaseConfig = config.getDatabase(DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER);
                if ( databaseConfig != null ) {
                    databaseConfig.setDatabaseAdminUsername( this.<String>getOption("database.admin.user", configuration) );
                    databaseConfig.setDatabaseAdminPassword( this.<String>getOption("database.admin.pass", configuration) );

                }

                if ( deleteConfig ) {
                    if ( deleteDatabase ) {
                        // check db creds before attempting delete
                        if ( !managementService.testDatabaseConfig( databaseConfig ) ) {
                            throw new ConfigurationException("Invalid database credentials.");
                        }
                    }

                    managementService.deleteNode( config.getName(), 20000 );
                }
                
                if ( deleteDatabase ) {
                    if ( databaseConfig != null ) {
                        Collection<String> hosts = new ArrayList<String>();
                        for ( DatabaseConfig dbConfig : config.getDatabases() ) {
                            hosts.add( dbConfig.getHost() );
                        }

                        managementService.deleteDatabase( databaseConfig, hosts );
                    }
                }
            }
        } catch ( ObjectModelException ome ) {
            throw new ConfigurationException( "Error when deleting node '"+ome.getMessage()+"'" );
        } catch (NodeManagementApi.DatabaseDeletionException dde) {
            throw new ConfigurationException( "Error deleting database '"+dde.getMessage()+"'" );
        } catch ( SOAPFaultException sf ) {
            String message = "Unexpected error saving configuration '"+sf.getMessage()+"'";
            logger.log( Level.WARNING, message, sf);
            throw new ConfigurationException( message );
        }
    }

    @Override
    public boolean isOptionActive( final OptionSet optionSet,
                                   final Option option ) {
        boolean active = true;

        if ( "database.admin.user".equals(option.getConfigName()) ||
             "database.admin.pass".equals(option.getConfigName()) ) {
            boolean deleteActive = false;
            if ( config != null ) {
                String managedHost = nodeManagementApiFactory.getHost();
                DatabaseConfig dbConfig =
                        config.getDatabase( DatabaseType.NODE_ALL,
                                            NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
                if ( dbConfig != null && nodeHostsDb(managedHost, dbConfig.getHost())) {
                    deleteActive = true;
                }
            }
            active = deleteActive;
        }

        return active;
    }

    //- PACKAGE

    @Override
    NodeManagementApi getManagementService() {
        return nodeManagementApiFactory.getManagementService();
    }

    @Override
    NodeConfig toConfig(NodeManagementApi.NodeHeader nodeHeader) throws FindException {
        return getManagementService().getNode( nodeHeader.getName() );
    }

    @Override
    @SuppressWarnings({"unchecked"})
    Collection<ConfigurationBean> toBeans( final NodeConfig config ) {
        return new ArrayList<ConfigurationBean>();
    }

    //- PRIVATE

    private final NodeManagementApiFactory nodeManagementApiFactory;

    @SuppressWarnings({"unchecked"})
    private <T> T getOption( final String id, final Collection<ConfigurationBean> beans ) {
        T value = null;

        for ( ConfigurationBean bean : beans ) {
            if ( id.equals(bean.getConfigName()) ) {
                value = (T) bean.getConfigValue();
                break;
            }
        }

        return value;
    }

    /**
     * Does the given node host the primary database? 
     */
    private boolean nodeHostsDb( final String nodeHost, final String dbHost ) {
        boolean nodeHostsDb = false;

        try {
            InetAddress nodeAddr = InetAddress.getByName( nodeHost );
            InetAddress dbAddr = InetAddress.getByName( dbHost );
            if ( dbAddr.isLoopbackAddress() ) {
                nodeHostsDb = true;
            } else if ( nodeAddr.isLoopbackAddress() ) {
                if ( NetworkInterface.getByInetAddress(dbAddr) != null ) {
                    nodeHostsDb = true;
                }
            } else if ( nodeAddr.getCanonicalHostName().equals(dbAddr.getCanonicalHostName()) ) {
                nodeHostsDb = true;
            }
        } catch ( UnknownHostException e ) {
            logger.warning( "Unknown host when checking if node hosts primary database '"+ExceptionUtils.getMessage(e)+"'." );
        } catch ( SocketException e ) {
            logger.log( Level.WARNING, "Error when checking if node hosts primary database '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
        }

        return nodeHostsDb;
    }
}