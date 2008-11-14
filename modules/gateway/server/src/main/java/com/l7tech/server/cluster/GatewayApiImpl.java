package com.l7tech.server.cluster;

import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.util.Config;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Gateway API implementation.
 *
 * @author steve
 */
@Transactional(propagation=Propagation.REQUIRED)
public class GatewayApiImpl implements GatewayApi {

    private static final Logger logger = Logger.getLogger( GatewayApiImpl.class.getName() );

    private final Config config;
    private final ClusterInfoManager clusterInfoManager;

    public GatewayApiImpl( final Config config,
                           final ClusterInfoManager clusterInfoManager ) {
        this.config = config;
        this.clusterInfoManager = clusterInfoManager;
    }

    @Override
    public ClusterInfo getClusterInfo() {
        logger.info("Processing request for cluster info.");
        
        ClusterInfo info = new ClusterInfo();
        info.setClusterHostname( config.getProperty("clusterHost", "") );
        info.setClusterHttpPort( config.getIntProperty("clusterhttpport", 8080) );
        info.setClusterHttpsPort( config.getIntProperty("clusterhttpsport", 8443) );
        
        return info;
    }

    @Override
    public Collection<GatewayInfo> getGatewayInfo() {
        logger.info("Processing request for gateway info.");
        
        Set<GatewayInfo> gateways = new LinkedHashSet<GatewayInfo>();

        try {
            Collection<ClusterNodeInfo> clusterNodeInfos = clusterInfoManager.retrieveClusterStatus();
            if ( clusterNodeInfos != null ) {
                for( ClusterNodeInfo info : clusterNodeInfos ) {
                    GatewayInfo gatewayInfo = new GatewayInfo();
                    gatewayInfo.setId( info.getNodeIdentifier() );
                    gatewayInfo.setName( info.getName() );
                    gatewayInfo.setIpAddress( info.getAddress() );
                    gatewayInfo.setSoftwareVersion( BuildInfo.getProductVersion() );
                    gatewayInfo.setStatusTimestamp( info.getLastUpdateTimeStamp() );
                    gateways.add(gatewayInfo);
                }
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error accessing node status", fe );   
        }

        return gateways;
    }

}
