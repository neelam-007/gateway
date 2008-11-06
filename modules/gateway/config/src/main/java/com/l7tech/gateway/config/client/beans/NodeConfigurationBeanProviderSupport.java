package com.l7tech.gateway.config.client.beans;

import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.objectmodel.FindException;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

/**
 * ConfigurationBeanProvider that is backed by a NodeManagementApi
 */
public abstract class NodeConfigurationBeanProviderSupport<C> implements ConfigurationBeanProvider {

    //- PUBLIC

    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            Collection<NodeManagementApi.NodeHeader> nodeHeaders = managementService.listNodes();
            if ( nodeHeaders != null && nodeHeaders.size() > 0) {
                for (NodeManagementApi.NodeHeader node : nodeHeaders ) {
                    if ( DEFAULT_NODE_NAME.equals(node.getName()) ) {
                        config = toConfig(node);
                    } else {
                        logger.warning("Will not process configuration for unsupported node '"+DEFAULT_NODE_NAME+"'.");
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

    public boolean isValid() {
        boolean valid = false;

        NodeManagementApi managementService = getManagementService();
        try {
            managementService.listNodes();
            valid = true;
        } catch ( FindException fe ) {
            logger.log(Level.WARNING, "Error listing nodes", fe );
        } catch ( SOAPFaultException sf ) {
            logger.log(Level.WARNING, "Error listing nodes", sf );
        }

        return valid;
    }

    //- PACKAGE

    final Logger logger = Logger.getLogger( getClass().getName() );

    final static String DEFAULT_NODE_NAME = "default";



    abstract NodeManagementApi getManagementService();
    abstract C toConfig( NodeManagementApi.NodeHeader nodeHeader ) throws FindException;
    abstract Collection<ConfigurationBean> toBeans( C config );

    C config;

}
