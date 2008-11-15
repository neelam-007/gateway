package com.l7tech.server.ems.gateway;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.beans.factory.InitializingBean;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.ConnectException;

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
                                GatewayApi api = context.getApi();
                                GatewayApi.ClusterInfo info = api.getClusterInfo();
                                if ( info != null ) {
                                    if ( !cluster.getTrustStatus() ) {
                                        logger.info("Trust established for gateway cluster '"+host+":"+port+"'.");
                                        cluster.setTrustStatus( true );
                                        ssgClusterManager.update( cluster );
                                    }
                                }

                                // Periodically update SSG Nodes.
                                Set<GatewayApi.GatewayInfo> currInfoSet = cluster.obtainGatewayInfoSet();
                                Set<GatewayApi.GatewayInfo> newInfoSet = new HashSet<GatewayApi.GatewayInfo>(api.getGatewayInfo());
                                if (! newInfoSet.equals(currInfoSet)) {
                                    Set<SsgNode> nodes = new HashSet<SsgNode>();

                                    for (GatewayApi.GatewayInfo newInfo: newInfoSet) {
                                        SsgNode node = new SsgNode();
                                        node.setGuid(newInfo.getId());
                                        node.setName(newInfo.getName());
                                        node.setSoftwareVersion(newInfo.getSoftwareVersion());
                                        node.setIpAddress(newInfo.getIpAddress());
                                        node.setOnlineStatus("xxx"); // todo: use real status later on
                                        node.setTrustStatus(false);  // todo: use real status later on
                                        node.setSsgCluster(cluster);
                                        nodes.add(node);
                                    }
                                    cluster.getNodes().clear();
                                    cluster.getNodes().addAll(nodes);
                                    ssgClusterManager.update(cluster);
                                }
                            } catch ( GatewayException ge ) {
                                logger.log( Level.WARNING, "Gateway error when polling gateways", ge );
                            } catch ( SOAPFaultException sfe ) {
                                if ( ExceptionUtils.causedBy( sfe, ConnectException.class ) ) {
                                    logger.log( Level.INFO, "Gateway connection failed for gateway '"+host+":"+port+"'." );
                                } else if ( "Authentication Required".equals(sfe.getMessage()) ){
                                    if ( cluster.getTrustStatus() ) {
                                        logger.info("Trust lost for gateway cluster '"+host+":"+port+"'.");
                                        cluster.setTrustStatus( false );
                                        ssgClusterManager.update( cluster );
                                    }
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
}
