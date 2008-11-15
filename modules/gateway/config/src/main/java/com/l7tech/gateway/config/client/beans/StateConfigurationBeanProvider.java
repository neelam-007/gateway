package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
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
public class StateConfigurationBeanProvider extends NodeConfigurationBeanProviderSupport<NodeManagementApi.NodeHeader> implements ConfigurationBeanProvider, StatusCodeSource {

    //- PUBLIC

    public StateConfigurationBeanProvider( final String nodeManagementUrl ) {
        this.nodeManagementApiFactory = new NodeManagementApiFactory( nodeManagementUrl );
    }

    @Override
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

    @Override
    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        throw new ConfigurationException("This provider is read-only.");
    }

    @Override
    public int getStatusCode() {
        return code;
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
    Collection<ConfigurationBean> toBeans( final NodeManagementApi.NodeHeader config ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        ConfigurationBean<String> statusConfigBean = new ConfigurationBean<String>();
        statusConfigBean.setConfigName( "status" );
        ConfigurationBean<Date> statusTimestampConfigBean = new ConfigurationBean<Date>();
        statusTimestampConfigBean.setConfigName( "status.time" );
        ConfigurationBean<Date> statusSinceConfigBean = new ConfigurationBean<Date>();
        statusSinceConfigBean.setConfigName( "status.since" );

        if ( config != null ) {
            statusConfigBean.setConfigValue( config.getState().toString() );
            switch ( config.getState() ) {
                case RUNNING:
                case STARTING:
                    code = CODE_RUNNING;
                    break;
                case CRASHED:
                case WONT_START:
                case STOPPING:
                case STOPPED:
                    code = CODE_STOPPED;
                    break;
                case UNKNOWN:
                default:
                    code = 0;
                    break;
            }

            if ( config.getSinceWhen() != null ) {
                statusTimestampConfigBean.setConfigValue( config.getSinceWhen() );
            } else {
                logger.warning("Received empty status timestamp.");
                statusTimestampConfigBean.setConfigValue( new Date() );
            }
            if ( config.getStateStartTime() != null ) {
                statusSinceConfigBean.setConfigValue( config.getStateStartTime() );
            } else {
                logger.warning("Received empty status starttime.");
                statusSinceConfigBean.setConfigValue( new Date() );
            }
        } else {
            statusConfigBean.setConfigValue( "Node Not Configured" );
            statusTimestampConfigBean.setConfigValue( new Date() );
            statusSinceConfigBean.setConfigValue( new Date() );
        }

        configuration.add(statusConfigBean);
        configuration.add(statusTimestampConfigBean);
        configuration.add(statusSinceConfigBean);

        return configuration;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StateConfigurationBeanProvider.class.getName() );

    private static final int CODE_RUNNING = 21;
    private static final int CODE_STOPPED = 22;

    private final NodeManagementApiFactory nodeManagementApiFactory;
    private int code = 0;                                        
}