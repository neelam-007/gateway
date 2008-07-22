package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterProperty;

/**
 * Listener interface for ClusterProperty changes. 
 *
 * @author Steve Jones
 */
public interface ClusterPropertyListener {

    /**
     * Called when a cluster property is added or updated.
     *
     * @param clusterPropertyOld The old value of the cluster property (may be null)
     * @param clusterPropertyNew The new value of the cluster property (may be null)
     */
    void clusterPropertyChanged( ClusterProperty clusterPropertyOld, ClusterProperty clusterPropertyNew);

    /**
     * Called when a cluster property is deleted.
     *
     * @param clusterProperty The deleted cluster property
     */
    void clusterPropertyDeleted(ClusterProperty clusterProperty);
}
