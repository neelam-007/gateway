package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.server.ems.enterprise.SsgNode;

/**
 * Retrieve a gateway context, which can obtain APIs including GatewayApi and NodeManagementApi.
 */
public interface GatewayContextFactory {
    /**
     * Create a GatewayContext that can be used to access Gateway APIs.
     * <p/>
     * A context created via this method will not provide ProcessController APIs.
     * <p/>
     * Callers of this method who never intend to pass null for user and clusterGuid are strongly encouraged
     * to use {@link GatewayClusterClientManager} instead, to access versions of the Gateway APIs that
     * offer local caching of lookup of some entities, and which will do automatic failover when talking
     * to a Gateway cluster, even in the absence of a load balancer front end.
     *
     * @param user User on whose behalf to issue API requests, or null if the access will be on behalf of the ESM itself.
     * @param clusterGuid the cluster GUID for checking that this user has a mapping set up on the target cluster.
     * @param gatewayHostname the SSL hostname of the Gateway cluster to access.  Required.
     * @param gatewayPort the HTTP port to connect to for the Gateway APIs.  Required.
     * @return a GatewayContext that can be used to access Gateway APIs.  Never null.
     * @throws GatewayException if a context cannot be created
     */
    GatewayContext createGatewayContext(User user, String clusterGuid, String gatewayHostname, int gatewayPort ) throws GatewayException;

    /**
     * Create a ProcessControllerContext that can be used to access Process Controller APIs.
     * <p/>
     * A context created via this method will not provide Gateway APIs.
     *
     * @param node the Node to connect to.  Required.
     * @return a GatewayContext that can be used to access Gateway APIs.  Never null.
     * @throws GatewayException if a context cannot be created
     */
    ProcessControllerContext createProcessControllerContext(SsgNode node) throws GatewayException;
}
