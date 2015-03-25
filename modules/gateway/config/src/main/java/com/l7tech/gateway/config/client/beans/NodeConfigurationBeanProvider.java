package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.OptionInitializer;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.NodeConfig.ClusterType;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Option;

import javax.xml.ws.WebServiceException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import static com.l7tech.util.Option.optional;

/**
 * ConfigurationBeanProvider for process controller / node configuration.
 *
 * @since 5.0
 */
public class NodeConfigurationBeanProvider extends NodeConfigurationBeanProviderSupport<NodeConfig> implements ConfigurationBeanProvider, OptionInitializer {

    //- PUBLIC

    public NodeConfigurationBeanProvider( final NodeManagementApiFactory nodeManagementApiFactory ) {
        this.nodeManagementApiFactory = nodeManagementApiFactory;
    }

    @Override
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
                Boolean enable = (Boolean)getOption("node.enable", configuration);
                if ( enable == null ) enable = true;
                config.setEnabled(enable);
                config.setClusterHostname((String)getOption("cluster.host", configuration));
                config.setClusterPassphrase((String)getOption("cluster.pass", configuration));
                fromBeans( config, configuration );

                final Option<DatabaseConfig> databaseConfig = optional( config.getDatabase( DatabaseType.NODE_ALL, ClusterType.STANDALONE, ClusterType.REPL_MASTER ) );
                String adminLogin = (String) getOption("admin.user", configuration);
                String adminPassphrase = (String)getOption("admin.pass", configuration);

                boolean createdb =
                        config.getClusterHostname() != null &&
                        config.getClusterHostname().trim().length() > 0 &&
                        (!databaseConfig.isSome() ||
                         databaseConfig.some().getDatabaseAdminUsername()!=null) &&
                        adminLogin != null &&
                        adminLogin.trim().length() > 0 &&
                        adminPassphrase != null &&
                        adminPassphrase.trim().length() > 0;

                if ( createdb ) {
                    Collection<String> hosts = new ArrayList<String>();
                    for ( DatabaseConfig dbConfig : config.getDatabases() ) {
                        hosts.add( dbConfig.getHost() );
                    }
                    managementService.createDatabase( config.getName(), databaseConfig.toNull(), hosts,  adminLogin, adminPassphrase, config.getClusterHostname() );
                }

                Boolean dbOnly = getOption("configure.dbonly", configuration);
                if ( dbOnly == null || !dbOnly ) {
                    managementService.createNode(config);
                }
            }
        } catch ( NodeManagementApi.DatabaseCreationException dce ) {
            throw new ConfigurationException( "Error creating database when saving configuration '"+dce.getMessage()+"'" );
        } catch ( ObjectModelException ome ) {
            throw new ConfigurationException( "Error saving configuration '"+ome.getMessage()+"'" );
        } catch ( WebServiceException e ) {
            String message = "Unexpected error saving configuration '"+e.getMessage()+"'";
            logger.log( Level.WARNING, message, e);
            throw new ConfigurationException( message );            
        } catch ( NodeManagementApi.RestartRequiredException rre ) {
            logger.log( Level.WARNING, "Restart required to apply configuration." );
        }
    }

    @Override
    public Object getInitialValue( final String configName ) {
        Object value = null;

        if ( "cluster.host".equals(configName) ) {
            String hostname = null;
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                try {
                    hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e2) {
                    // fail
                }
            }
            if ( hostname != null && !InetAddressUtil.isValidIpv4Address(hostname) || !InetAddressUtil.isValidIpv6Address(hostname) ) {
                value = hostname;
            }
        }

        return value;
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

        if ( databaseConfig.getHost() != null && !databaseConfig.getHost().isEmpty() ) {
            config.getDatabases().add( databaseConfig );
        }
        if ( addFailoverConfig ) {
            databaseConfig.setClusterType(NodeConfig.ClusterType.REPL_MASTER);
            failoverConfig.setClusterType(NodeConfig.ClusterType.REPL_SLAVE);
            config.getDatabases().add( failoverConfig );
        }
    }
}
