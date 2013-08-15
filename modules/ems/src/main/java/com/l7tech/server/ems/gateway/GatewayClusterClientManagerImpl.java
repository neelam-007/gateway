package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link GatewayClusterClientManager}.
 */
public class GatewayClusterClientManagerImpl implements GatewayClusterClientManager, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(GatewayClusterClientManagerImpl.class.getName());
    private static final String PROP_CONN_TIMEOUT = "com.l7tech.server.ems.gateway.clusterConnectTimeout";
    private static final String PROP_READ_TIMEOUT = "com.l7tech.server.ems.gateway.clusterReadTimeout";

    private final GatewayContextFactory gatewayContextFactory;
    private final SsgClusterManager ssgClusterManager;

    private final ConcurrentHashMap<ClientKey, GatewayClusterClientImpl> clients = new ConcurrentHashMap<ClientKey, GatewayClusterClientImpl>();

    public GatewayClusterClientManagerImpl(GatewayContextFactory gatewayContextFactory, SsgClusterManager ssgClusterManager) {
        this.gatewayContextFactory = gatewayContextFactory;
        this.ssgClusterManager = ssgClusterManager;
    }

    private static class GatewayContextCreationException extends RuntimeException {
        private GatewayContextCreationException(GatewayException cause) {
            super(cause);
        }

        @Override
        public GatewayException getCause() {
            return (GatewayException)super.getCause();
        }
    }

    @Override
    public GatewayClusterClient getGatewayClusterClient( final String clusterId, final User user) throws GatewayException {
        Callable<SsgCluster> clusterFinder = new Callable<SsgCluster>() {
            @Override
            public SsgCluster call() throws FindException {
                return ssgClusterManager.findByGuid(clusterId);
            }
        };
        return getGatewayClusterClient(clusterId, clusterFinder, user);
    }

    @Override
    public GatewayClusterClient getGatewayClusterClient(final SsgCluster cluster, final User user) throws GatewayException {
        if ( cluster == null ) throw new GatewayException("Invalid Gateway Cluster");
        Callable<SsgCluster> clusterFinder = new Callable<SsgCluster>() {
            @Override
            public SsgCluster call() {
                return cluster;
            }
        };
        return getGatewayClusterClient(cluster.getGuid(), clusterFinder, user);
    }

    private SsgCluster findCluster(Callable<SsgCluster> cluster) throws GatewayException {
        SsgCluster ssgCluster;
        try {
            ssgCluster = cluster.call();
        } catch (Exception e) {
            throw new GatewayException(e);
        }
        return ssgCluster;
    }

    private GatewayClusterClient getGatewayClusterClient(final String clusterId, final Callable<SsgCluster> cluster, final User user) throws GatewayException {
        ClientKey key = new ClientKey(clusterId, user);
        try {
            GatewayClusterClientImpl client = clients.get(key);
            if (client != null)
                return client;

            final SsgCluster ssgCluster = findCluster(cluster);

            if ( ssgCluster == null )
                throw new GatewayException("No Gateway Cluster found with ID '" + clusterId + "'.");
            if ( !ssgCluster.getTrustStatus() )
                throw new GatewayNoTrustException("Bad trust status for cluster with ID '" + clusterId + "'.");

            final long connectionTimeout = ConfigFactory.getLongProperty( PROP_CONN_TIMEOUT, 15000 );
            final long readTimeout = ConfigFactory.getLongProperty( PROP_READ_TIMEOUT, 30000 );
            List<GatewayContext> nodeContexts = Functions.map(ssgCluster.getAvailableNodes(), new Functions.Unary<GatewayContext, SsgNode>() {
                @Override
                public GatewayContext call(SsgNode ssgNode) {
                    try {
                        final String nodeAdminAddress = ssgNode.getIpAddress();
                        final int nodePort = ssgNode.getGatewayPort();
                        GatewayContext nodeContext = gatewayContextFactory.createGatewayContext(user, clusterId, nodeAdminAddress, nodePort);
                        nodeContext.setConnectionTimeout( connectionTimeout );
                        nodeContext.setReadTimeout( readTimeout );
                        return nodeContext;
                    } catch (GatewayException e) {
                        throw new GatewayContextCreationException(e);
                    }
                }
            });

            // Add the public hostname of the entire cluster as a least-preference option, just in case
            // none of the individual node IPs are reachable from the ESM.
            if ( ssgCluster.isAvailable() ) {
                final int adminPort = ssgCluster.getAdminPort();
                GatewayContext publicContext = gatewayContextFactory.createGatewayContext(user, clusterId, ssgCluster.getSslHostName(), adminPort);
                if ( publicContext != null ) {
                    publicContext.setConnectionTimeout( connectionTimeout );
                    publicContext.setReadTimeout( readTimeout );
                    nodeContexts.add(publicContext);
                }
            }

            client = new GatewayClusterClientImpl(ssgCluster, nodeContexts);
            GatewayClusterClientImpl prev = clients.putIfAbsent(key, client);
            return prev != null ? prev : client;
        } catch (GatewayContextCreationException e) {
            GatewayException ge = ExceptionUtils.getCauseIfCausedBy( e, GatewayException.class );
            if ( ge != null ) throw ge;
            else throw new GatewayException(e);
        }
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        ApplicationEventMulticaster eventMulticaster = applicationContext.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
        eventMulticaster.addApplicationListener( new ApplicationListener(){
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                GatewayClusterClientManagerImpl.this.onApplicationEvent( event );
            }
        } );
    }

    public void onApplicationEvent(ApplicationEvent event) {
        Entity entity = null;
        if (event instanceof Deleted) {
            Deleted deleted = (Deleted) event;
            entity = deleted.getEntity();
        } else if (event instanceof Updated) {
            Updated updated = (Updated) event;
            entity = updated.getEntity();
        }

        if (entity instanceof User || entity instanceof SsgCluster || entity instanceof SsgNode) {
            // A User, cluster, or cluster node was changed or updated.  Flush all cached client info.
            // TODO flush only the affected caches
            logger.fine("Flushing cluster client caches.");
            clients.clear();
        }
    }

    static class ClientKey {
        final String clusterId;
        final Goid userProviderOid;
        final String userId;

        ClientKey(String clusterId, User user) {
            this(clusterId, user.getProviderId(), user.getId());
        }

        private ClientKey(String clusterId, Goid userProviderOid, String userId) {
            this.clusterId = clusterId;
            this.userProviderOid = userProviderOid;
            this.userId = userId;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientKey clientKey = (ClientKey) o;

            if (userProviderOid != null ? !userProviderOid.equals(clientKey.userProviderOid) : clientKey.userProviderOid != null) return false;
            if (clusterId != null ? !clusterId.equals(clientKey.clusterId) : clientKey.clusterId != null) return false;
            if (userId != null ? !userId.equals(clientKey.userId) : clientKey.userId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (clusterId != null ? clusterId.hashCode() : 0);
            result = 31 * result + (userProviderOid != null ? userProviderOid.hashCode() : 0);
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            return result;
        }
    }
}
