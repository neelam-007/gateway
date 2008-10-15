package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapFault;

/**
 * ConfigurationBeanProvider for process controller / node enabled state.
 *
 * @since 4.7
 */
public class StateConfigurationBeanProvider extends ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public StateConfigurationBeanProvider( final String nodeManagementUrl ) {
        super(nodeManagementUrl);
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
                boolean wasEnabled = config.isEnabled();
                fromBeans( config, configuration );

                if ( wasEnabled != config.isEnabled() ) {
                    managementService.updateNode( config );
                }
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

    private static final Logger logger = Logger.getLogger( DatabaseConfigBeanProvider.class.getName() );

    private NodeConfig config;

    private Collection<ConfigurationBean> toBeans( final NodeConfig config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        if ( config != null ) {
            // enabled
            ConfigurationBean<Boolean> configBean = new ConfigurationBean<Boolean>();
            configBean.setConfigName( "enabled" );
            configBean.setConfigValue( config.isEnabled() );
            configuration.add(configBean);
        }

        return configuration;
    }

    @SuppressWarnings({"unchecked"})
    private void fromBeans( final NodeConfig config, final Collection<ConfigurationBean> beans ) throws ConfigurationException {
        for ( ConfigurationBean bean : beans ) {
            if ( "enabled".equals(bean.getConfigName()) ) {
                config.setEnabled( ((ConfigurationBean<Boolean>)bean).getConfigValue() );
            }
        }
    }

}