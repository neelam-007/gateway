package com.l7tech.server.ems.gateway;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.api.node.GatewayApi;

import java.util.Collection;

/**
 * Provides high-level access to GatewayApi methods, transparently handling result caching and node request
 * failover.
 * <p/>
 * There is typically one instance of this interface for each combination of (Gateway cluster ID, ESM user ID).
 */
public interface GatewayClusterClient {
    /**
     * Get the SsgCluster instance describing the cluster to which this client is providing access.
     *
     * @return the SsgCluster instance this client is accessing.  Never null.
     */
    SsgCluster getCluster();

    /**
     * Clear all cached data, forcing the next query method to obtain up-to-date information by
     * contacting the Gateway cluster.
     */
    void clearCachedData();

    /**
     * Get information about all entities of the specified type known to this cluster.
     * TODO split this into several finer-grained but more useful query methods, backed by a cache filled
     *      from the coarse-grained remote method
     *
     * @param entityTypes: the types of entities retrieved.  Required.
     * @return one EntityInfo instance for each entity known to this cluster that is of one of the requested types.
     *         May be empty, but never null.
     * @throws GatewayException if the Gateway cluster cannot be accessed.
     */
    Collection<GatewayApi.EntityInfo> getEntityInfo(Collection<EntityType> entityTypes) throws GatewayException;

    /**
     * Get information on the Cluster.
     *
     * @return the Cluster information.
     * @throws GatewayException if the Gateway cluster cannot be accessed.
     */
    GatewayApi.ClusterInfo getClusterInfo() throws GatewayException;

    /**
     * Get information on all Gateways in the cluster.
     *
     * @return The set of gateway information.
     * @throws GatewayException if the Gateway cluster cannot be accessed.
     */
    Collection<GatewayApi.GatewayInfo> getGatewayInfo() throws GatewayException;
}
