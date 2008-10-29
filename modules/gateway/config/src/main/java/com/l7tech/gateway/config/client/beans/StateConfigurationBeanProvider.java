package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.objectmodel.FindException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * ConfigurationBeanProvider for process controller / node enabled status.
 *
 * <p>This is to retrieve the runtime status, to read/edit the enabled/disabled
 * state use the main configuration provider.</p>
 *
 * <p>This configuration bean provider is read only.</p>
 *
 * @since 5.0
 */
public class StateConfigurationBeanProvider extends ProcessControllerConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public StateConfigurationBeanProvider( final String nodeManagementUrl ) {
        super(nodeManagementUrl);
    }

    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            Collection<NodeManagementApi.NodeHeader> nodeHeaders = managementService.listNodes();
            if ( nodeHeaders != null && nodeHeaders.size() > 0) {
                for (NodeManagementApi.NodeHeader node : nodeHeaders ) {
                    if ( DEFAULT_NODE_NAME.equals(node.getName()) ) {
                        config = node;
                    } else {
                        logger.warning("Will not report status for unsupported node '"+DEFAULT_NODE_NAME+"'.");
                    }
                }

                if ( config == null ) {
                    logger.warning("Could not get configuration for node '"+DEFAULT_NODE_NAME+"'.");
                }
            } else {
                logger.info("No nodes configured.");
            }
        } catch ( FindException fe ) {
            throw new ConfigurationException( "Error loading node configuration.", fe );
        } 

        return toBeans( config );
    }

    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        throw new ConfigurationException("This provider is read-only.");
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( DatabaseConfigBeanProvider.class.getName() );

    private NodeManagementApi.NodeHeader config;

    private Collection<ConfigurationBean> toBeans( final NodeManagementApi.NodeHeader config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        ConfigurationBean<String> configBean = new ConfigurationBean<String>();
        configBean.setConfigName( "status" );
        if ( config != null ) {
            configBean.setConfigValue( config.getState().toString() );
        } else {
            configBean.setConfigValue( "Node Not Configured" );
        }
        configuration.add(configBean);

        return configuration;
    }

}