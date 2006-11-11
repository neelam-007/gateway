package com.l7tech.server.partition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manage the partition configurations on an SSG.
 * Partitions are physically separate instances of the gateway running from one codebase (the WAR) with different configurations.
 *
 * Partition configurations are located in $SSG_HOME/etc/conf/partitions/partitionName where partitionName is a normal String.
 * All configuration for an SSG is under this directory
 *
 * Date: Nov 10, 2006
 * Time: 10:00:01 AM
 */
public class PartitionManager {
    private static PartitionManager instance;

    private Map<String, PartitionInformation> partitions;

    public static PartitionManager getInstance() {
            if (instance == null)
                instance = new PartitionManager();
            return instance;
        }

    //private constructor - must access this class through getInstance()
    private PartitionManager() {
        partitions = new HashMap<String, PartitionInformation>();
        enumeratePartitions();
    }

    private void enumeratePartitions() {

    }

    public boolean isPartitioned() {
        return !partitions.isEmpty();
    }

    public Set<String> getPartitionNames() {
            return partitions.keySet();
        }

    public PartitionInformation getPartition(String partitionId) {
        return partitions.get(partitionId);
    }

}
