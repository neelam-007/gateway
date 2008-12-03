package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link GatewayClusterClientManager}.
 */
public class GatewayClusterClientManagerImpl implements GatewayClusterClientManager, ApplicationListener {
    private final GatewayContextFactory gatewayContextFactory;
    private final SsgClusterManager ssgClusterManager;

    public GatewayClusterClientManagerImpl(GatewayContextFactory gatewayContextFactory, SsgClusterManager ssgClusterManager) {
        this.gatewayContextFactory = gatewayContextFactory;
        this.ssgClusterManager = ssgClusterManager;
    }

    private static class GatewayContextCreationException extends RuntimeException {
        private GatewayContextCreationException(GatewayException cause) {
            super(cause);
        }

        public GatewayException getCause() {
            return (GatewayException)super.getCause();
        }
    }

    public GatewayClusterClient getGatewayClusterClient(String clusterId, final User user) throws GatewayException {
        try {
            // TODO cache client impls by (clusterId, user); invalidate on user/cluster/node change or deletion
            final SsgCluster ssgCluster = ssgClusterManager.findByGuid(clusterId);
            Set<SsgNode> nodes = ssgCluster.getNodes();
            List<GatewayContext> nodeContexts = Functions.map(nodes, new Functions.Unary<GatewayContext, SsgNode>() {
                public GatewayContext call(SsgNode ssgNode) {
                    try {
                        // TODO confirm that node.getIpAddress() is the appropriate target for ESM admin connections
                        final String nodeAdminAddress = ssgNode.getIpAddress();
                        return gatewayContextFactory.getGatewayContext(user, nodeAdminAddress, ssgCluster.getAdminPort());
                    } catch (GatewayException e) {
                        throw new GatewayContextCreationException(e);
                    }
                }
            });

            return new GatewayClusterClientImpl(ssgCluster, nodeContexts);
        } catch (GatewayContextCreationException e) {
            throw new GatewayException(e);
        } catch (FindException e) {
            throw new GatewayException(e);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        // TODO invalidate affected caches if ESM user or cluster or cluster node is deleted.
    }
}
