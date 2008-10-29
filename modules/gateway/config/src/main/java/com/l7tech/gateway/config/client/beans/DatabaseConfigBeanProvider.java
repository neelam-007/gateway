package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;

import com.l7tech.server.management.config.node.NodeConfig.ClusterType;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Level;

/**
 * ConfigurationBeanProvider for process controller / node configuration.
 *
 * @since 5.0
 */
public class DatabaseConfigBeanProvider extends ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public DatabaseConfigBeanProvider( final String nodeManagementUrl ) {
        super(nodeManagementUrl);
    }

    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            Collection<NodeManagementApi.NodeHeader> nodeHeaders = managementService.listNodes();
            if ( nodeHeaders != null && nodeHeaders.size() > 0) {
                config = managementService.getNode(DEFAULT_NODE_NAME);
                if ( config == null ) {
                    logger.warning("Could not get configuration for node '"+DEFAULT_NODE_NAME+"'.");
                }

                for (NodeManagementApi.NodeHeader node : nodeHeaders ) {
                    if ( !DEFAULT_NODE_NAME.equals(node.getName()) ) {
                        logger.warning("Will not report status for unsupported node '"+DEFAULT_NODE_NAME+"'.");
                    }
                }
            } else {
                logger.info("No nodes configured.");
            }
        } catch ( FindException fe ) {
            throw new ConfigurationException( "Error loading node configuration.", fe );
        } catch ( SOAPFaultException sf ) {
            throw new ConfigurationException( "Error loading node configuration", sf );
        }

        return toBeans( config );
    }

    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            if ( config != null ) {
                if ( getOption("node.enable", configuration) != null ) { 
                    config.setEnabled((Boolean)getOption("node.enable", configuration));
                }
                config.setClusterPassphrase((String)getOption("cluster.pass", configuration));
                fromBeans( config, configuration );
                managementService.updateNode( config );
            } else {
                // String newNodeName, String version, Map<DatabaseType, DatabaseConfig> databaseConfigMap
                NodeConfig config = new NodeConfig();
                config.setName("default");
                config.setEnabled((Boolean)getOption("node.enable", configuration));
                config.setClusterHostname((String)getOption("cluster.host", configuration));
                config.setClusterPassphrase((String)getOption("cluster.pass", configuration));
                fromBeans( config, configuration );
                managementService.createNode(
                        config,
                        (String)getOption("admin.user", configuration),
                        (String)getOption("admin.pass", configuration)                        
                );
            }
        } catch ( ObjectModelException ome ) {
            throw new ConfigurationException( "Error saving configuration '"+ome.getMessage()+"'" );
        } catch ( SOAPFaultException sf ) {
            String message = "Unexpected error saving configuration '"+sf.getMessage()+"'";
            logger.log( Level.WARNING, message, sf);
            throw new ConfigurationException( message );            
        } catch ( NodeManagementApi.RestartRequiredException rre ) {
            logger.log( Level.WARNING, "Restart required to apply configuration." );
        }
    }

    @SuppressWarnings({"unchecked"})
    private Collection<ConfigurationBean> toBeans( final NodeConfig config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        if ( config != null ) {
            logger.info("Processing configuration.");

            {
                ConfigurationBean<Boolean> configBean = new ConfigurationBean<Boolean>();
                configBean.setConfigName( "node.enable" );
                configBean.setConfigValue( config.isEnabled() );
                configuration.add(configBean);
            }

            if ( config.getDatabases() != null ) {
                logger.info("Processing "+config.getDatabases().size()+" databases.");

                for ( DatabaseConfig dbConfig : config.getDatabases() ) {
                    if ( dbConfig.getType() != DatabaseType.NODE_ALL ) {
                        logger.info("Skipping database configuration with type '"+ dbConfig.getType() +"'.");
                        continue;
                    }

                    if ( dbConfig.getClusterType() == ClusterType.STANDALONE ||
                         dbConfig.getClusterType() == ClusterType.REPL_MASTER ) {
                        // host
                        if ( dbConfig.getHost() != null ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.host" );
                            configBean.setConfigValue( dbConfig.getHost() );
                            configuration.add(configBean);
                        }

                        // port
                        if ( dbConfig.getPort() > 0l ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.port" );
                            configBean.setConfigValue( Integer.toString(dbConfig.getPort()) );
                            configuration.add(configBean);
                        }

                        // name
                        if ( dbConfig.getName() != null ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.name" );
                            configBean.setConfigValue( dbConfig.getName() );
                            configuration.add(configBean);
                        }

                        // username
                        if ( dbConfig.getNodeUsername() != null ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.user" );
                            configBean.setConfigValue( dbConfig.getNodeUsername() );
                            configuration.add(configBean);
                        }

                        // admin username
                        if ( dbConfig.getDatabaseAdminUsername() != null ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.admin.user" );
                            configBean.setConfigValue( dbConfig.getDatabaseAdminUsername() );
                            configuration.add(configBean);
                        }
                    } else if ( dbConfig.getClusterType() == ClusterType.REPL_SLAVE ) {
                        // failover host
                        if ( dbConfig.getHost() != null ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.failover.host" );
                            configBean.setConfigValue( dbConfig.getHost() );
                            configuration.add(configBean);
                        }

                        // failover port
                        if ( dbConfig.getPort() > 0l ) {
                            ConfigurationBean configBean = new ConfigurationBean();
                            configBean.setConfigName( "database.failover.port" );
                            configBean.setConfigValue( Integer.toString(dbConfig.getPort()) );
                            configuration.add(configBean);
                        }                        
                    }
                }
            }

        }

        return configuration;
    }

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

    private void fromBeans( final NodeConfig config, final Collection<ConfigurationBean> beans ) throws ConfigurationException {
        boolean addFailoverConfig = false;
        DatabaseConfig databaseConfig = config.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        DatabaseConfig failoverConfig = config.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.REPL_SLAVE );
        config.setDatabases(new ArrayList<DatabaseConfig>());

        if ( databaseConfig == null ) {
            databaseConfig = new DatabaseConfig();
        }

        if ( failoverConfig == null ) {
            failoverConfig = new DatabaseConfig();
        }

        databaseConfig.setType( DatabaseType.NODE_ALL );
        databaseConfig.setClusterType(NodeConfig.ClusterType.STANDALONE);

        for ( ConfigurationBean bean : beans ) {
            if ( "database.host".equals(bean.getConfigName()) ) {
                databaseConfig.setHost( bean.getConfigValue().toString() );
            } else if ( "database.port".equals(bean.getConfigName()) ) {
                try {
                    databaseConfig.setPort( Integer.parseInt(bean.getConfigValue().toString()) );
                } catch (NumberFormatException nfe) {
                    throw new ConfigurationException( "Invalid database port '"+bean.getConfigValue()+"'." );
                }
            } else if ( "database.name".equals(bean.getConfigName()) ) {
                databaseConfig.setName( bean.getConfigValue().toString() );
            } else if ( "database.user".equals(bean.getConfigName()) ) {
                databaseConfig.setNodeUsername( bean.getConfigValue().toString() );
            } else if ( "database.pass".equals(bean.getConfigName()) ) {
                databaseConfig.setNodePassword( bean.getConfigValue().toString() );
            } else if ( "database.admin.user".equals(bean.getConfigName()) ) {
                databaseConfig.setDatabaseAdminUsername( bean.getConfigValue().toString() );
            } else if ( "database.admin.pass".equals(bean.getConfigName()) ) {
                databaseConfig.setDatabaseAdminPassword( bean.getConfigValue().toString() );
            } else if ( "database.failover.host".equals(bean.getConfigName()) ) {
                if ( bean.getConfigValue() != null ) {
                    failoverConfig.setHost( bean.getConfigValue().toString() );
                    addFailoverConfig = true;
                }
            } else if ( "database.failover.port".equals(bean.getConfigName()) ) {
                if ( bean.getConfigValue() != null ) {
                    try {
                        failoverConfig.setPort( Integer.parseInt(bean.getConfigValue().toString()) );
                        addFailoverConfig = true;
                    } catch (NumberFormatException nfe) {
                        throw new ConfigurationException( "Invalid failover database port '"+bean.getConfigValue()+"'." );
                    }
                }
            }
        }

        config.getDatabases().add( databaseConfig );
        if ( addFailoverConfig ) {
            databaseConfig.setClusterType(NodeConfig.ClusterType.REPL_MASTER);
            failoverConfig.setClusterType(NodeConfig.ClusterType.REPL_SLAVE);
            config.getDatabases().add( failoverConfig );
        }
    }

    //- PRIVATE

    private NodeConfig config;
}
