package com.l7tech.server.util;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.transport.SsgConnectorActivationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Endpoint activation listener that records the cluster node port for later use.
 */
public class ClusterNodeSsgConnectorActivationListener implements PostStartupApplicationListener {

    //- PUBLIC

    public ClusterNodeSsgConnectorActivationListener( final ServerConfig serverConfig ) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void onApplicationEvent( ApplicationEvent applicationEvent ) {
        if ( applicationEvent instanceof SsgConnectorActivationEvent ) {
            SsgConnectorActivationEvent event  = ( SsgConnectorActivationEvent )applicationEvent;
            notifyActivated( event.getConnector() );
        }
    }

    //- PRIVATE

    private void notifyActivated( final SsgConnector ssgConnector ) {
        if ( ssgConnector.offersEndpoint(SsgConnector.Endpoint.NODE_COMMUNICATION) ) {
            logger.log( Level.CONFIG, "Cluster port {0} activated.", Integer.toString(ssgConnector.getPort()));
            serverConfig.putProperty( ServerConfigParams.PARAM_CLUSTER_PORT, Integer.toString(ssgConnector.getPort()) );
        }
    }


    private static final Logger logger = Logger.getLogger(ClusterNodeSsgConnectorActivationListener.class.getName());

    private final ServerConfig serverConfig;

}
