package com.l7tech.server.ems.gateway;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.beans.factory.InitializingBean;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 
 */
public class GatewayPoller implements InitializingBean {

    //- PUBLIC

    public GatewayPoller( final PlatformTransactionManager transactionManager,
                          final Timer timer,
                          final AuditContext auditContext,
                          final SsgClusterManager ssgClusterManager,
                          final GatewayContextFactory gatewayContextFactory ) {
        this.transactionManager = transactionManager;
        this.timer = timer;
        this.auditContext = auditContext;
        this.ssgClusterManager = ssgClusterManager;
        this.gatewayContextFactory = gatewayContextFactory;
    }

    @SuppressWarnings({"override"})
    public void afterPropertiesSet() throws Exception {
        timer.schedule( new TimerTask(  ) {
            @Override
            public void run() {
                boolean isSystem = auditContext.isSystem();
                try {
                    auditContext.setSystem( true );
                    pollGateways();
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error polling gateways", e );
                } finally {
                    auditContext.setSystem( isSystem );
                }
            }
        }, 30000, 15000 );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GatewayPoller.class.getName() );

    private final PlatformTransactionManager transactionManager;
    private final Timer timer;
    private final AuditContext auditContext;                             
    private final SsgClusterManager ssgClusterManager;
    private final GatewayContextFactory gatewayContextFactory;

    private void pollGateways() {
        TransactionTemplate template = new TransactionTemplate( transactionManager );
        template.execute( new TransactionCallbackWithoutResult(){
            @Override
            protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                try {
                    Collection<SsgCluster> clusters = ssgClusterManager.findAll();
                    for ( SsgCluster cluster : clusters ) {
                        String host = cluster.getSslHostName();
                        int port = cluster.getAdminPort();

                        if ( host != null && host.length() > 0 && port > 0 ) {
                            try {
                                GatewayContext context = gatewayContextFactory.getGatewayContext( null, host, port );
                                Set<GatewayApi.GatewayInfo> newInfoSet = null;
                                try {
                                    GatewayApi api = context.getApi();
                                    GatewayApi.ClusterInfo info = api.getClusterInfo();
                                    if ( info != null ) {
                                        if ( !cluster.getTrustStatus() ) {
                                            logger.info("Trust established for gateway cluster '"+host+":"+port+"'.");
                                            cluster.setTrustStatus( true );
                                            ssgClusterManager.update( cluster );
                                        }
                                    }
                                    newInfoSet = new HashSet<GatewayApi.GatewayInfo>(api.getGatewayInfo());
                                } catch ( SOAPFaultException sfe ) {
                                    if ( GatewayContext.isNetworkException(sfe) ) {
                                        logger.log( Level.FINE, "Gateway connection failed for gateway '"+host+":"+port+"'." );
                                    } else {
                                        throw sfe;
                                    }
                                }

                                // Periodically update SSG Nodes.
                                Set<GatewayApi.GatewayInfo> currInfoSet = cluster.obtainGatewayInfoSet();
                                if ( newInfoSet != null && !newInfoSet.equals(currInfoSet) ) {
                                    Set<SsgNode> nodes = new HashSet<SsgNode>();

                                    for (GatewayApi.GatewayInfo newInfo: newInfoSet) {
                                        SsgNode node = new SsgNode();
                                        node.setGuid(newInfo.getId());
                                        node.setName(newInfo.getName());
                                        node.setSoftwareVersion(newInfo.getSoftwareVersion());
                                        node.setIpAddress(newInfo.getIpAddress());
                                        refreshNodeStatus(node, port);
                                        node.setSsgCluster(cluster);
                                        nodes.add(node);
                                    }
                                    cluster.getNodes().clear();
                                    cluster.getNodes().addAll(nodes);
                                    refreshClusterStatus(cluster);
                                    ssgClusterManager.update(cluster);
                                } else {
                                    boolean updated = false;
                                    for ( SsgNode node : cluster.getNodes() ) {
                                        updated = updated || refreshNodeStatus( node, port );
                                    }
                                    if ( updated ) {
                                        refreshClusterStatus(cluster);
                                        ssgClusterManager.update(cluster);
                                    }
                                }
                            } catch ( GatewayException ge ) {
                                logger.log( Level.WARNING, "Gateway error when polling gateways", ge );
                            } catch ( SOAPFaultException sfe ) {
                                if ( GatewayContext.isNetworkException(sfe) ) {
                                    logger.log( Level.FINE, "Gateway connection failed for gateway '"+host+":"+port+"'." );
                                } else if ( "Authentication Required".equals(sfe.getMessage()) ){
                                    if ( cluster.getTrustStatus() ) {
                                        logger.info("Trust lost for gateway cluster '"+host+":"+port+"'.");
                                        cluster.setTrustStatus( false );
                                        ssgClusterManager.update( cluster );
                                    }
                                } else if ( "Not Licensed".equals(sfe.getMessage()) ) {
                                    logger.fine("Gateway cluster is not licensed '"+host+":"+port+"'.");
                                } else{
                                    logger.log( Level.WARNING, "Gateway error when polling gateways", sfe );
                                }
                            }
                        }
                    }
                } catch ( ObjectModelException ome ) {
                    logger.log( Level.WARNING, "Persistence error when polling gateways", ome );
                }
            }
        } );
    }

    /**
     * Update the online status of the given cluster based on the status of its
     * nodes.
     */
    private void refreshClusterStatus( final SsgCluster cluster ) {
        int nodeCount = 0;
        int upCount = 0;

        if ( cluster.getNodes() != null ) {
            for ( SsgNode node : cluster.getNodes() ) {
                nodeCount++;
                if ( JSONConstants.SsgNodeOnlineState.ON.equals(node.getOnlineStatus()) ) {
                    upCount++;
                }

            }
        }

        String clusterStatus;
        if ( upCount == 0 ) {
            clusterStatus = JSONConstants.SsgClusterOnlineState.DOWN;
        } else if ( upCount != nodeCount ) {
            clusterStatus = JSONConstants.SsgClusterOnlineState.PARTIAL;
        } else {
            clusterStatus = JSONConstants.SsgClusterOnlineState.UP;
        }

        cluster.setOnlineStatus( clusterStatus );        
    }

    /**
     * Update the trusted / online status of the given ssg node.
     *
     * @return true if updated
     */
    private boolean refreshNodeStatus( final SsgNode node, final int port ) {
        boolean updated = false;

        final String host = node.getIpAddress();
        Boolean trusted = null;
        String status = JSONConstants.SsgNodeOnlineState.OFFLINE;
        try {
            NodeManagementApi nodeApi = gatewayContextFactory.getGatewayContext( null, host, 0 ).getManagementApi();
            Collection<NodeManagementApi.NodeHeader> nodeHeaders = nodeApi.listNodes();
            trusted = true;
            for ( NodeManagementApi.NodeHeader header : nodeHeaders ) {
                if ( header.getName().equals( "default" ) ) {
                    switch ( header.getState() ) {
                        case UNKNOWN:
                        case WONT_START:
                        case CRASHED:
                            status = JSONConstants.SsgNodeOnlineState.DOWN;
                            break;
                        case STARTING:
                        case RUNNING:
                            status = JSONConstants.SsgNodeOnlineState.ON;
                            break;
                        case STOPPING:
                        case STOPPED:
                            status = JSONConstants.SsgNodeOnlineState.OFF;
                            break;
                    }
                }
            }
        } catch ( SOAPFaultException sfe ) {
            if ( GatewayContext.isNetworkException(sfe) ) {
                logger.log( Level.FINE, "Gateway connection failed for gateway '"+host+"'." );

                // perhaps we don't have a process controller that works, let's check the node directly
                try {
                    GatewayContext context = gatewayContextFactory.getGatewayContext( null, host, port );
                    GatewayApi api = context.getApi();
                    if ( api.getClusterInfo() != null ) {
                        status = JSONConstants.SsgNodeOnlineState.ON;
                    }
                } catch (GatewayException e) {
                    // don't care
                    logger.log( Level.FINE, "Error checking gateway status using gateway api for '"+host+"'.", ExceptionUtils.getDebugException(e) );
                } catch ( SOAPFaultException sfe2 ) {
                    // don't care
                    if ( !GatewayContext.isNetworkException(sfe2) ) {
                        logger.log( Level.FINE, "Error checking gateway status using gateway api for  '"+host+"'.", ExceptionUtils.getDebugException(sfe2)  );
                    }
                }

            } else if ( "Authentication Required".equals(sfe.getMessage()) ){
                trusted = false;
            } else{
                logger.log( Level.WARNING, "Gateway error when polling gateways", sfe );
            }
        } catch (GatewayException e) {
            logger.log( Level.WARNING, "Error when polling gateways", e );
        } catch (FindException fe) {
            logger.log( Level.WARNING, "Gateway error when polling gateways '"+ExceptionUtils.getMessage(fe)+"'.", ExceptionUtils.getDebugException(fe));
        }

        if ( trusted != null && trusted != node.isTrustStatus() ) {
            updated = true;
            if ( !trusted ) {
                logger.info("Trust lost for gateway node '"+host+"'.");
            } else {
                logger.info("Trust established for gateway '"+host+"'.");
            }
            node.setTrustStatus(trusted);
            node.setOnlineStatus( status );
        } else if ( !status.equals( node.getOnlineStatus() ) ) {
            updated = true;
            logger.info("Gateway node status changed, status is now '"+status+"' (was '"+node.getOnlineStatus()+"').");
            node.setOnlineStatus( status );
        }

        return updated;
    }
}
