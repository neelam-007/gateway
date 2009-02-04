package com.l7tech.server.ems.gateway;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.audit.LogonEvent;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * 
 */
public class GatewayPoller implements InitializingBean, ApplicationListener {

    //- PUBLIC

    public GatewayPoller( final PlatformTransactionManager transactionManager,
                          final Timer timer,
                          final SsgClusterManager ssgClusterManager,
                          final GatewayContextFactory gatewayContextFactory,
                          final UserPropertyManager userPropertyManager ) {
        this.transactionManager = transactionManager;
        this.timer = timer;
        this.ssgClusterManager = ssgClusterManager;
        this.gatewayContextFactory = gatewayContextFactory;
        this.userPropertyManager = userPropertyManager;
        this.timerTask = new TimerTask(  ) {
            @Override
            public void run() {
                boolean isSystem = AuditContextUtils.isSystem();
                try {
                    AuditContextUtils.setSystem( true );
                    pollGateways();
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error polling gateways", e );
                } finally {
                    AuditContextUtils.setSystem( isSystem );
                }
            }
        };
    }

    @SuppressWarnings({"override"})
    public void afterPropertiesSet() throws Exception {
        timer.schedule( timerTask, 30000, 15000 );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof GatewayRegistrationEvent ) {
            timer.schedule( timerTask, 0 );   
        } else if ( event instanceof LogonEvent) {
            LogonEvent logonEvent = (LogonEvent) event;
            if ( logonEvent.getType() == LogonEvent.LOGON ) {
                scheduleUserAccessChecks( (User)event.getSource() );
            }
        }
    }

    public void scheduleUserAccessChecks( final User user ) {
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                boolean isSystem = AuditContextUtils.isSystem();
                try {
                    AuditContextUtils.setSystem( true );
                    pollUserAccess( user );
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error polling gateways", e );
                } finally {
                    AuditContextUtils.setSystem( isSystem );
                }
            }
        }, 0);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GatewayPoller.class.getName() );

    private final PlatformTransactionManager transactionManager;
    private final Timer timer;
    private final SsgClusterManager ssgClusterManager;
    private final GatewayContextFactory gatewayContextFactory;
    private final UserPropertyManager userPropertyManager;
    private final TimerTask timerTask;

    private void pollUserAccess( final User user ) {
        if ( user != null ) {
            TransactionTemplate template = new TransactionTemplate( transactionManager );
            template.execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                    try {
                        Collection<SsgCluster> clusters = ssgClusterManager.findAll();
                        for ( SsgCluster cluster : clusters ) {
                            if ( cluster.getTrustStatus() && cluster.getOnlineStatus().equals( JSONConstants.SsgClusterOnlineState.UP ) ) {
                                try {
                                    String host = cluster.getSslHostName();
                                    int port = cluster.getAdminPort();
                                    GatewayContext context = gatewayContextFactory.getGatewayContext( user, cluster.getGuid(), host, port );
                                    context.getApi().getEntityInfo( Collections.singleton(EntityType.FOLDER) );
                                } catch ( GatewayException ge ) {
                                    // ok, can't update status
                                } catch (GatewayApi.GatewayException ge) {
                                    if ( "Authentication Required".equals(ge.getMessage()) ) {
                                        deleteAccessAccountMapping( user, cluster );
                                    }
                                } catch ( SOAPFaultException sfe ) {
                                    if ( GatewayContext.isNetworkException(sfe) ) {
                                        // ok, can't update status
                                    } else if ( "Access Denied".equals(sfe.getMessage()) ) {
                                        deleteAccessAccountMapping( user, cluster );   
                                    } else if ( "Authentication Required".equals(sfe.getMessage()) ){
                                        // ok, don't update status since the certificate trust failed
                                    } else if ( "Not Licensed".equals(sfe.getMessage()) ) {
                                        // ok, can't update status
                                    } else {
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
    }

    private void deleteAccessAccountMapping( final User user, final SsgCluster cluster ) throws ObjectModelException {
        Map<String,String> props = userPropertyManager.getUserProperties( user );
        String username = props.remove( "cluster." +  cluster.getGuid() + ".trusteduser" );
        if ( username != null ) {
            logger.info("Removing access account mapping '"+username+"', on cluster '"+cluster.getName()+"' for user '"+user.getLogin()+"'.");
            userPropertyManager.saveUserProperties( user, props );
        }
    }

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
                                        int appletPortFromEsm = cluster.getAdminAppletPort();
                                        int appletPortFromSsg = info.getAdminAppletPort();
                                        if (appletPortFromEsm != appletPortFromSsg) {
                                            cluster.setAdminAppletPort(appletPortFromSsg);
                                            ssgClusterManager.update(cluster);
                                        }
                                        if ( !cluster.getTrustStatus() ) {
                                            logger.info("Trust established for gateway cluster '"+host+":"+port+"'.");
                                            cluster.setTrustStatus( true );
                                            ssgClusterManager.update( cluster );
                                        }
                                    }
                                    Collection<GatewayApi.GatewayInfo> gatewayInfoCollection = api.getGatewayInfo();
                                    if ( gatewayInfoCollection != null ) {
                                        newInfoSet = new HashSet<GatewayApi.GatewayInfo>( gatewayInfoCollection );
                                    }
                                } catch ( SOAPFaultException sfe ) {
                                    if ( GatewayContext.isNetworkException(sfe) ) {
                                        logger.log( Level.FINE, "Gateway connection failed for gateway '"+host+":"+port+"'." );
                                    } else {
                                        throw sfe;
                                    }
                                }

                                // Periodically update SSG Nodes.
                                Set<GatewayApi.GatewayInfo> currInfoSet = cluster.obtainGatewayInfoSet();
                                if (newInfoSet == null) {
                                    cluster.getNodes().clear();
                                    cluster.setTrustStatus(false);
                                    refreshClusterStatus(cluster);
                                    ssgClusterManager.update(cluster);
                                } else if ( !newInfoSet.equals(currInfoSet) ) {
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
                                } else if ( "Could not send Message.".equals(sfe.getMessage()) && ExceptionUtils.causedBy(sfe, IOException.class)) {
                                    logger.info("Unexpected response from Gateway cluster '"+host+":"+port+"'.");
                                } else if ( sfe.getMessage() != null && sfe.getMessage().startsWith("Response was of unexpected ") && sfe.getMessage().contains("ContentType") ) {
                                    logger.info("Unexpected response from Gateway cluster '"+host+":"+port+"'.");
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
            if ( nodeHeaders != null ) {
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
