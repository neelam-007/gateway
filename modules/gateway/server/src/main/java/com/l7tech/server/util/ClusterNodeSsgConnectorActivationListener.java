package com.l7tech.server.util;

import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.transport.SsgConnector;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Endpoint activation listener that records the cluster node port for later use.
 */
public class ClusterNodeSsgConnectorActivationListener implements SsgConnectorActivationListener {

    //- PUBLIC

    public ClusterNodeSsgConnectorActivationListener( final ServerConfig serverConfig ) {
        this.serverConfig = serverConfig;
    }

    public void notifyActivated( final SsgConnector ssgConnector ) {
        if ( ssgConnector.offersEndpoint(SsgConnector.Endpoint.NODE_COMMUNICATION) ) {
            logger.log( Level.CONFIG, "Cluster port {0} activated.", Integer.toString(ssgConnector.getPort()));
            serverConfig.putProperty( ServerConfig.PARAM_CLUSTER_PORT, Integer.toString(ssgConnector.getPort()) );            
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClusterNodeSsgConnectorActivationListener.class.getName());

    private final ServerConfig serverConfig;

}
