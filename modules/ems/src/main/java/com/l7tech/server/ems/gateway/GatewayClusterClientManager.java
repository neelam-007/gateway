package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;

/**
 * Interface to a bean that keeps track of {@link GatewayClusterClient} instances, creating, caching, and
 * discarding them as needed.
 * <p/>
 * ESM component code can use this bean to obtain an instance of GatewayClusterClient for a given
 * (Gateway cluster ID, ESM user ID) combination.
 */
public interface GatewayClusterClientManager {
    /**
     * Get a GatewayClusterClient implementation that will provide access to API services managing
     * the specified Gateway cluster, accessed claiming the access rights of the specified ESM user ID.
     *
     * @param clusterId  the ID of the cluster to access.  Required.
     * @param user   the user on whose behalf the cluster API calls are to be made, or null it
     *               the request is being issued by the ESM itself and not on
     *               behalf of some ESM user
     * @return a GatewayClusterClient instance.  Never null.
     * @throws GatewayException if a GatewayClsuterClient cannot be created.
     */
    GatewayClusterClient getGatewayClusterClient(String clusterId, User user) throws GatewayException;
}
