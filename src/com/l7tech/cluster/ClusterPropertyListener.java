package com.l7tech.cluster;

/**
 * Listener interface for ClusterProperty changes. 
 *
 * @author Steve Jones
 */
public interface ClusterPropertyListener {

    /**
     * Called when a cluster property is added or updated.
     *
     * @param clusterProperty The cluster property
     */
    void clusterPropertyChanged(ClusterProperty clusterProperty);

    /**
     * Called when a cluster property is deleted.
     *
     * @param clusterProperty The deleted cluster property
     */
    void clusterPropertyDeleted(ClusterProperty clusterProperty);
}
