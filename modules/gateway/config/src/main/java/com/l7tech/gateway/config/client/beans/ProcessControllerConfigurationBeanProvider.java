package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.binding.soap.SoapFault;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

/**
 * ConfigurationBeanProvider that is backed by the Process Controller
 */
public class ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public ProcessControllerConfigurationBeanProvider( final URL nodeManagementUrl ) {
        this.nodeManagementUrl = nodeManagementUrl;
    }

    public boolean isValid() {
        boolean valid = false;

        NodeManagementApi managementService = getManagementService();
        try {
            Set<NodeManagementApi.NodeHeader> nodeHeaders = managementService.listNodes();
            valid = nodeHeaders == null || nodeHeaders.isEmpty();
        } catch ( FindException fe ) {
            logger.log(Level.WARNING, "Error listing nodes", fe );
        } catch ( SoapFault sf ) {
            logger.log(Level.WARNING, "Error listing nodes", sf );
        }

        return valid;
    }

    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            Set<NodeManagementApi.NodeHeader> nodeHeaders = managementService.listNodes();
            if ( nodeHeaders != null && nodeHeaders.size() > 0) {
                for (NodeManagementApi.NodeHeader node : nodeHeaders ) {
                    if ( config != null ) {
                        throw new ConfigurationException("Multiple nodes found, only single node is supported.");
                    }
                    config = managementService.getNode(node.getName());
                    if ( config == null ) {
                        logger.warning("Could not get configuration for node '"+node.getName()+"'.");
                    }
                }
            } else {
                logger.info("No nodes configured.");
            }
        } catch ( FindException fe ) {
            throw new ConfigurationException( "Error loading node configuration.", fe );
        } catch ( SoapFault sf ) {
            throw new ConfigurationException( "Error loading node configuration", sf );
        }

        return toBeans( config );
    }

    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            if ( config != null ) {
                boolean updated = false;
                List<DatabaseConfig> newConfig = new ArrayList<DatabaseConfig>();
                if ( config.getDatabases() != null ) {
                    for ( DatabaseConfig dbConfig : config.getDatabases() ) {
                        if ( dbConfig.getType() != DatabaseType.NODE_ALL ) {
                            newConfig.add( dbConfig );
                        } else {
                            fromBeans( dbConfig, configuration );
                            updated = true;
                        }
                    }
                }
                if ( !updated ) {
                    DatabaseConfig dbConfig = new DatabaseConfig();
                    fromBeans( dbConfig, configuration );
                    newConfig.add( dbConfig );
                }
                config.setDatabases( newConfig );
                managementService.updateNode( config );
            } else {
                // String newNodeName, String version, Map<DatabaseType, DatabaseConfig> databaseConfigMap
                DatabaseConfig dbConfig = new DatabaseConfig();
                fromBeans( dbConfig, configuration );
                NodeManagementApi.DatabaseConfigRow config = new NodeManagementApi.DatabaseConfigRow();
                config.setType(DatabaseType.NODE_ALL);
                config.setConfig(dbConfig);
                managementService.createNode( "default", null, getPassword(configuration), new HashSet<NodeManagementApi.DatabaseConfigRow>( Collections.singleton( config ) ) );
            }
        } catch ( ObjectModelException ome ) {
            throw new ConfigurationException( "Error storing node configuration", ome );
        } catch ( SoapFault sf ) {
            throw new ConfigurationException( "Error storing node configuration", sf );
        } catch ( NodeManagementApi.RestartRequiredException rre ) {
            logger.log( Level.WARNING, "Restart required to apply configuration." );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ProcessControllerConfigurationBeanProvider.class.getName() );

    private final URL nodeManagementUrl;
    private NodeManagementApi managementService;
    private NodeConfig config;

    private NodeManagementApi getManagementService() {
        NodeManagementApi managementService = this.managementService;

        if ( managementService == null ) {
            ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
            factory.setServiceClass(NodeManagementApi.class);
            factory.setAddress(nodeManagementUrl.toString());
            Client c = factory.getClientFactoryBean().create();
            HTTPConduit hc = (HTTPConduit)c.getConduit();
            hc.setTlsClientParameters(new TLSClientParameters() {
                @Override
                public TrustManager[] getTrustManagers() {
                    return new TrustManager[] { new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                    }};
                }

                @Override
                public boolean isDisableCNCheck() {
                    return true;
                }
            });
            managementService = (NodeManagementApi) factory.create();
            this.managementService = managementService;
         }

        return managementService;
    }

    private Collection<ConfigurationBean> toBeans( final NodeConfig config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        if ( config != null ) {
            if ( config.getDatabases() != null ) {
                for ( DatabaseConfig dbConfig : config.getDatabases() ) {
                    if ( dbConfig.getType() != DatabaseType.NODE_ALL ) continue;

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
                }
            }

        }

        return configuration;
    }

    private String getPassword( final Collection<ConfigurationBean> beans ) {
        String passphrase = null;

        for ( ConfigurationBean bean : beans ) {
            if ( "cluster.pass".equals(bean.getConfigName()) ) {
                passphrase = bean.getConfigValue();
                break;
            }
        }

        return passphrase;
    }

    private void fromBeans( final DatabaseConfig databaseConfig, final Collection<ConfigurationBean> beans ) throws ConfigurationException {
        databaseConfig.setType( DatabaseType.NODE_ALL );

        for ( ConfigurationBean bean : beans ) {
            if ( "database.host".equals(bean.getConfigName()) ) {
                databaseConfig.setHost( bean.getConfigValue() );
            } else if ( "database.port".equals(bean.getConfigName()) ) {
                try {
                    databaseConfig.setPort( Integer.parseInt(bean.getConfigValue()) );
                } catch (NumberFormatException nfe) {
                    throw new ConfigurationException( "Invalid database port '"+bean.getConfigValue()+"'." );
                }
            } else if ( "database.name".equals(bean.getConfigName()) ) {
                databaseConfig.setName( bean.getConfigValue() );
            } else if ( "database.user".equals(bean.getConfigName()) ) {
                databaseConfig.setNodeUsername( bean.getConfigValue() );
            } else if ( "database.pass".equals(bean.getConfigName()) ) {
                databaseConfig.setNodePassword( bean.getConfigValue() );
            } else if ( "database.admin.user".equals(bean.getConfigName()) ) {
                databaseConfig.setDatabaseAdminUsername( bean.getConfigValue() );
            } else if ( "database.admin.pass".equals(bean.getConfigName()) ) {
                databaseConfig.setDatabaseAdminPassword( bean.getConfigValue() );
            }
        }
    }
}
