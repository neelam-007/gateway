package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.objectmodel.FindException;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

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
public class StateConfigurationBeanProvider extends NodeConfigurationBeanProviderSupport<NodeManagementApi.NodeHeader> implements ConfigurationBeanProvider {

    //- PUBLIC

    public StateConfigurationBeanProvider( final String nodeManagementUrl ) {
        this.nodeManagementApiFactory = new NodeManagementApiFactory( nodeManagementUrl );
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

    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        throw new ConfigurationException("This provider is read-only.");
    }

    //- PACKAGE

    NodeManagementApi getManagementService() {
        return nodeManagementApiFactory.getManagementService();
    }

    NodeManagementApi.NodeHeader toConfig(NodeManagementApi.NodeHeader nodeHeader) throws FindException {
        return nodeHeader;
    }

    Collection<ConfigurationBean> toBeans( final NodeManagementApi.NodeHeader config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        ConfigurationBean<String> statusConfigBean = new ConfigurationBean<String>();
        statusConfigBean.setConfigName( "status" );
        ConfigurationBean<Date> statusTimestampConfigBean = new ConfigurationBean<Date>();
        statusTimestampConfigBean.setConfigName( "status.time" );

        if ( config != null ) {
            statusConfigBean.setConfigValue( config.getState().toString() );
            if ( config.getSinceWhen() != null ) {
                statusTimestampConfigBean.setConfigValue( config.getSinceWhen() );
            } else {
                logger.warning("Received empty status timestamp.");
                statusTimestampConfigBean.setConfigValue( new Date() );
            }
        } else {
            statusConfigBean.setConfigValue( "Node Not Configured" );
            statusTimestampConfigBean.setConfigValue( new Date() );
        }

        configuration.add(statusConfigBean);
        configuration.add(statusTimestampConfigBean);

        return configuration;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StateConfigurationBeanProvider.class.getName() );

    private final NodeManagementApiFactory nodeManagementApiFactory;

}