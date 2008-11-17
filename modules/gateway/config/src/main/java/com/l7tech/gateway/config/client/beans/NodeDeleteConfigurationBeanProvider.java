package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.management.api.node.NodeManagementApi;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Level;

/**
 * ConfigurationBeanProvider for process controller / node configuration.
 *
 * @since 5.0
 */
public class NodeDeleteConfigurationBeanProvider extends NodeConfigurationBeanProviderSupport<NodeManagementApi.NodeHeader> implements ConfigurationBeanProvider {

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
    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        NodeManagementApi managementService = getManagementService();
        try {
            if ( config != null ) {
                if ( getOption("node.delete", configuration) != null && this.<Boolean>getOption("node.delete", configuration) ) {
                    managementService.deleteNode( config.getName(), 20000 );
                }
            }
        } catch ( ObjectModelException ome ) {
            throw new ConfigurationException( "Error saving configuration '"+ome.getMessage()+"'" );
        } catch ( SOAPFaultException sf ) {
            String message = "Unexpected error saving configuration '"+sf.getMessage()+"'";
            logger.log( Level.WARNING, message, sf);
            throw new ConfigurationException( message );
        } catch (NodeManagementApi.ForcedShutdownException e) {            
            logger.log( Level.WARNING, "Node did not shutdown within permitted time." );
        }
    }

    //- PACKAGE

    @Override
    NodeManagementApi getManagementService() {
        return nodeManagementApiFactory.getManagementService();
    }

    @Override
    NodeManagementApi.NodeHeader toConfig(NodeManagementApi.NodeHeader nodeHeader) throws FindException {
        return nodeHeader;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    Collection<ConfigurationBean> toBeans( final NodeManagementApi.NodeHeader config ) {
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
}