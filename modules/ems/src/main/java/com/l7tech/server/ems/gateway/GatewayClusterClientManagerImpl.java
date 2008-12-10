package com.l7tech.server.ems.gateway;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link GatewayClusterClientManager}.
 */
public class GatewayClusterClientManagerImpl implements GatewayClusterClientManager, ApplicationListener {
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
        ClientKey key = new ClientKey(clusterId, user);
        try {
            GatewayClusterClientImpl client = clients.get(key);
            if (client != null)
                return client;

            final SsgCluster ssgCluster = ssgClusterManager.findByGuid(clusterId);
            if (ssgCluster == null)
                throw new GatewayException("No cluster found with ID " + clusterId);

            if ( !ssgCluster.getTrustStatus() )
                throw new GatewayNoTrustException("Bad trust status for cluster with ID " + clusterId);
            
            final int adminPort = ssgCluster.getAdminPort();
            List<GatewayContext> nodeContexts = Functions.map(ssgCluster.getNodes(), new Functions.Unary<GatewayContext, SsgNode>() {
                @Override
                public GatewayContext call(SsgNode ssgNode) {
                    try {
                        final String nodeAdminAddress = ssgNode.getIpAddress();
                        return gatewayContextFactory.getGatewayContext(user, clusterId, nodeAdminAddress, adminPort);
                    } catch (GatewayException e) {
                        throw new GatewayContextCreationException(e);
                    }
                }
            });

            // Add the public hostname of the entire cluster as a least-preference option, just in case
            // none of the individual node IPs are reachable from the ESM.
            GatewayContext publicContext = gatewayContextFactory.getGatewayContext(user, ssgCluster.getSslHostName(), adminPort);
            nodeContexts.add(publicContext);

            client = new GatewayClusterClientImpl(ssgCluster, nodeContexts);
            GatewayClusterClientImpl prev = clients.putIfAbsent(key, client);
            return prev != null ? prev : client;
        } catch (GatewayContextCreationException e) {
            GatewayException ge = ExceptionUtils.getCauseIfCausedBy( e, GatewayException.class );
            if ( ge != null ) throw ge;
            else throw new GatewayException(e);
        } catch (FindException e) {
            throw new GatewayException(e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        Entity entity = null;
        if (event instanceof Deleted) {
            Deleted deleted = (Deleted) event;
            entity = deleted.getEntity();
        } else if (event instanceof Updated) {
            Updated updated = (Updated) event;
            entity = updated.getEntity();
        }

        if (entity instanceof User || entity instanceof SsgCluster || entity instanceof ClusterNodeInfo) {
            // A User, cluster, or cluster node was changed or updated.  Flush all cached client info.
            // TODO flush only the affected caches
            clients.clear();
        }
    }

    static class ClientKey {
        final String clusterId;
        final long userProviderOid;
        final String userId;

        public ClientKey(String clusterId, User user) {
            this(clusterId, user.getProviderId(), user.getId());
        }

        private ClientKey(String clusterId, long userProviderOid, String userId) {
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

            if (userProviderOid != clientKey.userProviderOid) return false;
            if (clusterId != null ? !clusterId.equals(clientKey.clusterId) : clientKey.clusterId != null) return false;
            if (userId != null ? !userId.equals(clientKey.userId) : clientKey.userId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (clusterId != null ? clusterId.hashCode() : 0);
            result = 31 * result + (int) (userProviderOid ^ (userProviderOid >>> 32));
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            return result;
        }
    }
}
